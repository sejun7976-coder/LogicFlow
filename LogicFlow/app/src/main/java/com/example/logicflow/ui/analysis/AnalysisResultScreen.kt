package com.example.logicflow.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logicflow.data.local.AnalysisResultEntity
import com.example.logicflow.data.local.PassageEntity
import com.example.logicflow.ui.components.LogicFlowBottomNavBar
import com.example.logicflow.ui.components.SquircleCard
import com.example.logicflow.ui.theme.BorderLight
import com.example.logicflow.ui.theme.ErrorRed
import com.example.logicflow.ui.theme.PrimaryBlue
import com.example.logicflow.ui.theme.SuccessEmerald
import com.example.logicflow.ui.theme.WarningOrange
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    viewModel: AnalysisResultViewModel,
    resultId: String,
    onBackClick: () -> Unit,
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    val resultState by viewModel.currentResult.collectAsState()
    val passageState by viewModel.currentPassage.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var showDetailedReport by remember { mutableStateOf(false) }

    LaunchedEffect(resultId) {
        viewModel.loadResult(resultId)
        showDetailedReport = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "요약 채점 결과",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
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
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val result = resultState
            if (result == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { 3 })
                var expandUserSummary by remember(result.id) { mutableStateOf(false) }
                var hasSummaryOverflow by remember(result.id) { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(4.dp))

                // Title & Passage Metadata Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.passageTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Passage Type Badge
                            val isLit = result.passageType == "문학"
                            val badgeBg = if (isLit) WarningOrange.copy(alpha = 0.1f) else PrimaryBlue.copy(alpha = 0.1f)
                            val badgeColor = if (isLit) WarningOrange else PrimaryBlue
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = result.passageType,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeColor
                                )
                            }

                            val passage = passageState
                            if (passage != null) {
                                // Category Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = passage.category,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                // Difficulty Badge
                                val diffBg = when (passage.difficulty) {
                                    "상" -> ErrorRed.copy(alpha = 0.1f)
                                    "중" -> WarningOrange.copy(alpha = 0.1f)
                                    else -> SuccessEmerald.copy(alpha = 0.1f)
                                }
                                val diffColor = when (passage.difficulty) {
                                    "상" -> ErrorRed
                                    "중" -> WarningOrange
                                    else -> SuccessEmerald
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(diffBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "난이도: ${passage.difficulty}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = diffColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Bento 1: Score & Overview (Duolingo-style Full-width Hero Card)
                val isHigh = result.grade.contains("높은") || result.grade.contains("높음")
                val isLow = result.grade.contains("낮은") || result.grade.contains("낮음")
                
                val gradientBrush = when {
                    isHigh -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
                    isLow -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF87171)))
                    else -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
                }
                
                val gradeDescription = when {
                    isHigh -> "우수한 논리 구조 파악! 지문의 전제와 추론을 완벽하게 이해하고 요약했습니다."
                    isLow -> "기초를 다질 기회! 핵심 논리 구조와 기본 전제 분석을 다시 점검해 봅시다."
                    else -> "대체로 양호한 분석! 세부 논리 흐름과 인과관계를 조금만 더 보완해 보아요."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradientBrush, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Giant score display badge
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${result.score}",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "점수",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.grade,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = gradeDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Bento 1.5: Detailed Metrics Grid (의미 일치도 & 문맥 보존도)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Card: 의미 일치도
                    SquircleCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "의미 일치도",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${result.semanticMatch}%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = PrimaryBlue
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { result.semanticMatch.toFloat() / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PrimaryBlue,
                                trackColor = PrimaryBlue.copy(alpha = 0.1f)
                            )
                        }
                    }

                    // Right Card: 문맥 보존도
                    SquircleCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "문맥 보존도",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${result.contextPreservation}%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = SuccessEmerald
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { result.contextPreservation.toFloat() / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SuccessEmerald,
                                trackColor = SuccessEmerald.copy(alpha = 0.1f)
                            )
                        }
                    }
                }

                // Bento 1.55: Logic Checks Quick Status
                SquircleCard {
                    Text(
                        text = "논리력 3대 검증 요약",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Premise check
                        LogicQuickStatusItem(
                            title = "기본 전제",
                            passed = result.premiseCheck,
                            modifier = Modifier.weight(1f)
                        )
                        // Inference check
                        LogicQuickStatusItem(
                            title = "추론 일관성",
                            passed = result.inferenceCheck,
                            modifier = Modifier.weight(1f)
                        )
                        // Exception check
                        LogicQuickStatusItem(
                            title = "예외 모순",
                            passed = result.exceptionCheck,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Bento 1.6: Submitted Summary Card
                SquircleCard {
                    Text(
                        text = "내가 제출한 요약문",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.userSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = if (expandUserSummary) Int.MAX_VALUE else 3,
                        onTextLayout = { textLayoutResult ->
                            if (!expandUserSummary) {
                                hasSummaryOverflow = textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 3
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (hasSummaryOverflow) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { expandUserSummary = !expandUserSummary },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = if (expandUserSummary) "접기" else "...더보기",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                // Brief Screen CTA Button
                AnimatedVisibility(
                    visible = !showDetailedReport,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(
                        onClick = { showDetailedReport = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        )
                    ) {
                        Text(
                            text = "상세 리포트 보기",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                // Detailed Report Card sections with entry animation
                AnimatedVisibility(
                    visible = showDetailedReport,
                    enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { it / 3 }),
                    exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { it / 3 })
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Bento 1.7: AI 학습 총평 Card (Flat Column to fix double-border rendering bug)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(PrimaryBlue.copy(alpha = 0.03f))
                                .border(1.dp, PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "피드백",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI 학습 피드백",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.aiFeedback,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Bento 1.8: Collapsible Original Passage Card
                        val passage = passageState
                        if (passage != null) {
                            var expandPassage by remember(result.id) { mutableStateOf(false) }
                            SquircleCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "지문 원문 보기",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    TextButton(
                                        onClick = { expandPassage = !expandPassage },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (expandPassage) "접기" else "펼치기",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = passage.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = if (expandPassage) Int.MAX_VALUE else 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .padding(bottom = 4.dp)
                                )
                            }
                        }

                        // Swipeable logic analysis details (Premise, Inference, and Exception check)
                        SquircleCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "논리 세부 분석",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${pagerState.currentPage + 1} / 3",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryBlue
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp)
                                ) { page ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        verticalArrangement = Arrangement.Top
                                    ) {
                                        when (page) {
                                            0 -> {
                                                Text(
                                                    text = "기본 전제 분석",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                if (result.premiseCheck) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = "일치", tint = SuccessEmerald, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("전제 정합함", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SuccessEmerald)
                                                    }
                                                } else {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Close, contentDescription = "오류", tint = WarningOrange, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("전제 보완 필요", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = WarningOrange)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(result.premiseDetail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            1 -> {
                                                Text(
                                                    text = "추론 일관성",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                if (result.inferenceCheck) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = "안전", tint = SuccessEmerald, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("비약 없음", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SuccessEmerald)
                                                    }
                                                } else {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.ErrorOutline, contentDescription = "오류 발견", tint = WarningOrange, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("논리 비약 발견", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = WarningOrange)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(result.inferenceDetail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            2 -> {
                                                Text(
                                                    text = "예외 사례 검증",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                if (result.exceptionCheck) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = "안전", tint = SuccessEmerald, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("모순 없음", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SuccessEmerald)
                                                    }
                                                } else {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.ErrorOutline, contentDescription = "모순 발견", tint = WarningOrange, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("예외 모순 발견", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = WarningOrange)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(result.exceptionDetail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Indicator dots
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    repeat(3) { index ->
                                        val color = if (pagerState.currentPage == index) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                                        val size = if (pagerState.currentPage == index) 8.dp else 6.dp
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(size)
                                                .background(color, CircleShape)
                                        )
                                    }
                                }
                            }
                        }

                        // AI Model Summary Card
                        SquircleCard {
                            Text(
                                text = "AI 모범 요약문",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.aiSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Bento 4: Corrected Text (Original vs Sugested)
                        SquircleCard {
                            Text(
                                text = "요약문 첨삭 피드백",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "작성한 요약문:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = result.userSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 12.dp)
                            )

                            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "추천 첨삭 요약문:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Text(
                                text = if (result.correctedText.isBlank()) {
                                    "의미 있는 문장이 아니어서 첨삭 요약문을 제공할 수 없습니다."
                                } else {
                                    result.correctedText
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = if (result.correctedText.isBlank()) FontStyle.Normal else FontStyle.Italic
                                ),
                                color = if (result.correctedText.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else PrimaryBlue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }

                        // Bento 5: Multi-turn Chat Q&A interface (FR-05)
                        SquircleCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = "학습 대화",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI 튜터와 일대일 질의응답",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Chat messages area (max height and scrollable internally, or just list)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chatHistory.forEach { msg ->
                                    val isUser = msg.role == "user"
                                    if (isUser) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = 12.dp,
                                                            topEnd = 12.dp,
                                                            bottomStart = 12.dp,
                                                            bottomEnd = 2.dp
                                                        )
                                                    )
                                                    .background(PrimaryBlue)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    .widthIn(max = 260.dp)
                                            ) {
                                                Text(
                                                    text = msg.content,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "AI",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = 12.dp,
                                                            topEnd = 12.dp,
                                                            bottomStart = 2.dp,
                                                            bottomEnd = 12.dp
                                                        )
                                                    )
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    .widthIn(max = 220.dp)
                                            ) {
                                                Text(
                                                    text = msg.content,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isGenerating) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI 튜터가 생각 중입니다...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                errorMsg?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Input field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chatInput,
                                    onValueChange = { viewModel.updateChatInput(it) },
                                    placeholder = { Text("피드백에 대해 질문해 보세요...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                IconButton(
                                    onClick = { viewModel.sendChatMessage() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(PrimaryBlue)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "전송",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { onTabSelected("home") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "대시보드로 돌아가기",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LogicQuickStatusItem(
    title: String,
    passed: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = if (passed) SuccessEmerald else WarningOrange
        val icon = if (passed) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
        val bg = color.copy(alpha = 0.1f)

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (passed) "적합" else "보완 필요",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
