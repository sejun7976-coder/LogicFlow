package com.example.logicflow.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logicflow.ui.components.LogicFlowBottomNavBar
import com.example.logicflow.ui.components.LogicFlowTopAppBar
import com.example.logicflow.ui.components.SquircleCard
import com.example.logicflow.ui.history.HistoryViewModel
import com.example.logicflow.ui.theme.BorderLight
import com.example.logicflow.ui.theme.PrimaryBlue
import com.example.logicflow.ui.theme.SuccessEmerald
import com.example.logicflow.ui.theme.TertiaryPurple
import com.example.logicflow.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontStyle

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    historyViewModel: HistoryViewModel,
    onResultSelected: (String) -> Unit,
    onMenuClick: () -> Unit,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    val stats by viewModel.statsData.collectAsState()
    val savedNotes by historyViewModel.filteredResults.collectAsState()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("logicflow_prefs", Context.MODE_PRIVATE) }
    var showTutorial by remember { mutableStateOf(!sharedPrefs.getBoolean("tutorial_stats_completed", false)) }
    var currentTutorialStep by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()
    var columnBounds by remember { mutableStateOf<Rect?>(null) }

    var parentHeight by remember { mutableStateOf(0) }
    var tutorialParentHeight by remember { mutableStateOf(0) }
    var circularGaugeBounds by remember { mutableStateOf<Rect?>(null) }
    var summaryRowBounds by remember { mutableStateOf<Rect?>(null) }
    var chartBounds by remember { mutableStateOf<Rect?>(null) }
    var notesBounds by remember { mutableStateOf<Rect?>(null) }

    // Scroll automatically to bring the highlighted element into view
    LaunchedEffect(currentTutorialStep, showTutorial) {
        if (showTutorial) {
            kotlinx.coroutines.delay(150)
            val targetBounds = when (currentTutorialStep) {
                1 -> circularGaugeBounds
                2 -> summaryRowBounds
                3 -> chartBounds
                4 -> notesBounds
                else -> null
            }
            val colBounds = columnBounds
            if (targetBounds != null && colBounds != null) {
                val relativeTop = targetBounds.top - colBounds.top
                val elementHeight = targetBounds.height
                val viewportHeight = colBounds.height
                val absoluteTop = scrollState.value + relativeTop
                val targetScroll = (absoluteTop - (viewportHeight - elementHeight) / 2f).toInt()
                    .coerceIn(0, scrollState.maxValue)
                scrollState.animateScrollTo(targetScroll)
            } else if (currentTutorialStep == 0) {
                scrollState.animateScrollTo(0)
            }
        }
    }

    var showLevelUpEffect by remember { mutableStateOf(false) }
    var levelUpTierName by remember { mutableStateOf("") }
    var levelUpTierNumber by remember { mutableStateOf(1) }

    var showGradeDialog by remember { mutableStateOf(false) }

    val userCount = stats.totalCount
    val userAvg = stats.averageScore

    val currentTier = when {
        userCount >= 20 && userAvg >= 90 -> 7
        userCount >= 16 && userAvg >= 85 -> 6
        userCount >= 12 && userAvg >= 80 -> 5
        userCount >= 8 && userAvg >= 75 -> 4
        userCount >= 5 && userAvg >= 70 -> 3
        userCount >= 3 && userAvg >= 65 -> 2
        userCount >= 1 -> 1
        else -> 0
    }

    val currentGradeName = when (currentTier) {
        7 -> "논리 아키텍트"
        6 -> "논리 마스터"
        5 -> "논리 전문가"
        4 -> "논리 분석가"
        3 -> "논리 탐색가"
        2 -> "논리 수습생"
        1 -> "논리 입문자"
        else -> "등급 없음"
    }

    LaunchedEffect(currentTier) {
        if (currentTier > 0) {
            val hasKey = sharedPrefs.contains("last_saved_tier")
            val lastSavedTier = sharedPrefs.getInt("last_saved_tier", 0)
            if (hasKey && currentTier > lastSavedTier) {
                levelUpTierNumber = currentTier
                levelUpTierName = currentGradeName
                showLevelUpEffect = true
                sharedPrefs.edit().putInt("last_saved_tier", currentTier).apply()
            } else if (!hasKey) {
                sharedPrefs.edit().putInt("last_saved_tier", currentTier).apply()
            } else if (currentTier < lastSavedTier) {
                sharedPrefs.edit().putInt("last_saved_tier", currentTier).apply()
            }
        }
    }

    val gradeTiers = listOf(
        GradeTier(1, "논리 입문자", 1, 0, "1편 이상"),
        GradeTier(2, "논리 수습생", 3, 65, "3편 이상 & 평균 65점 이상"),
        GradeTier(3, "논리 탐색가", 5, 70, "5편 이상 & 평균 70점 이상"),
        GradeTier(4, "논리 분석가", 8, 75, "8편 이상 & 평균 75점 이상"),
        GradeTier(5, "논리 전문가", 12, 80, "12편 이상 & 평균 80점 이상"),
        GradeTier(6, "논리 마스터", 16, 85, "16편 이상 & 평균 85점 이상"),
        GradeTier(7, "논리 아키텍트", 20, 90, "20편 이상 & 평균 90점 이상")
    )

    Scaffold(
        topBar = {
            LogicFlowTopAppBar(
                title = "학습 통계",
                onMenuClick = onMenuClick,
                onNotificationClick = onNotificationClick,
                onHelpClick = {
                    sharedPrefs.edit().putBoolean("tutorial_stats_completed", false).apply()
                    currentTutorialStep = 0
                    showTutorial = true
                }
            )
        },
        bottomBar = {
            LogicFlowBottomNavBar(
                currentRoute = currentRoute,
                onTabSelected = onTabSelected
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .onGloballyPositioned { coords ->
                    parentHeight = coords.size.height
                    columnBounds = coords.boundsInRoot()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Average score circular gauge card
            SquircleCard(
                modifier = Modifier.onGloballyPositioned { coords ->
                    circularGaugeBounds = coords.boundsInRoot()
                }
            ) {
                Text(
                    text = "평균 독해 성취도",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val averageScore = stats.averageScore
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    val progressColor = PrimaryBlue

                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Track Circle
                            drawArc(
                                color = trackColor,
                                startAngle = -225f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Progress Circle
                            val sweepAngle = 270f * (averageScore / 100f)
                            drawArc(
                                color = progressColor,
                                startAngle = -225f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${averageScore}점",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    averageScore >= 85 -> "우수 (Excellent)"
                                    averageScore >= 70 -> "보통 (Average)"
                                    averageScore > 0 -> "노력 필요 (Needs Work)"
                                    else -> "기록 없음"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (averageScore >= 85) SuccessEmerald else if (averageScore >= 70) WarningOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Summary stats cards (Total passages completed)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        summaryRowBounds = coords.boundsInRoot()
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SquircleCard(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = "완료 지문", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("총 요약 횟수", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stats.totalCount}편 완료",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                SquircleCard(
                    modifier = Modifier.weight(1f),
                    onClick = { showGradeDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = "달성 칭호", tint = TertiaryPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("현재 등급", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentGradeName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }



            // Literature vs Non-Literature breakdown card
            SquircleCard(
                modifier = Modifier.onGloballyPositioned { coords ->
                    chartBounds = coords.boundsInRoot()
                }
            ) {
                Text(
                    text = "문학 vs 비문학 학습 현황",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                val totalLitNonLit = stats.litCount + stats.nonLitCount
                if (totalLitNonLit == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 완료된 학습 기록이 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    val litPercentage = (stats.litCount.toFloat() / totalLitNonLit * 100).toInt()
                    val nonLitPercentage = 100 - litPercentage

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "문학 (주황색)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = WarningOrange
                            )
                            Text(
                                text = "${stats.litCount}회 (${litPercentage}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "비문학 (파란색)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryBlue
                            )
                            Text(
                                text = "${stats.nonLitCount}회 (${nonLitPercentage}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal segmented bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (stats.litCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(stats.litCount.toFloat())
                                    .background(WarningOrange)
                            )
                        }
                        if (stats.nonLitCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(stats.nonLitCount.toFloat())
                                    .background(PrimaryBlue)
                            )
                        }
                    }
                }
            }

            // ── 저장된 노트 섹션 ──────────────────────────────────
            HorizontalDivider(
                color = BorderLight.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        notesBounds = coords.boundsInRoot()
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "저장된 노트",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "저장된 노트",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${savedNotes.size}개",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
            }

            if (savedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 작성된 학습 노트가 없습니다.\n읽기 탭에서 요약을 시작해보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    savedNotes.forEach { result ->
                        key(result.id) {
                            val currentResult = rememberUpdatedState(result)
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        historyViewModel.deleteResult(currentResult.value.id)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "삭제",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                            ) {
                                SquircleCard(
                                    onClick = { onResultSelected(result.id) }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = result.passageTitle,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = result.userSummary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            when {
                                                                result.score >= 85 -> SuccessEmerald.copy(alpha = 0.1f)
                                                                result.score >= 70 -> WarningOrange.copy(alpha = 0.1f)
                                                                else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                                            }
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "${result.score}점",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = when {
                                                            result.score >= 85 -> SuccessEmerald
                                                            result.score >= 70 -> WarningOrange
                                                            else -> MaterialTheme.colorScheme.error
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = result.grade,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            Text(
                                                text = dateFormat.format(Date(result.timestamp)),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showTutorial) {
                Spacer(modifier = Modifier.height(400.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }



    if (showLevelUpEffect) {
        val infiniteTransition = rememberInfiniteTransition(label = "levelup_anim")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(enabled = false) {}, // block touches
            contentAlignment = Alignment.Center
        ) {
            // Rotating sunburst background behind card
            Canvas(
                modifier = Modifier
                    .size(360.dp)
                    .graphicsLayer { rotationZ = rotationAngle }
            ) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val rayCount = 12
                val angleDelta = 360f / rayCount
                val rayLength = size.width * 0.5f
                for (i in 0 until rayCount) {
                    val angle = Math.toRadians((i * angleDelta).toDouble())
                    val endX = centerOffset.x + Math.cos(angle).toFloat() * rayLength
                    val endY = centerOffset.y + Math.sin(angle).toFloat() * rayLength
                    val path = Path().apply {
                        moveTo(centerOffset.x, centerOffset.y)
                        val sideAngle1 = Math.toRadians((i * angleDelta - 10f).toDouble())
                        val sideAngle2 = Math.toRadians((i * angleDelta + 10f).toDouble())
                        lineTo(
                            centerOffset.x + Math.cos(sideAngle1).toFloat() * rayLength,
                            centerOffset.y + Math.sin(sideAngle1).toFloat() * rayLength
                        )
                        lineTo(
                            centerOffset.x + Math.cos(sideAngle2).toFloat() * rayLength,
                            centerOffset.y + Math.sin(sideAngle2).toFloat() * rayLength
                        )
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFFD700).copy(alpha = 0.15f)
                    )
                }
            }

            // Congratulations card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Trophy icon pulsing
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .background(WarningOrange.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Text(
                        text = "등급 승급 완료! 🎉",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = WarningOrange,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "사용자님의 독해 수준이 상승했습니다.\n새로운 경지에 오르신 것을 축하합니다!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    // Tier upgrade visualizer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        val prevTierName = when (levelUpTierNumber - 1) {
                            6 -> "논리 마스터"
                            5 -> "논리 전문가"
                            4 -> "논리 분석가"
                            3 -> "논리 탐색가"
                            2 -> "논리 수습생"
                            1 -> "논리 입문자"
                            else -> "등급 없음"
                        }
                        Text(
                            text = prevTierName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = levelUpTierName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showLevelUpEffect = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(
                            text = "멋져요!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showTutorial) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
                .onGloballyPositioned { coords ->
                    tutorialParentHeight = coords.size.height
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                if (currentTutorialStep == 0) {
                    drawRect(color = Color.Black.copy(alpha = 0.8f))
                } else {
                    val path = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, canvasWidth, canvasHeight))

                        when (currentTutorialStep) {
                            1 -> {
                                circularGaugeBounds?.let { bounds ->
                                    addRoundRect(
                                        RoundRect(
                                            rect = Rect(
                                                left = bounds.left - 4.dp.toPx(),
                                                top = bounds.top - 4.dp.toPx(),
                                                right = bounds.right + 4.dp.toPx(),
                                                bottom = bounds.bottom + 4.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                                        )
                                    )
                                }
                            }
                            2 -> {
                                summaryRowBounds?.let { bounds ->
                                    addRoundRect(
                                        RoundRect(
                                            rect = Rect(
                                                left = bounds.left - 4.dp.toPx(),
                                                top = bounds.top - 4.dp.toPx(),
                                                right = bounds.right + 4.dp.toPx(),
                                                bottom = bounds.bottom + 4.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                                        )
                                    )
                                }
                            }
                            3 -> {
                                chartBounds?.let { bounds ->
                                    addRoundRect(
                                        RoundRect(
                                            rect = Rect(
                                                left = bounds.left - 4.dp.toPx(),
                                                top = bounds.top - 4.dp.toPx(),
                                                right = bounds.right + 4.dp.toPx(),
                                                bottom = bounds.bottom + 4.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                                        )
                                    )
                                }
                            }
                            4 -> {
                                notesBounds?.let { bounds ->
                                    addRoundRect(
                                        RoundRect(
                                            rect = Rect(
                                                left = bounds.left - 4.dp.toPx(),
                                                top = bounds.top - 4.dp.toPx(),
                                                right = bounds.right + 4.dp.toPx(),
                                                bottom = bounds.bottom + 4.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                                        )
                                    )
                                }
                            }
                        }
                    }
                    drawPath(path = path, color = Color.Black.copy(alpha = 0.8f))
                }
            }

            // Skip button
            TextButton(
                onClick = {
                    sharedPrefs.edit().putBoolean("tutorial_stats_completed", true).apply()
                    showTutorial = false
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp)
            ) {
                Text(
                    text = "건너뛰기",
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Step Content
            when (currentTutorialStep) {
                0 -> {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "학습 통계 가이드",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "독해 요약 학습 현황과 지금까지 기록된 분석 결과를 확인하는 공간입니다. 중요한 정보들의 설명과 사용법을 안내해 드릴게요.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { currentTutorialStep = 1 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text(
                                    text = "시작하기",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val density = LocalDensity.current
                    val topOffset = remember(circularGaugeBounds) {
                        if (circularGaugeBounds != null) {
                            with(density) { circularGaugeBounds!!.bottom.toDp() }
                        } else {
                            260.dp
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topOffset + 12.dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "↑ 평균 독해 성취도",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        TutorialGuideCard(
                            title = "성취도 게이지 🎯",
                            content = "지금까지 연습한 결과들의 평균 점수를 보여줍니다. 점수대에 따라 우수, 보통, 노력 필요 등 나의 실력을 한눈에 확인할 수 있습니다.",
                            currentStep = 1,
                            totalSteps = 4,
                            onNext = { currentTutorialStep = 2 },
                            onPrev = { currentTutorialStep = 0 }
                        )
                    }
                }
                2 -> {
                    val density = LocalDensity.current
                    val topOffset = remember(summaryRowBounds) {
                        if (summaryRowBounds != null) {
                            with(density) { summaryRowBounds!!.bottom.toDp() }
                        } else {
                            380.dp
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topOffset + 12.dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "↑ 학습 횟수 & 등급",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        TutorialGuideCard(
                            title = "학습량 & 등급 🏆",
                            content = "총 요약한 지문 개수와 현재 내 등급을 보여줍니다. 오른쪽 등급 카드를 누르면 각 등급이 되는 조건과 다음 등급으로 올라가기 위한 목표를 확인할 수 있습니다.",
                            currentStep = 2,
                            totalSteps = 4,
                            onNext = { currentTutorialStep = 3 },
                            onPrev = { currentTutorialStep = 1 }
                        )
                    }
                }
                3 -> {
                    val density = LocalDensity.current
                    val topOffset = remember(chartBounds) {
                        if (chartBounds != null) {
                            with(density) { chartBounds!!.bottom.toDp() }
                        } else {
                            520.dp
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topOffset + 12.dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "↑ 장르별 비율 그래프",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        TutorialGuideCard(
                            title = "문학 vs 비문학 비율 📊",
                            content = "문학과 비문학 학습 비율을 비율 그래프로 한눈에 볼 수 있습니다. 한쪽 분야에 치우치지 않게 균형 있는 요약 독해 연습을 해보세요.",
                            currentStep = 3,
                            totalSteps = 4,
                            onNext = { currentTutorialStep = 4 },
                            onPrev = { currentTutorialStep = 2 }
                        )
                    }
                }
                4 -> {
                    val density = LocalDensity.current
                    val bottomOffset = remember(tutorialParentHeight, notesBounds) {
                        if (tutorialParentHeight > 0 && notesBounds != null) {
                            with(density) { (tutorialParentHeight - notesBounds!!.top).toDp() }
                        } else {
                            180.dp
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomOffset + 12.dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TutorialGuideCard(
                            title = "저장된 노트 보관소 📓",
                            content = "지금까지 제출해서 AI 첨삭을 받은 학습 노트 목록입니다. 노트를 누르면 AI 분석 결과를 다시 볼 수 있으며, 지우고 싶다면 왼쪽으로 밀어서 삭제할 수 있습니다.",
                            currentStep = 4,
                            totalSteps = 4,
                            onNext = {
                                sharedPrefs.edit().putBoolean("tutorial_stats_completed", true).apply()
                                showTutorial = false
                            },
                            onPrev = { currentTutorialStep = 3 }
                        )

                        Text(
                            text = "↓ 저장된 노트 목록",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showGradeDialog) {
        AlertDialog(
            onDismissRequest = { showGradeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "등급 안내",
                        tint = TertiaryPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "독해 등급 안내",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 현재 현황 카드
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "나의 현재 상태",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "등급: $currentGradeName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "요약 횟수: ${userCount}편",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "평균 점수: ${userAvg}점",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 다음 등급 목표 안내
                    if (currentTier < 7) {
                        val nextTier = gradeTiers[currentTier]
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = SuccessEmerald.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "다음 승급 목표: ${nextTier.name}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessEmerald
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• 요약 횟수: ${nextTier.countReq}편 이상 (현재 ${userCount}편)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (nextTier.scoreReq > 0) {
                                    Text(
                                        text = "• 평균 점수: ${nextTier.scoreReq}점 이상 (현재 ${userAvg}점)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = TertiaryPurple.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎉 최상위 등급 달성!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TertiaryPurple
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "최고의 논리 마스터, 아키텍트 등급입니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // 전체 등급 리스트
                    Text(
                        text = "전체 등급 요건",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        gradeTiers.reversed().forEach { tier ->
                            val isCurrent = tier.tier == currentTier
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) PrimaryBlue.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Lv.${tier.tier} ${tier.name}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isCurrent) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PrimaryBlue)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "현재",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tier.desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGradeDialog = false }) {
                    Text("닫기", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

data class GradeTier(
    val tier: Int,
    val name: String,
    val countReq: Int,
    val scoreReq: Int,
    val desc: String
)

@Composable
private fun TutorialGuideCard(
    title: String,
    content: String,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = PrimaryBlue
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..totalSteps) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (i == currentStep) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onPrev) {
                        Text(
                            text = "이전",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (currentStep == totalSteps) "완료" else "다음 ➔",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
