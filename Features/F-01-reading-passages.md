# F-01 — 다양한 독해 지문 제공 (네트워크 페이징 & 로컬 캐싱)

| 항목 | 값 |
|---|---|
| 우선순위 | High |
| 예상 변경 파일 수 | 4~5 |
| 신규 의존성 | `androidx.room:room-runtime`, `androidx.room:room-compiler`, `androidx.room:room-ktx` |
| 선행 조건 | 없음 |

---

## 1. 목표

시스템은 온디맨드 방식으로 사용자에게 요약 훈련용 지문을 제공한다 (방법 B: 실시간 온디맨드 조회). 최초 앱 실행 시 대량의 데이터를 데이터베이스에 미리 시딩(Seeding)하지 않고, 사용자가 각 난이도별(상, 중, 하) 지문을 요청할 때 실시간으로 모의 API 서버로부터 페이징(Pagination) 형태로 데이터를 가져와 로컬 데이터베이스에 캐싱(Cache) 처리한 후 제공한다.

## 2. 인수 기준

- [x] 사용자는 앱 실행 시 난이도("상", "중", "하")를 선택해 해당 난이도의 지문 목록을 필터링할 수 있다.
- [x] 지문 목록은 성능 최적화 및 네트워크 대역폭 절약을 위해 페이지당 10개 단위의 **네트워크 페이징** 방식으로 점진적으로 로드된다.
- [x] 로컬 Room DB에 해당 페이지의 캐시가 존재하면 즉시 반환(Cache Hit)하고, 존재하지 않으면 **800ms의 네트워크 지연 시간(Latency)을 시뮬레이션**한 후 API 서버로부터 지문을 온디맨드 형태로 다운로드하여 Room DB에 캐시(Cache Miss)한 뒤 제공한다.
- [x] API 서버에 더 이상 지문이 없을 때(빈 리스트 반환 시)에만 목록 하단의 '더보기' 버튼이 사라지고 **"지문 추가 개발중입니다..!"**라는 안내 메시지가 노출된다.
- [x] 지문을 선택하면 정답이나 해설을 제외하고, 읽기 편한 전용 리더 UI(적절한 여백, 폰트 크기, 행간 28sp 제공)를 통해 원문 텍스트를 확인할 수 있다.
- [x] 각 지문에는 난이도 배지와 학습 권장 시간(recommendTimeSec)이 시각적으로 표시되어야 한다.
- [x] 13번째 지문부터 동적으로 자동 생성되는 지문들은 플레이스홀더 형태의 단락 안내문이 아니라, 각 난이도(하, 중, 상) 수준에 맞춤 설계된 고품질의 3단락 분량 국어 독해 줄글 본문을 생성하여 실제 읽기 훈련이 가능하도록 구현한다.

## 3. 변경 범위

### 신규/수정 파일
- `data/local/Entities.kt` — 지문 데이터 모델 (`PassageEntity`) 정의
- `data/local/LogicFlowDao.kt` — 난이도 필터링 및 로컬 페이징 캐시 쿼리 정의
- `data/local/LogicFlowDatabase.kt` — 데이터베이스 생성 및 기존의 startup DB 시딩 로직 제거
- `data/repository/LogicFlowRepository.kt` — **[방법 B]** 온디맨드 API 네트워크 지연 시뮬레이션(800ms) 및 로컬 DB 캐싱 로직 구현
- `ui/reading/ReadingScreen.kt` — 난이도 선택 탭, 무한 스크롤(더보기 버튼), 마지막 페이지 도달 시 끝인사 노출 및 리더 뷰 구성
- `ui/reading/ReadingViewModel.kt` — 난이도 필터 상태 관리, 더보기 페이징 처리 로직 및 기존 checkAndSeedDatabase 호출 제거

---

## 4. 구현 가이드

### 4.1 지문 엔티티 (`PassageEntity`)
데이터베이스에서 호출할 지문 스키마는 다음과 같이 구성한다.
```kotlin
@Entity(tableName = "passages")
data class PassageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val difficulty: String, // "하", "중", "상"
    val category: String,   // "중등 국어", "고등 국어" 등
    val recommendTimeSec: Int,
    val modelSummary: String // 모범 요약문 (정답/해설 제외 지문 정보만 활용)
)
```

