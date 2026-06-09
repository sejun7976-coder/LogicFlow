# F-05 — 대화형 추가 피드백

| 항목 | 값 |
|---|---|
| 우선순위 | Medium |
| 예상 변경 파일 수 | 3~4 |
| 신규 의존성 | 없음 (Gson 활용 객체 직렬화) |
| 선행 조건 | F-04 (LLM 기반 평가 완료) |

---

## 1. 목표

사용자가 평가 결과를 단순히 보고 끝내는 것이 아니라, 피드백에서 지적된 부분이나 지문 원문 내용에 대해 AI 튜터와 일대일 실시간 채팅으로 대화하며 의문점을 해소하고 논리력을 추가 개선할 수 있도록 지원한다.

## 2. 인수 기준

- [x] 요약 채점 결과 화면 하단에 "AI 튜터와 일대일 질의응답" 인터페이스가 배치된다.
- [x] 대화 중 사용자가 질문을 전송하면 목록 하단에 질문이 즉시 추가되며, 챗봇이 생각 중임을 알리는 상태 로딩 바가 활성화된다.
- [x] 대화의 맥락이 끊기지 않도록, 사용자의 질문에 답변할 때 현재 독해 지문의 원문 정보, 사용자가 제출한 요약문 정보, 이전에 받은 AI 평가 결과 피드백 전체가 Context 프롬프트로 주입되어 답변을 유도한다.
- [x] 생성된 대화 기록(Chat History)은 직렬화(JSON 형태)되어 해당 채점 결과 데이터베이스 레코드(`chatHistoryJson`)에 누적 저장된다.
- [x] 대화창은 말풍선 형태로 수신/송신 상태를 좌우 분리(유저: 우측 파란색, 모델: 좌측 흰색/회색)하여 대화 흐름이 한눈에 파악되도록 연출한다.

## 3. 변경 범위

### 신규 파일
- 없음 (평가 화면 하단에 챗봇 기능 모듈 결합)

### 수정 파일
- `data/local/Entities.kt` — `AnalysisResultEntity`에 `chatHistoryJson` 컬럼 탑재 확인
- `data/repository/LogicFlowRepository.kt` — 지문 맥락 및 이전 대화 기록을 포함하는 멀티턴(Multi-turn) API 질의 로직 (`chatWithAI`) 구현
- `ui/analysis/AnalysisResultScreen.kt` — 말풍선 스타일 대화 내역 렌더링, 메시지 입력 창 및 전송 버튼 배치
- `ui/analysis/AnalysisResultViewModel.kt` — 입력 상태 버퍼링, 메시지 전송 비동기 이벤트 헨들링 로직 추가

---

## 4. 구현 가이드

### 4.1 대화 컨텍스트 구성 및 API 요청 (`LogicFlowRepository`)
이전 대화 내역(`updatedHistory`)에 지문 및 최초 피드백 정보를 담은 시스템 프롬프트를 접합하여, AI가 전체 맥락 하에서 대화할 수 있도록 컨텍스트를 구성한다.

```kotlin
suspend fun chatWithAI(resultId: String, messageText: String): Result<AnalysisResultEntity> = withContext(Dispatchers.IO) {
    val result = dao.getAnalysisResultById(resultId).first() ?: throw Exception("결과 없음")
    val passage = dao.getPassageById(result.passageId) ?: throw Exception("지문 없음")

    // 대화 내역 역직렬화
    val currentHistory: List<ChatMessage> = gson.fromJson(result.chatHistoryJson, type) ?: emptyList()
    val updatedHistory = currentHistory + ChatMessage("user", messageText)

    // 맥락 주입 시스템 프롬프트 구성
    val systemContext = """
        당신은 사용자의 논리 독해 공부를 돕는 AI 챗봇 튜터입니다.
        
        [독해 지문]
        ${passage.title}
        ${passage.content}
        
        [사용자가 작성한 요약]
        ${result.userSummary}
        
        [AI 평가 피드백]
        ${result.aiFeedback}
        
        위 정보를 바탕으로 사용자의 질문에 친절하고 정확하게 논리적으로 설명해주세요. 지문 내용과 사용자가 작성한 요약의 맥락에서 벗어나지 마세요.
    """.trimIndent()

    val geminiContents = mutableListOf<Content>().apply {
        add(Content(role = "user", parts = listOf(Part(systemContext))))
        add(Content(role = "model", parts = listOf(Part("네, 해당 지문과 피드백 내용을 토대로 도움을 드리겠습니다. 무엇이 궁금하신가요?"))))
        
        // 이전 대화 매핑
        addAll(updatedHistory.map { msg ->
            Content(role = if (msg.role == "user") "user" else "model", parts = listOf(Part(msg.content)))
        })
    }

    val response = apiService.generateContent(model = modelName, apiKey = apiKey, request = GeminiRequest(contents = geminiContents))
    val botResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "응답 실패"

    // 최종 결과 DB 업데이트 및 반환
    val finalHistory = updatedHistory + ChatMessage("model", botResponse)
    val updatedResult = result.copy(chatHistoryJson = gson.toJson(finalHistory))
    dao.insertAnalysisResult(updatedResult)
    Result.success(updatedResult)
}
```

### 4.2 UI 말풍선 구현 (`AnalysisResultScreen`)
메시지 역할(`role`) 분기에 의거하여 색상, 모서리 둥글기 배치 및 폭 제한을 구성한다.

```kotlin
val isUser = msg.role == "user"
Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
) {
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isUser) 12.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 12.dp
                )
            )
            .background(if (isUser) PrimaryBlue else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .widthIn(max = 260.dp)
    ) {
        Text(text = msg.content, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}
```

---

## 5. 검증

1. **지문 컨텍스트 유지**: 챗봇에게 "방금 쓴 요약에서 어떤 부분이 제일 심각한 오류인가요?"라고 대명사를 사용하여 질의했을 때 지문 내용을 기반으로 알맞게 대답하는가?
2. **이전 대화 연계**: "그럼 그 문장을 어떻게 고쳐야 하죠?"라고 연속 질문을 이어 나갔을 때, 이전 턴의 답변 맥락을 올바르게 기억하고 구체적인 교정안을 제시하는가?
3. **대화 내용 저장**: 대화 종료 후 화면을 나갔다가 다시 해당 평가 결과 뷰로 진입했을 때, 이전에 나누었던 챗봇 질의응답 기록이 고스란히 정렬되어 화면에 나타나는가?
4. **로딩 인디케이터**: 전송 버튼을 누른 즉시 튜터가 생각 중이라는 문구와 로딩 스피너가 구동되고, 답변 수신과 동시에 사라지는가?

---

## 6. 비고

- 대화가 끊이지 않고 매끄럽게 지속될 수 있도록 최대 대화 횟수(턴 수)의 제한을 두지 않고 저장하나, 과도한 토큰 사용을 제어하기 위해 뷰모델 또는 프롬프트 차원에서 적절한 가이드를 제공할 수 있다.
- 텍스트 입력 후 전송 버튼 뿐만 아니라 엔터키(또는 키보드 완료 액션)를 통한 입력 전송 또한 기본적으로 허용한다.
