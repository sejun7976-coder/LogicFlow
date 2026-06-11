package com.example.logicflow.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logicflow.data.local.AnalysisResultEntity
import com.example.logicflow.data.repository.LogicFlowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class WeeklyDayProgress(
    val dayName: String,
    val litCount: Int,
    val nonLitCount: Int,
    val cumulativeCount: Int
)

// 명언과 인물을 분리해서 담는 클래스
data class Proverb(
    val text: String,
    val author: String = ""
)

// 요약을 해야 하는 이유 목록
val summaryReasons = listOf(
    "글을 읽고 나서 아무것도 남지 않는 느낌, 한 번쯤 경험해봤을 겁니다. 요약은 그 문제를 해결하는 가장 직접적인 방법입니다. 핵심만 추리는 과정에서 뇌는 내용을 단순히 '받아들이는' 것을 넘어 적극적으로 처리하게 됩니다. 이 과정이 반복될수록 글을 읽는 속도와 이해의 깊이가 함께 늘어납니다.",
    "요약은 '내가 실제로 이해했는지'를 스스로 점검하는 도구입니다. 읽을 때는 이해한 것 같아도 막상 정리하려 하면 말이 안 나오는 부분이 생깁니다. 그 부분이 바로 아직 완전히 소화되지 않은 부분입니다. 요약을 습관으로 삼으면 그 공백을 스스로 발견하고 채워나갈 수 있습니다.",
    "긴 글을 짧게 만드는 작업은 단순한 축약이 아닙니다. 무엇이 본론이고 무엇이 부연인지, 어떤 문장이 전제이고 어떤 문장이 결론인지를 판단하는 능력을 요구합니다. 이 판단력은 논술, 발표, 보고서 작성 등 어떤 맥락에서도 유용하게 쓰입니다.",
    "자신의 말로 다시 쓰는 과정은 단순 암기와 다릅니다. 외운 문장은 맥락이 바뀌면 쓰기 어렵지만, 이해한 내용을 재구성한 문장은 응용이 됩니다. 요약 훈련을 꾸준히 하면 새로운 글을 읽을 때 구조를 파악하는 속도가 점점 빨라집니다.",
    "짧게 쓰는 것이 길게 쓰는 것보다 어렵습니다. 요약은 불필요한 것을 걷어내는 작업이기 때문에, 남길 것과 버릴 것을 계속 판단해야 합니다. 이 훈련이 반복될수록 글을 읽는 눈이 달라집니다. 처음 읽을 때부터 중요한 문장이 먼저 눈에 들어오게 됩니다."
)

// 생각을 정리하는 법 목록
val thinkingTips = listOf(
    "생각이 정리되지 않는 가장 큰 이유 중 하나는 머릿속에서만 처리하려 하기 때문입니다. 생각은 눈에 보이지 않아서 잡기 어렵습니다. 일단 손으로 적어보세요. 문장이 완성되지 않아도 괜찮습니다. 단어 하나라도 종이 위에 올려놓으면, 뭉쳐 있던 생각들이 분리되기 시작합니다. 적고 나서 보면 '이게 내가 고민하던 것이었구나' 하고 놀라는 경험을 하게 됩니다.",
    "비슷한 내용끼리 묶고 이름을 붙이는 작업은 생각에 구조를 만드는 과정입니다. 생각이 많을수록 항목이 섞이고 우선순위가 흐릿해집니다. 묶음을 만들면 전체 그림이 보이고, 지금 어디서 막혀 있는지도 보입니다. 덩어리 하나하나에 이름을 붙이는 순간, 그 생각은 막연한 고민에서 다룰 수 있는 문제로 바뀝니다.",
    "생각을 시작하기 전에 딱 한 가지만 정하세요. '이 생각의 목적이 무엇인가?'입니다. 결정을 내려야 하는 건지, 아이디어를 모으는 건지, 감정을 정리하는 건지에 따라 방식이 달라집니다. 목적 없이 생각하면 같은 자리를 계속 맴돌게 됩니다. 방향을 먼저 잡으면 생각의 시간이 훨씬 짧아집니다.",
    "한 번에 완벽하게 정리하려는 마음이 오히려 정리를 방해합니다. 생각은 단번에 완성되지 않습니다. 오늘은 60%만 정리하고 내일 다시 보세요. 하룻밤 자고 나면 어제 보이지 않던 것이 보입니다. 뇌는 잠을 자는 동안에도 생각을 처리하고 있기 때문에, 억지로 완성하려 하지 않는 것이 오히려 더 나은 결과로 이어집니다.",
    "생각이 막혔을 때 가장 빠른 출구는 질문입니다. '이걸 왜 생각하고 있지?', '지금 나는 무엇을 원하고 있지?', '이 문제에서 내가 통제할 수 있는 것은 무엇이지?' 이 세 가지 질문 중 하나만 던져도 방향이 잡힙니다. 생각이 복잡할수록 더 단순한 질문이 필요합니다."
)

