# F-02 — STT 기반 요약문 자동 입력

| 항목 | 값 |
|---|---|
| 우선순위 | High |
| 예상 변경 파일 수 | 2~3 |
| 신규 의존성 | 없음 (Android Speech SDK 내장 기능 사용) |
| 선행 조건 | F-01 (독해 지문 화면 완료) |

---

## 1. 목표

사용자가 키보드로 타이핑하는 번거로움을 덜고 말하듯이 자연스럽게 요약을 작성할 수 있도록, 스마트폰 마이크를 통해 사용자의 음성을 실시간으로 인식하여 요약문 입력창에 자동 텍스트로 변환해 채워준다.

## 2. 인수 기준

- [x] 요약 입력 영역에 "음성으로 요약 입력" 마이크 버튼이 시각적으로 명확히 제공된다.
- [x] 버튼 탭 시 마이크 권한(`RECORD_AUDIO`)이 승인되어 있으면 음성 녹음 및 인식이 시작된다.
- [x] 녹음 중에는 사용자의 목소리 크기(Volume)를 감지하여 동적 사운드웨이브(`SoundWaveVisualizer`) 애니메이션을 실시간으로 표시하여 녹음 상태를 알린다.
- [x] 음성 인식 중에는 실시간 부분 인식 결과(`sttPartialText`)가 텍스트 필드에 임시로 노출되며, 말이 끝나거나 "완료" 버튼을 누르면 최종 텍스트가 텍스트 필드(`userSummaryText`)에 자동 추가된다.
- [x] 녹음 권한 거부 시 권한 필요 알림(`Toast`)을 띄우며 정상적으로 녹음이 종료된다.

## 3. 변경 범위

### 신규 파일
- 없음 (기존 리더 화면 기능 확장)

### 수정 파일
- `ui/reading/ReadingScreen.kt` — Android `SpeechRecognizer` 연동, 마이크 권한 요청 런처 및 실시간 볼륨 시각화 컴포넌트 추가
- `ui/reading/ReadingViewModel.kt` — 녹음 상태(`isRecording`), 볼륨(`sttVolume`), 부분 텍스트(`sttPartialText`) 관리 상태 흐름 추가

---

## 4. 구현 가이드

### 4.1 음성 인식기 생명주기 관리 (`ReadingScreen`)
컴포즈 생명주기에 맞추어 `SpeechRecognizer`를 할당하고 해제한다.

```kotlin
var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
val recognitionListener = remember {
    object : RecognitionListener {
        override fun onRmsChanged(rmsdB: Float) {
            viewModel.updateSttVolume(rmsdB) // 볼륨 업데이트
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                viewModel.commitPartialText(matches[0]) // 최종 텍스트 적용
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                viewModel.updatePartialText(matches[0]) // 실시간 텍스트 노출
            }
        }
        override fun onError(error: Int) {
            viewModel.setRecordingState(false)
            viewModel.clearPartialText()
            // 에러 코드별 알림 처리
        }
        // 기타 필수 메소드 오버라이드...
    }
}

DisposableEffect(context) {
    val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
    recognizer.setRecognitionListener(recognitionListener)
    speechRecognizer = recognizer
    onDispose {
        recognizer.destroy()
    }
}
```

### 4.2 권한 처리 런처 (`ReadingScreen`)
```kotlin
val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        speechRecognizer?.let { startSpeechRecognition(context, it, viewModel) }
    } else {
        Toast.makeText(context, "마이크 녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }
}
```

### 4.3 볼륨 기반 사운드웨이브 (`SoundWaveVisualizer`)
`onRmsChanged`로 인입된 데시벨 값(약 -2 ~ 12)을 정규화하여 5개 바의 높이로 매핑해 동적 시각화를 지원한다.

```kotlin
@Composable
private fun SoundWaveVisualizer(volume: Float, modifier: Modifier = Modifier) {
    val normalizedVolume = ((volume + 2f) / 14f).coerceIn(0.1f, 1.0f)
    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val factors = listOf(0.4f, 0.8f, 1.0f, 0.7f, 0.3f)
        factors.forEach { factor ->
            val barHeight = 18.dp * (normalizedVolume * factor).coerceAtLeast(0.2f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(PrimaryBlue)
            )
        }
    }
}
```

---

## 5. 검증

1. **권한 확인**: 마이크 권한이 없는 최초 진입 시, 음성 버튼 클릭 시 안드로이드 권한 팝업이 발생하는가?
2. **실시간 텍스트 변환**: 말하는 도중 단어 단위로 텍스트 필드에 실시간(임시 텍스트)으로 보이며, 종료 시 최종 결합되는가?
3. **볼륨 시각화**: 마이크 소리가 커지면 사운드웨이브 바가 커지고, 조용해지면 줄어드는 동적인 그래픽이 작동하는가?
4. **에러 핸들링**: 말하지 않고 일정 시간 방치했을 때 `SpeechRecognizer.ERROR_SPEECH_TIMEOUT` 등이 발생하고 정상적인 녹음 대기 상태로 복귀하는가?

---

## 6. 비고

- 안드로이드 에뮬레이터의 경우 마이크 입력이 연동되도록 에뮬레이터 설정(Microphone 활성화)을 마쳐야 테스트가 가능하다.
- Google STT 모듈 미비 기기를 대비하여 `SpeechRecognizer.isRecognitionAvailable(context)` 분기 처리가 들어가야 한다.