### 4.2 Room DAO 페이징 처리 (`LogicFlowDao`)
```kotlin
@Query("SELECT * FROM passages WHERE difficulty = :difficulty ORDER BY LENGTH(id) ASC, id ASC LIMIT :limit OFFSET :offset")
suspend fun getPassagesByDifficultyPaginated(difficulty: String, limit: Int, offset: Int): List<PassageEntity>
```

### 4.3 리포지토리 온디맨드 페이징 및 로컬 캐싱 (`LogicFlowRepository`)
로컬 캐시를 먼저 조회하고, 캐시 미스 발생 시 800ms 지연 후 지문을 동적 생성하여 로컬 DB에 저장한다.
```kotlin
suspend fun getPassagesByDifficultyPaginated(difficulty: String, limit: Int, offset: Int): List<PassageEntity> =
    withContext(Dispatchers.IO) {
        if (offset >= 100) return@withContext emptyList()
        val expectedSize = minOf(limit, 100 - offset)
        if (expectedSize <= 0) return@withContext emptyList()
        
        // 1. 로컬 캐시 조회
        val localCached = dao.getPassagesByDifficultyPaginated(difficulty, limit, offset)
        if (localCached.size == expectedSize) {
            return@withContext localCached
        }
        
        // 2. 캐시 미스: 800ms 네트워크 지연 시뮬레이션
        delay(800)
        
        // 3. API 실시간 온디맨드 조회 시뮬레이션 (동적 생성)
        val generatedPassages = mutableListOf<PassageEntity>()
        for (i in offset until minOf(offset + limit, 100)) {
            generatedPassages.add(generatePassage(difficulty, i))
        }
        
        // 4. 로컬 DB 캐시에 삽입
        dao.insertPassages(generatedPassages)
        
        // 5. 정렬 보증을 위해 캐시로부터 최종 쿼리 반환
        dao.getPassagesByDifficultyPaginated(difficulty, limit, offset)
    }
```

### 4.4 뷰모델 페이징 로직 (`ReadingViewModel`)
시딩 로직이 완전히 빠지고 지문 페이징 로드만 트리거하며, 취소 가능한 잡 관리 및 방어적인 중복 ID 필터링을 동반한다.
```kotlin
fun loadNextPage(reset: Boolean = false) {
    if (!reset && _isLoading.value) return
    if (!reset && !_hasMore.value) return

    if (reset) {
        loadJob?.cancel()
        currentOffset = 0
        _hasMore.value = true
    }

    _isLoading.value = true

    loadJob = viewModelScope.launch {
        try {
            val newItems = repository.getPassagesByDifficultyPaginated(
                difficulty = _difficultyFilter.value,
                limit = PAGE_SIZE,
                offset = currentOffset
            )
            if (reset) {
                _passages.value = newItems
            } else {
                _passages.value = (_passages.value + newItems).distinctBy { it.id }
            }
            _hasMore.value = newItems.size >= PAGE_SIZE
            currentOffset += newItems.size
        } catch (e: Exception) {
            // handle exception quietly
        } finally {
            _isLoading.value = false
        }
    }
}
```

---

## 5. 검증

1. **DB 시딩 제거**: 앱을 최초 실행하거나 데이터베이스를 삭제한 후 진입했을 때, 구동 단계에서 DB에 초기 300개의 데이터가 한 번에 적재되지 않는가?
2. **네트워크 지연 및 로딩 인디케이터**: 지문 목록에서 '더보기'를 클릭하여 캐시 미스가 일어날 때 로딩 인디케이터가 표시되고 약 800ms 후 데이터가 로드되는가?
3. **로컬 캐싱**: 이미 한 번 불러와서 Room DB에 저장된 지문 페이지는 지연(800ms) 없이 즉시 반환(Cache Hit)되는가?
4. **마지막 페이지 도달 처리**: API 서버로부터 더 이상 지문이 반환되지 않을 때(가령, 100번째 지문 로드 후 빈 리스트가 반환될 때) 목록 하단에 '더보기' 버튼이 숨겨지고 **"지문 추가 개발중입니다..!"** 안내 문구가 정확히 노출되는가?