class HomeViewModel(
    private val repository: LogicFlowRepository,
    private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("logicflow_prefs", Context.MODE_PRIVATE)

    val allResults: StateFlow<List<AnalysisResultEntity>> = repository.getAllAnalysisResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streakCount: StateFlow<Int> = allResults.map { results ->
        calculateStreak(results)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyProgress: StateFlow<List<WeeklyDayProgress>> = allResults.map { results ->
        calculateWeeklyProgress(results)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultWeeklyProgress())

    val hasStudiedToday: StateFlow<Boolean> = allResults.map { results ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis
        results.any { it.timestamp >= startOfToday }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial

    private val _currentTutorialStep = MutableStateFlow(0)
    val currentTutorialStep: StateFlow<Int> = _currentTutorialStep

    init {
        val completed = sharedPrefs.getBoolean("tutorial_completed", false)
        _showTutorial.value = !completed
    }

    fun completeTutorial() {
        sharedPrefs.edit().putBoolean("tutorial_completed", true).apply()
        _showTutorial.value = false
    }

    fun resetTutorial() {
        sharedPrefs.edit().putBoolean("tutorial_completed", false).apply()
        _currentTutorialStep.value = 0
        _showTutorial.value = true
    }

    fun setTutorialStep(step: Int) {
        _currentTutorialStep.value = step
    }

    // 앱 실행마다 랜덤으로 명언 하나 뽑기
    val todayProverb: Proverb = loadRandomProverb()

    // 요약을 해야 하는 이유 (날짜 기반으로 하나 고정)
    val summaryReason: String = summaryReasons[
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % summaryReasons.size
    ]

    // 생각을 정리하는 법 (날짜 기반으로 하나 고정)
    val thinkingTip: String = thinkingTips[
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % thinkingTips.size
    ]

    private fun loadRandomProverb(): Proverb {
        return try {
            val lines = context.assets.open("sokdam.txt")
                .bufferedReader(Charsets.UTF_8)
                .readLines()
                .filter { it.isNotBlank() }
            if (lines.isEmpty()) return Proverb("시작이 반이다")
            val raw = lines.random().trim()
            parseProverb(raw)
        } catch (e: Exception) {
            Proverb("시작이 반이다")
        }
    }

    // "번호. 명언 - 인물" 형식 파싱
    private fun parseProverb(raw: String): Proverb {
        // 앞의 번호 제거: "12. " 형태
        val withoutNumber = raw.replace(Regex("^\\d+\\.\\s*"), "").trim()
        // " - " 기준으로 마지막 분리 (명언 안에 "-"가 있을 수 있어 lastIndexOf 사용)
        val dashIndex = withoutNumber.lastIndexOf(" - ")
        return if (dashIndex >= 0) {
            val text = withoutNumber.substring(0, dashIndex).trim()
            val author = withoutNumber.substring(dashIndex + 3).trim()
            Proverb(text, author)
        } else {
            Proverb(withoutNumber)
        }
    }

    private fun calculateStreak(results: List<AnalysisResultEntity>): Int {
        if (results.isEmpty()) return 0
        val dates = results.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }.toSet()

        var streak = 0
        val today = Calendar.getInstance()

        while (true) {
            val dateStr = "${today.get(Calendar.YEAR)}-${today.get(Calendar.MONTH)}-${today.get(Calendar.DAY_OF_MONTH)}"
            if (dates.contains(dateStr)) {
                streak++
                today.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                if (streak == 0) {
                    today.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = "${today.get(Calendar.YEAR)}-${today.get(Calendar.MONTH)}-${today.get(Calendar.DAY_OF_MONTH)}"
                    if (dates.contains(yesterdayStr)) {
                        streak++
                        today.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                }
                break
            }
        }
        return streak
    }

    private fun calculateWeeklyProgress(results: List<AnalysisResultEntity>): List<WeeklyDayProgress> {
        val days = listOf("일", "월", "화", "수", "목", "금", "토")
        val dailyLit = IntArray(7) { 0 }
        val dailyNonLit = IntArray(7) { 0 }

        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val weekResults = results.filter { it.timestamp >= startOfWeek.timeInMillis }

        weekResults.forEach { result ->
            val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            if (dayOfWeek in 0..6) {
                if (result.passageType == "문학") {
                    dailyLit[dayOfWeek]++
                } else {
                    dailyNonLit[dayOfWeek]++
                }
            }
        }

        var runningSum = 0
        val progress = ArrayList<WeeklyDayProgress>()
        for (i in 0..6) {
            val dailyTotal = dailyLit[i] + dailyNonLit[i]
            runningSum += dailyTotal
            progress.add(
                WeeklyDayProgress(
                    dayName = days[i],
                    litCount = dailyLit[i],
                    nonLitCount = dailyNonLit[i],
                    cumulativeCount = runningSum
                )
            )
        }
        return progress
    }

    private fun defaultWeeklyProgress(): List<WeeklyDayProgress> {
        val days = listOf("일", "월", "화", "수", "목", "금", "토")
        return days.map { WeeklyDayProgress(it, 0, 0, 0) }
    }
}
