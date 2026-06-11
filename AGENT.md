# AGENT.md

## 1. 개요 (Overview)

본 프로젝트는 사용자의 독해 및 논리 요약 훈련을 위한 안드로이드 학습 앱 **LogicFlow(로직플로)**입니다.
인공지능 코딩 에이전트(Antigravity)에 의해 설계되고 구현되었으며, MVVM 아키텍처와 Room DB 캐싱, Gemini API 기반 대화형 피드백 및 Android 네이티브 STT 기능을 유기적으로 연합하여 고품질의 학습 환경을 완성하였습니다.

---

## 2. 주요 구현 성과 (Key Accomplishments)

* **FR-01: 실시간 온디맨드 지문 페이징 & 로컬 캐싱 (방법 B)**
  * 초기 구동 단계 시딩(Seeding)을 완전히 제거하여 실행 시간 단축 및 리소스 절약.
  * Room DB 캐시 조회(Cache Hit) 및 800ms의 네트워크 딜레이 지연 시뮬레이션(Cache Miss) 구현.
  * 데이터 종료 지점에서 자동으로 "더보기" 버튼을 숨기고 **"지문 추가 개발중입니다..!"** 피드백 문구를 렌더링.
  * 기존의 플레이스홀더 안내문 대신 난이도(하, 중, 상) 맞춤형 어휘로 구성된 **실감 나는 3단락 분량의 국어 독해 지문**을 동적 컴파일하여 제공.
  * String ID 기준 정렬 문제(`mid_100`이 `mid_10` 뒤에 오는 문제)로 인한 페이지네이션 중복 및 LazyColumn 크래시 방지를 위해 SQL 쿼리를 **`ORDER BY LENGTH(id) ASC, id ASC`**로 재설계 및 `ReadingViewModel` 내 `.distinctBy { it.id }` 중복 키 방어 로직 연계.
* **FR-02: STT 기반 요약문 자동 입력**
  * 안드로이드 `SpeechRecognizer` 및 마이크 권한 연동.
  * 음성 녹음 중 실시간 데시벨 볼륨 크기에 반응하는 `SoundWaveVisualizer` UI 적용.
  * 실시간 임시 부분 텍스트 표시(`sttPartialText`) 및 완료 시 누적 텍스트 결합 로직 구현.
* **FR-03: 요약문 수동 편집**
  * STT 입력과 키보드 타이핑 수동 입력이 유실 없이 실시간으로 조합되어 보존되는 `displaySummaryText` 합성 바인딩 구현.
* **FR-04: LLM 기반 일치도 분석 및 평가 (Bento UI)**
  * Gemini API 연동 및 고정형 JSON 스키마 프롬프팅(`application/json`).
  * 의미론적 일치율, 문맥 보존도 분석 및 전제/추론/모순성에 대한 3차 논리 검증 피드백 출력.
  * API Key 만료 및 미등록 시 로컬 규칙 기반 채점 결과로 유연하게 대응하는 **데모 모드(Fallback)** 탑재.
  * 채점 결과의 Room DB 영구 기록 및 히스토리 내역 재조회 기능 제공.
* **FR-05: 대화형 추가 피드백 (챗봇)**
  * 채점 결과 화면에서 이전 대화 기록을 포함하여 질문하는 **멀티턴 대화형 챗봇(AI 튜터)** 구현.
  * 챗봇 대화 이력(`chatHistoryJson`)을 JSON으로 Room DB에 직렬화 저장하여 세션 복원.
* **FR-06: 학습 권장 알림 및 수신 내역 기록**
  * SharedPreferences를 활용한 알림 설정(시간 및 활성화 여부) 저장.
  * `AlarmManager`를 이용한 매일 반복 알림 등록 및 당일 미학습자 필터링 발송 기능.
  * 푸시 발송 시 DB 로그 적재(`NotificationLogEntity`) 및 내비게이션 다이얼로그 연동.
  * 수신 내역 다이얼로그의 리스트 항목을 왼쪽으로 슬라이드 시 시각 피드백과 함께 제거되는 **Swipe-to-Dismiss 개별 삭제 제스처** 구현.
  * 설정 다이얼로그 내 즉시 알림 발송 및 DB 기록을 한 번에 검증할 수 있는 **"즉시 알림 테스트"** 기능 제공.

---

## 3. 기술 스택 & 구조 (Tech Stack)

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose (Material 3)
* **Database**: Room Database (version 9 마이그레이션 및 캐싱 탑재)
* **Network**: Retrofit2 (HTTP API 호출 및 지수 백오프 기반 Retry 인터셉터)
* **Background Services**: AlarmManager & BroadcastReceiver (학습 권장 알람)
* **Speech SDK**: Android SpeechRecognizer (음성인식 처리)

---
