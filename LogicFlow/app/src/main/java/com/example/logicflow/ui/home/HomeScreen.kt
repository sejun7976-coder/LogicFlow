package com.example.logicflow.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logicflow.data.local.AnalysisResultEntity
import com.example.logicflow.ui.components.LogicFlowBottomNavBar
import com.example.logicflow.ui.components.LogicFlowTopAppBar
import com.example.logicflow.ui.components.SquircleCard
import com.example.logicflow.ui.theme.PrimaryBlue
import com.example.logicflow.ui.theme.SuccessEmerald
import com.example.logicflow.ui.theme.TertiaryPurple
import com.example.logicflow.ui.theme.WarningOrange
import com.example.logicflow.ui.theme.BorderLight
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartReading: () -> Unit,
    onMenuClick: () -> Unit,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    val results by viewModel.allResults.collectAsState()
    val streak by viewModel.streakCount.collectAsState()
    val progressList by viewModel.weeklyProgress.collectAsState()

    val currentWeekDaysCompleted = checkCurrentWeekDays(results)

    var showTipSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val infiniteTransition = rememberInfiniteTransition(label = "fire_pulse")
    
    // Scale pulse animation
    val pulseScale by if (streak > 0) {
        val maxPulseExtra = (0.05f + (streak * 0.01f)).coerceAtMost(0.2f)
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.0f + maxPulseExtra,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (1200 - (streak * 100)).coerceAtLeast(600),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }
    
    // Glow alpha animation
    val glowAlpha by if (streak > 0) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = (0.4f + (streak * 0.05f)).coerceAtMost(0.8f),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (1200 - (streak * 100)).coerceAtLeast(600),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        remember { mutableStateOf(0.0f) }
    }

    val fireColor = if (streak > 0) {
        if (streak >= 7) Color(0xFF00E5FF) // Hot Cyan
        else if (streak >= 3) Color(0xFF33B5E5) // Medium Sky Blue
        else PrimaryBlue // Normal Blue
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    }

    val fireContainerBg = if (streak > 0) {
        fireColor.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    if (showTipSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTipSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            TipBottomSheetContent(
                proverb = viewModel.todayProverb,
                summaryReason = viewModel.summaryReason,
                thinkingTip = viewModel.thinkingTip,
                onDismiss = { showTipSheet = false }
            )
        }
    }

    Scaffold(
        topBar = {
            LogicFlowTopAppBar(
                title = "LogicFlow",
                onMenuClick = onMenuClick,
                onNotificationClick = onNotificationClick
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Streak card
            SquircleCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        if (streak > 0) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        alpha = glowAlpha
                                    }
                                    .background(fireColor.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(fireContainerBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "연속 학습",
                                tint = fireColor,
                                modifier = Modifier
                                    .size(
                                        if (streak > 0) {
                                            (28 + streak * 2).coerceAtMost(40).dp
                                        } else {
                                            26.dp
                                        }
                                    )
                                    .graphicsLayer {
                                        if (streak > 0) {
                                            scaleX = pulseScale * 0.95f
                                            scaleY = pulseScale * 0.95f
                                        }
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${streak}일 연속 학습 중",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (streak > 0) "매일 한 편씩 독해 습관을 지키고 있어요!" else "오늘의 지문을 요약하고 스트릭을 채워보세요!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Custom Canvas-drawn stacked bar and line chart card (FR-03)
            SquircleCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "주간 통계",
                        tint = SuccessEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "이번 주 학습 성취도",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Literature Legend (Orange)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(WarningOrange, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "문학",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    // Non-Literature Legend (Blue)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(PrimaryBlue, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "비문학",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    // Cumulative Line Legend (Purple Line with Dot)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(width = 20.dp, height = 12.dp)) {
                            drawLine(
                                color = TertiaryPurple,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawCircle(
                                color = TertiaryPurple,
                                radius = 3.dp.toPx(),
                                center = Offset(size.width / 2, size.height / 2)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "누적 학습량",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Canvas Weekly Stacked Bar & Line Chart
                val emptyColor = MaterialTheme.colorScheme.surfaceVariant
                val textSecondaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                val density = LocalDensity.current

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val barWidth = 16.dp.toPx()
                    val topPadding = 24.dp.toPx()
                    val chartLineY = height - 30.dp.toPx()
                    val maxBarHeight = chartLineY - topPadding

                    val spaceBetween = (width - (barWidth * 7)) / 8

                    val maxCumulative = progressList.maxOf { it.cumulativeCount }
                    val maxCumulativeScale = maxOf(maxCumulative, 5).toFloat()

                    // 1. Draw Day label text below bar and bottom horizontal line
                    drawLine(
                        color = BorderLight.copy(alpha = 0.4f),
                        start = Offset(0f, chartLineY),
                        end = Offset(width, chartLineY),
                        strokeWidth = 1.dp.toPx()
                    )

                    progressList.forEachIndexed { index, dayProgress ->
                        val xOffset = spaceBetween + index * (barWidth + spaceBetween)
                        val dailyTotal = dayProgress.litCount + dayProgress.nonLitCount

                        // Draw background track of the bar (full size of daily scale limit)
                        drawRoundRect(
                            color = emptyColor.copy(alpha = 0.4f),
                            topLeft = Offset(xOffset, topPadding),
                            size = Size(barWidth, maxBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Draw actual progress bar segments (stacked)
                        if (dailyTotal > 0) {
                            val nonLitHeight = maxBarHeight * (dayProgress.nonLitCount.toFloat() / maxCumulativeScale)
                            val litHeight = maxBarHeight * (dayProgress.litCount.toFloat() / maxCumulativeScale)

                            // Non-literature segment (Blue) at the bottom
                            if (dayProgress.nonLitCount > 0) {
                                drawRoundRect(
                                    color = PrimaryBlue,
                                    topLeft = Offset(xOffset, chartLineY - nonLitHeight),
                                    size = Size(barWidth, nonLitHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }

                            // Literature segment (Orange) on top
                            if (dayProgress.litCount > 0) {
                                drawRoundRect(
                                    color = WarningOrange,
                                    topLeft = Offset(xOffset, chartLineY - nonLitHeight - litHeight),
                                    size = Size(barWidth, litHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }

                        // Draw day label text below horizontal line
                        val labelPaint = android.graphics.Paint().apply {
                            color = textSecondaryColor.toArgb()
                            textSize = with(density) { 12.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                dayProgress.dayName,
                                xOffset + barWidth / 2,
                                height - 8.dp.toPx(),
                                labelPaint
                            )
                        }
                    }

                    // 2. Draw superimposed cumulative line chart
                    val todayDayOfWeek = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)

                    // Connect cumulative dots with line
                    var prevPoint: Offset? = null
                    for (i in 0..todayDayOfWeek) {
                        val dayProgress = progressList[i]
                        val xOffset = spaceBetween + i * (barWidth + spaceBetween)
                        val xPos = xOffset + barWidth / 2
                        val cumRatio = dayProgress.cumulativeCount.toFloat() / maxCumulativeScale
                        val yPos = chartLineY - maxBarHeight * cumRatio

                        val currentPoint = Offset(xPos, yPos)
                        if (prevPoint != null) {
                            drawLine(
                                color = TertiaryPurple,
                                start = prevPoint,
                                end = currentPoint,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        prevPoint = currentPoint
                    }

                    // Draw dots and cumulative counts
                    for (i in 0..todayDayOfWeek) {
                        val dayProgress = progressList[i]
                        val xOffset = spaceBetween + i * (barWidth + spaceBetween)
                        val xPos = xOffset + barWidth / 2
                        val cumRatio = dayProgress.cumulativeCount.toFloat() / maxCumulativeScale
                        val yPos = chartLineY - maxBarHeight * cumRatio

                        // Draw white outer border circle for premium look
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(xPos, yPos)
                        )
                        // Draw purple circle
                        drawCircle(
                            color = TertiaryPurple,
                            radius = 4.5.dp.toPx(),
                            center = Offset(xPos, yPos)
                        )
                        // Inner white center dot
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(xPos, yPos)
                        )

                        // Draw cumulative count text inside a styled pill badge
                        val countText = "${dayProgress.cumulativeCount}개"
                        val countPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        }
                        
                        val textWidth = countPaint.measureText(countText)
                        val badgeWidth = textWidth + 10.dp.toPx()
                        val badgeHeight = 16.dp.toPx()
                        val badgeX = xPos - badgeWidth / 2
                        val badgeY = yPos - 8.dp.toPx() - badgeHeight
                        
                        // Draw rounded rect badge
                        drawRoundRect(
                            color = TertiaryPurple,
                            topLeft = Offset(badgeX, badgeY),
                            size = Size(badgeWidth, badgeHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        
                        // Draw text centered inside the badge
                        drawIntoCanvas { canvas ->
                            val textY = badgeY + badgeHeight / 2f - (countPaint.descent() + countPaint.ascent()) / 2f
                            canvas.nativeCanvas.drawText(
                                countText,
                                xPos,
                                textY,
                                countPaint
                            )
                        }
                    }
                }
            }

            // 오늘의 팁 카드 (클릭 시 바텀시트)
            SquircleCard(onClick = { showTipSheet = true }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 오늘의 팁",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "자세히 보기",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\"${viewModel.todayProverb.text}\"",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                if (viewModel.todayProverb.author.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "— ${viewModel.todayProverb.author}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Start reading CTA button
            Button(
                onClick = onStartReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "학습 시작",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "요약능력 기르러가기",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "이동"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TipBottomSheetContent(
    proverb: Proverb,
    summaryReason: String,
    thinkingTip: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "오늘의 팁",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. 속담
        TipSection(
            number = "1",
            title = "속담",
            content = proverb.text,
            author = proverb.author,
            isQuote = true,
            accentColor = PrimaryBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 요약을 해야 하는 이유
        TipSection(
            number = "2",
            title = "요약을 해야 하는 이유",
            content = summaryReason,
            author = "",
            isQuote = false,
            accentColor = SuccessEmerald
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 생각을 정리하는 법
        TipSection(
            number = "3",
            title = "생각을 정리하는 법",
            content = thinkingTip,
            author = "",
            isQuote = false,
            accentColor = TertiaryPurple
        )
    }
}

@Composable
private fun TipSection(
    number: String,
    title: String,
    content: String,
    author: String = "",
    isQuote: Boolean,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = if (isQuote) FontStyle.Italic else FontStyle.Normal,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            if (author.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\u2014 $author",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// Check which days of the current week (Sunday to Saturday) have at least one study result
private fun checkCurrentWeekDays(results: List<AnalysisResultEntity>): List<Boolean> {
    val checked = MutableList(7) { false }
    val startOfWeek = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    results.filter { it.timestamp >= startOfWeek.timeInMillis }.forEach { result ->
        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sun) to 6 (Sat)
        if (dayOfWeek in 0..6) {
            checked[dayOfWeek] = true
        }
    }
    return checked
}
