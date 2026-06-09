# F-03 — 요약문 수동 편집

| 항목 | 값 |
|---|---|
| 우선순위 | High |
| 예상 변경 파일 수 | 2 |
| 신규 의존성 | 없음 |
| 선행 조건 | F-02 (STT 입력 기능 연동) |

---

## 1. 목표

사용자는 STT(음성 인식) 도중 발생한 단어 오인식이나 오타를 수정하거나, 마이크를 사용하지 않고 키보드로 직접 요약문을 작성 및 수정할 수 있다.

## 2. 인수 기준

- [x] 음성으로 변환되어 채워진 텍스트 필드는 포커스를 얻었을 때 키보드가 올라오고 자유롭게 수정할 수 있어야 한다.
- [x] 키보드로 타이핑을 하거나 텍스트를 삭제/수정할 때, 화면상의 요약문 상태가 실시간으로 동기화되어 유실되지 않아야 한다.
- [x] STT 임시 결과가 출력 중일 때는 수동 수정창이 해당 텍스트를 포함하여 매끄럽게 표시하며, 최종 적용된 이후에도 추가 수동 편집이 완벽하게 지원되어야 한다.

## 3. 변경 범위

### 신규 파일
- 없음 (텍스트 필드 인터랙션 고도화)

### 수정 파일
- `ui/reading/ReadingScreen.kt` — `OutlinedTextField`에 상태 변수 바인딩 및 활성화
- `ui/reading/ReadingViewModel.kt` — 수동 편집 시의 텍스트 업데이트 메소드 (`updateSummaryText`)

---

## 4. 구현 가이드

### 4.1 수동 및 자동 결합 텍스트 출력 (`ReadingScreen`)
사용자가 직접 타이핑하는 텍스트(`summaryText`)와 실시간 인식되고 있는 음성 부분 텍스트(`sttPartialText`)가 화면에서 겹치지 않고 자연스럽게 합성되어 보이도록 구현한다.

```kotlin
val displaySummaryText = if (sttPartialText.isNotEmpty()) {
    if (summaryText.isEmpty()) sttPartialText else "$summaryText $sttPartialText"
} else {
    summaryText
}

OutlinedTextField(
    value = displaySummaryText,
    onValueChange = { viewModel.updateSummaryText(it) }, // 사용자가 직접 타이핑할 때 호출
    placeholder = { Text("여기에 지문의 요약문을 직접 타이핑하거나 아래 마이크를 눌러 음성으로 입력해보세요.") },
    modifier = Modifier
        .fillMaxWidth()
        .height(150.dp),
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
    )
)
```

### 4.2 뷰모델 텍스트 가공 로직 (`ReadingViewModel`)
사용자가 텍스트를 편집할 때 상태를 실시간 업데이트하고, 음성 입력 완료 시 기존 텍스트 뒤에 덧붙여주는 공백 처리(`appendSummaryText`)를 포함한다.

```kotlin
private val _userSummaryText = MutableStateFlow("")
val userSummaryText: StateFlow<String> = _userSummaryText

fun updateSummaryText(text: String) {
    _userSummaryText.value = text
}

fun appendSummaryText(text: String) {
    val current = _userSummaryText.value
    _userSummaryText.value = if (current.isEmpty()) text else "$current $text"
}
```

---

## 5. 검증

1. **텍스트 필드 터치**: 텍스트 필드 영역을 탭했을 때 가상 키보드가 올라오고 포커스가 잡히는가?
2. **수동 타이핑 및 지우기**: 키보드로 텍스트를 자유롭게 입력하거나 백스페이스(Backspace)로 지울 수 있으며, 수정한 부분이 그대로 저장되는가?
3. **STT와 혼용 테스트**: 
   - 음성으로 "한국어 요약"을 말한 뒤 -> 수동으로 "공부"라는 단어를 뒤에 타이핑했을 때 -> 다시 음성으로 입력하면 마지막 단어 뒤에 한 칸 띄우고 음성이 정상 추가되는가?

---

## 6. 비고

- `displaySummaryText`는 음성이 입력되는 중에 수동 타이핑 내용이 가려지거나 왜곡되는 것을 방지하기 위해 분기 설계되어 있다.
- 텍스트 입력창은 최대 150dp 높이로, 긴 요약문 작성 시 내부 스크롤이 가능하도록 기본 지원된다.
