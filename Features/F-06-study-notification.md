# F-06 — 학습 알림 설정 및 알림 수신 내역 (스와이프 삭제)

| 항목 | 값 |
|---|---|
| 우선순위 | Medium |
| 예상 변경 파일 수 | 5~6 |
| 신규 의존성 | 없음 (기존 Room 및 AlarmManager 사용) |
| 선행 조건 | F-01 |

---

## 1. 목표

사용자가 매일 규칙적으로 독해 연습을 할 수 있도록 학습 권장 알림 기능을 제공한다. 사용자는 설정 화면에서 매일 정해진 시간에 알림을 수신하도록 지정할 수 있으며, 실제 수신된 알림 이력은 로컬 DB에 영구 기록된다. 사용자는 알림 수신 이력 창을 띄워 내역을 조회하고, 개별 내역을 드래그(Swipe)하여 간편하게 삭제할 수 있다.

## 2. 인수 기준

- [x] 사용자는 설정 화면에서 매일 학습 권장 알림 수신 여부(Switch)를 활성화/비활성화할 수 있다.
- [x] 사용자는 알림이 울릴 시간(Hour, 0~23)을 설정할 수 있으며, 해당 설정은 SharedPreferences 등을 통해 로컬에 저장되고 `AlarmManager`를 통해 매일 반복 예약된다.
- [x] 알림 작동 조건: 설정된 시간에 도달했을 때, 당일 학습 이력(분석 결과 테이블)을 조회하여 **아직 오늘 공부하지 않은 경우에만** 학습 권장 푸시 알림을 발송한다.
- [x] 알림이 발송될 때(백그라운드 예약 알림 또는 즉시 테스트 알림 모두 포함)마다 `notification_logs` 테이블에 수신 로그(`NotificationLogEntity`)를 삽입하여 영구 기록한다.
- [x] 내비게이션 바/헤더 우측 상단의 알림 종 아이콘을 누르면 **알림 수신 내역 다이얼로그**가 열린다.
- [x] 수신 이력 목록은 최신순 정렬되며, 각 로그의 수신 시각은 `"MM월 dd일 HH:mm"` (예: `06월 09일 22:30`) 형태로 가독성 있게 노출된다.
- [x] 사용자는 목록의 각 항목을 **오른쪽에서 왼쪽으로 밀어서(Swipe to Dismiss)** 개별 항목을 시각적 피드백과 함께 간편하게 삭제할 수 있으며, 이 동작은 데이터베이스에도 실시간으로 반영되어 삭제 처리된다.
- [x] 설정 및 다이얼로그 내에 **"즉시 알림 테스트"** 버튼을 제공하여, 클릭 즉시 실제 알림 수신 및 수신 로그 저장이 작동해 다이얼로그 목록에 실시간 반영되는지 직접 테스트해 볼 수 있다.

## 3. 변경 범위

### 신규/수정 파일
- `data/local/Entities.kt` — 알림 로그 엔티티 (`NotificationLogEntity`) 추가
- `data/local/LogicFlowDao.kt` — 알림 로그 삽입, 전체 조회(최신순), 개별 ID 기준 삭제 쿼리 추가
- `data/local/LogicFlowDatabase.kt` — 데이터베이스 버전을 업데이트하고 `NotificationLogEntity` 등록
- `data/repository/LogicFlowRepository.kt` — 알림 로그 관련 삽입, Flow 관찰, 삭제 메소드 노출 및 알림 시간/활성화 여부 SharedPref 래핑 제공
- `notification/AlarmReceiver.kt` — 지정된 시각에 알람을 받아 오늘 학습 여부를 판별하여 백그라운드 푸시 알림을 발송하고 로그를 DB에 적재
- `ui/navigation/NavGraph.kt` — 상단 알림 아이콘 노출, `NotificationStatusDialog` 설계 및 `SwipeToDismissBox` 기반 스와이프 개별 삭제 제스처 연동

---

## 4. 구현 가이드

### 4.1 알림 로그 엔티티 (`NotificationLogEntity`)
```kotlin
@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey val id: String, // UUID 형식 등
    val title: String,
    val content: String,
    val timestamp: Long
)
```

### 4.2 스와이프 삭제 UI 연동 (`SwipeToDismissBox`)
Compose의 `SwipeToDismissBox` 및 `SwipeToDismissBoxValue`를 연동하여 알림 리스트 아이템에 제스처를 부여한다.
```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart) {
            coroutineScope.launch {
                repository.deleteNotificationLogById(log.id)
            }
            true
        } else false
    }
)

SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = false, // 오른쪽에서 왼쪽 스와이프만 허용
    backgroundContent = {
        // 빨간색 배경 및 휴지통 아이콘 등 스와이프 중 배경 연출
    },
    content = {
        // 실제 알림 항목 정보 렌더링 (날짜 형식 MM월 dd일 HH:mm 표시)
    }
)
```

### 4.3 알림 발송 시 로그 자동 적재 (`AlarmReceiver`)
```kotlin
val notificationLog = NotificationLogEntity(
    id = UUID.randomUUID().toString(),
    title = "학습 권장 알림",
    content = "오늘의 논리 독해 훈련이 아직 완료되지 않았습니다. 지금 학습을 시작해보세요!",
    timestamp = System.currentTimeMillis()
)
repository.insertNotificationLog(notificationLog)
```

---

## 5. 검증

1. **알림 설정 저장 및 예약**: 설정 화면에서 알림 시간을 변경하고 토글을 껐다 켤 때 앱 재시작 후에도 설정 정보가 온전히 유지되는가?
2. **오늘 학습 여부 판별 필터링**: 당일 이미 독해 요약 학습(분석 결과 저장)을 완료한 경우, 지정 시각에 도달해도 알람이 발송되지 않는가?
3. **실시간 이력 갱신**: 다이얼로그가 열린 상태에서 "즉시 알림 테스트" 클릭 시 푸시 알림 수신과 동시에 다이얼로그의 수신 이력 목록에 실시간 추가 노출되는가?
4. **스와이프 제스처 및 삭제**: 알림 항목을 왼쪽으로 슬라이드할 때 슬라이드 방향에 따라 삭제 배경이 노출되고 완전히 밀면 진동/애니메이션과 함께 목록 및 DB에서 완전히 삭제되는가?
