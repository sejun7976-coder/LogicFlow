# F-04 — LLM 기반 일치도 분석 및 평가

| 항목 | 값 |
|---|---|
| 우선순위 | High |
| 예상 변경 파일 수 | 5~6 |
| 신규 의존성 | `com.squareup.retrofit2:retrofit:2.9.0`, `com.squareup.retrofit2:converter-gson:2.9.0` |
| 선행 조건 | F-01 (독해 지문 제공), F-03 (요약문 입력 완료) |

---

## 1. 목표

사용자가 제출한 요약문과 원문 지문을 LLM(Gemini API)을 통해 비교 분석한다. 단순 단어 일치가 아닌 의미론적 문맥 일치율과 논리 정합성(전제, 추론, 모순 검증)을 계산하여 시각적이고 구체적인 텍스트 피드백과 점수를 제공한다.

## 2. 인수 기준

- [x] 채점 요청 시 로딩 인디케이터가 활성화되며, Gemini API와의 통신을 통해 요약 평가가 백그라운드에서 진행된다.
- [x] 단순한 형태소 매칭 방식이 아닌, 의미론적 일치율(`semanticMatch`) 및 문맥 보존도(`contextPreservation`)를 LLM으로 판별하여 수치로 표기한다.
- [x] 논리적 구조 분석을 위해 **기본 전제 검증**, **추론 일관성**, **예외 모순 검증** 결과와 각 항목별 상세 텍스트 피드백이 제공된다.
- [x] 맞춤법 오류 및 비문 수정을 포함하는 구체적인 첨삭본(`correctedText`)과 AI가 작성한 모범 요약문(`aiSummary`)이 분리 표기된다.
- [x] API Key가 존재하지 않거나 유효하지 않은 경우, 학습 중단 방지를 위해 로컬 규칙 기반의 데모 모드(Fallback) 결과값 생성을 지원한다.
- [x] 채점 결과는 Room 데이터베이스에 즉시 영구 저장되어 역사(History) 화면에서 언제든지 재참조가 가능하다.

## 3. 변경 범위

### 신규 파일
- `data/remote/GeminiApiService.kt` — Gemini API 통신 인터페이스 및 DTO 정의
- `ui/analysis/AnalysisResultScreen.kt` — 점수, 등급, 세부 검증(전제/추론), 첨삭 피드백을 표현하는 벤토 그리드(Bento-Grid) 형식의 UI 화면
- `ui/analysis/AnalysisResultViewModel.kt` — 특정 채점 결과 조회 및 피드백 상태 파이프라인 구축

### 수정 파일
- `data/local/Entities.kt` — `AnalysisResultEntity` 정의 추가
- `data/repository/LogicFlowRepository.kt` — Gemini JSON 스키마 프롬프트 구성, 호출 실패 시 지수 백오프 기반 재시도(Retry) 알고리즘 및 데모 모드 Fallback 지원
- `ui/navigation/NavGraph.kt` — `analysis/{resultId}` 라우트 추가 및 뷰모델 연결

---

## 4. 구현 가이드

### 4.1 Gemini API 응답 스키마 프롬프트 (`LogicFlowRepository`)
Gemini API 호출 시 예측 가능하고 일관된 파싱을 지원하도록 `responseMimeType = "application/json"`을 활용하고 명확한 JSON 스키마를 프롬프트에 동봉한다.

```kotlin
val prompt = """
    당신은 독해 및 논리적 요약 평가 전문가입니다.
    다음 지문과 사용자가 작성한 요약문을 분석하여 JSON 형식으로 결과를 출력해주세요.

    [지문]
    ${passage.content}

    [사용자 요약문]
    $userSummary

    출력 JSON 스키마:
    {
      "score": 0~100 사이의 점수 (정수),
      "grade": "높은 신뢰도 등급", "보통 신뢰도 등급", "낮은 신뢰도 등급" 중 하나,
      "semanticMatch": 0~100 사이의 의미론적 일치도 점수 (정수),
      "contextPreservation": 0~100 사이의 문맥 보존도 점수 (정수),
      "premiseCheck": 전제 검증 여부 (true/false),
      "premiseDetail": "전제 검증 상세 피드백",
      "inferenceCheck": 추론 과정 검증 여부 (true/false),
      "inferenceDetail": "추론 과정 검증 상세 피드백",
      "exceptionCheck": 예외 사례 검증 여부 (true/false),
      "exceptionDetail": "예외 사례 검증 상세 피드백",
      "aiFeedback": "전반적인 개선점과 논리적 피드백에 대한 종합 의견",
      "correctedText": "사용자 요약문의 맞춤법 및 논리적 흐름을 개선한 교정본 문장",
      "aiSummary": "지문의 핵심을 잘 살린 모범 요약문",
      "passageType": "문학" 또는 "비문학"
    }
""".trimIndent()
```

### 4.2 API 예외 및 재시도 정책 (`LogicFlowRepository`)
네트워크 상태 불안정 또는 API 할당량 초과(HTTP 429, 503)를 대응하기 위해 최대 3회 지수 백오프 재시도 로직을 적용한다.

```kotlin
private suspend fun <T> callWithRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 3000L,
    block: suspend () -> T
): T {
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: HttpException) {
            if (e.code() == 429 || e.code() == 503) {
                delay(initialDelayMs * (1L shl attempt))
            } else { throw e }
        }
    }
    throw Exception("네트워크 오류")
}
```

### 4.3 벤토 그리드 UI 구성 (`AnalysisResultScreen`)
획일적인 목록 나열 대신 중요도에 따라 그리드 형태의 카드 배치(Bento UI)를 구현한다.
- **상단**: 지문 제목 및 문학/비문학 유형 배지
- **중앙 카드**: 원형 그래프 형태의 획득 점수 및 신뢰도 등급 표기
- **좌우 분할 영역**: 기본 전제 분석 및 추론 일관성 체크 결과 나란히 배치
- **하단 카드**: AI 모범 요약문 및 작성된 요약문 대비 첨삭 피드백 비교 화면

---

## 5. 검증

1. **API 호출**: 지문 요약 작성 후 채점 버튼 탭 시 로딩 인디케이터가 정상 회전하며 백그라운드 호출이 수행되는가?
2. **평가 데이터 매핑**: AI 평가 결과 데이터(점수, 신뢰도 등급, 전제/추론 정합 여부, 텍스트 피드백 등)가 화면상의 각 Bento 카드 영역에 빠짐없이 정확히 표시되는가?
3. **데모 모드 동작**: API Key 설정이 비어 있는 상황에서도 정상적으로 점수 연산 및 모의 피드백 결과가 출력되는가?
4. **저장 상태 확인**: 홈 화면에서 히스토리 목록으로 진입하여 해당 평가 아이템 선택 시 이전에 생성된 분석 정보와 피드백이 동일하게 표출되는가?

---

## 6. 비고

- API Key는 사용자 환경 보호를 위해 `SettingsScreen`에서 안전하게 암호화되거나 별도 Preferences 데이터로 저장하여 사용하도록 한다.
- JSON 파싱 오류 시 파손된 데이터가 화면을 중단시키지 않도록 `runCatching` 또는 안전한 Fallback 엔티티 바인딩 처리를 필수로 포함한다.
