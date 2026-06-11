package com.example.logicflow.ui.reading

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.logicflow.data.local.PassageEntity
import com.example.logicflow.ui.components.LogicFlowBottomNavBar
import com.example.logicflow.ui.components.LogicFlowTopAppBar
import com.example.logicflow.ui.components.SquircleCard
import com.example.logicflow.ui.theme.BorderLight
import com.example.logicflow.ui.theme.PrimaryBlue
import com.example.logicflow.ui.theme.SuccessEmerald
import com.example.logicflow.ui.theme.WarningOrange
import java.util.Locale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.School
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import android.content.SharedPreferences
import androidx.compose.material.icons.filled.HelpOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel,
    onResultGenerated: (String) -> Unit,
    onMenuClick: () -> Unit,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    val selectedPassage by viewModel.selectedPassage.collectAsState()
    val passages by viewModel.filteredPassages.collectAsState()
    val filter by viewModel.difficultyFilter.collectAsState()
    val timerSec by viewModel.timerSeconds.collectAsState()
    val summaryText by viewModel.userSummaryText.collectAsState()
    val sttPartialText by viewModel.sttPartialText.collectAsState()
    val sttVolume by viewModel.sttVolume.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val evalState by viewModel.evaluationState.collectAsState()

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("logicflow_prefs", Context.MODE_PRIVATE) }
    var showTutorial by remember { mutableStateOf(!sharedPrefs.getBoolean("tutorial_reading_completed", false)) }
    var currentTutorialStep by remember { mutableStateOf(0) }

    var parentHeight by remember { mutableStateOf(0) }
    var filterBounds by remember { mutableStateOf<Rect?>(null) }
    var listBounds by remember { mutableStateOf<Rect?>(null) }

    // Observe evaluation state to navigate
    LaunchedEffect(evalState) {
        if (evalState is EvaluationUiState.Success) {
            val resultId = (evalState as EvaluationUiState.Success).resultId
            onResultGenerated(resultId)
            viewModel.resetEvaluationState()
            viewModel.selectPassage(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedPassage == null) {
        // Mode A: Passage Selection Screen
        Scaffold(
            topBar = {
                LogicFlowTopAppBar(
                    title = "요약할 지문 선택",
                    onMenuClick = onMenuClick,
                    onNotificationClick = onNotificationClick,
                    onHelpClick = {
                        sharedPrefs.edit().putBoolean("tutorial_reading_completed", false).apply()
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
                    .onGloballyPositioned { coords ->
                        parentHeight = coords.size.height
                    }
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Difficulty Filters
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            filterBounds = coords.boundsInRoot()
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("상", "중", "하")
                    filters.forEach { name ->
                        val isSelected = filter == name
                        Button(
                            onClick = { viewModel.setDifficultyFilter(name) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Passages list with infinite scroll
                val listState = key(filter) { rememberLazyListState() }
                val isLoading by viewModel.isLoading.collectAsState()
                val hasMore by viewModel.hasMore.collectAsState()



                if (passages.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "해당 난이도의 지문이 존재하지 않습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                            .onGloballyPositioned { coords ->
                                listBounds = coords.boundsInRoot()
                            },
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(passages, key = { it.id }) { passage ->
                            SquircleCard(
                                onClick = { viewModel.selectPassage(passage) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = passage.title,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            // 난이도 배지
                                            val (badgeColor, badgeBg) = when (passage.difficulty) {
                                                "상" -> Pair(Color(0xFFD32F2F), Color(0xFFFFEBEE))
                                                "중" -> Pair(WarningOrange, WarningOrange.copy(alpha = 0.12f))
                                                else -> Pair(SuccessEmerald, SuccessEmerald.copy(alpha = 0.12f))
                                            }
                                            androidx.compose.foundation.layout.Box(
                                                modifier = Modifier
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                                    .background(badgeBg)
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = passage.difficulty,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    ),
                                                    color = badgeColor
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "권장 시간: ${passage.recommendTimeSec / 60}분 ${passage.recommendTimeSec % 60}초",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "이동",
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                        }

                        if (hasMore && !isLoading) {
                            item {
                                Button(
                                    onClick = { viewModel.loadNextPage() },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = PrimaryBlue
                                    )
                                ) {
                                    Text("더보기", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (!hasMore && !isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "지문 추가 개발중입니다..!",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Mode B: Active Reading Workspace (FR-01, FR-02, FR-04)
        val passage = selectedPassage!!

        // STT speech recognition initialization
        var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
        val recognitionListener = remember {
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    viewModel.updateSttVolume(rmsdB)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    viewModel.setRecordingState(false)
                }

                override fun onError(error: Int) {
                    viewModel.setRecordingState(false)
                    viewModel.clearPartialText()
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "오디오 입력 에러"
                        SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
                        SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 시간 초과"
                        SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식하지 못했습니다. 천천히 다시 말씀해 주세요."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성이 감지되지 않았습니다. 마이크를 대고 말씀해 주세요."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식 서비스가 바쁩니다. 잠시 후 다시 시도해 주세요."
                        else -> "음성 인식 에러"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        viewModel.commitPartialText(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        viewModel.updatePartialText(text)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        }

        DisposableEffect(context) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            recognizer.setRecognitionListener(recognitionListener)
            speechRecognizer = recognizer
            onDispose {
                recognizer.destroy()
                speechRecognizer = null
            }
        }

        val micPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted, start recording
                speechRecognizer?.let {
                    startSpeechRecognition(context, it, viewModel)
                }
            } else {
                Toast.makeText(context, "마이크 녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = passage.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.selectPassage(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = PrimaryBlue)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
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

                // Timer indicator (FR-01)
                SquircleCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "시간",
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatTimer(timerSec),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "권장 시간: ${passage.recommendTimeSec / 60}분 ${passage.recommendTimeSec % 60}초",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Scrollable passage contents (Squircles text reader)
                SquircleCard {
                    Text(
                        text = passage.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    )
                }

                // Summary Writing and STT input Area (FR-02, FR-04)
                SquircleCard {
                    Text(
                        text = "지문 요약하기",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val displaySummaryText = if (sttPartialText.isNotEmpty()) {
                        if (summaryText.isEmpty()) sttPartialText else "$summaryText $sttPartialText"
                    } else {
                        summaryText
                    }

                    OutlinedTextField(
                        value = displaySummaryText,
                        onValueChange = { viewModel.updateSummaryText(it) },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // STT recording UI
                    if (isRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlue.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            SoundWaveVisualizer(
                                volume = sttVolume,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "🎙️ 음성 인식 중... 말씀해 주세요.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    speechRecognizer?.stopListening()
                                    viewModel.setRecordingState(false)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("완료", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                                    Toast.makeText(context, "기기에서 음성 인식 서비스를 사용할 수 없습니다. Google 앱 활성화를 확인하세요.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    speechRecognizer?.let {
                                        startSpeechRecognition(context, it, viewModel)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = PrimaryBlue
                            )
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "음성 입력")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("음성으로 요약 입력", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Error message
                if (evalState is EvaluationUiState.Error) {
                    Text(
                        text = (evalState as EvaluationUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Submit button
                Button(
                    onClick = { viewModel.submitSummary() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = evalState !is EvaluationUiState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    if (evalState is EvaluationUiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = "채점")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "요약 채점 요청하기",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (evalState is EvaluationUiState.Loading) {
        LoadingAnimationOverlay()
    }

    if (showTutorial && selectedPassage == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
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
                                filterBounds?.let { bounds ->
                                    addRoundRect(
                                        RoundRect(
                                            rect = Rect(
                                                left = bounds.left - 4.dp.toPx(),
                                                top = bounds.top - 4.dp.toPx(),
                                                right = bounds.right + 4.dp.toPx(),
                                                bottom = bounds.bottom + 4.dp.toPx()
                                            ),
                                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                                        )
                                    )
                                }
                            }
                            2 -> {
                                listBounds?.let { bounds ->
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
                    sharedPrefs.edit().putBoolean("tutorial_reading_completed", true).apply()
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
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "지문 선택 가이드",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "요약 및 독해 훈련을 진행할 지문을 선택하는 공간입니다. 난이도별로 다양한 지문이 준비되어 있습니다.",
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
                    val topOffset = remember(filterBounds) {
                        if (filterBounds != null) {
                            with(density) { filterBounds!!.bottom.toDp() }
                        } else {
                            160.dp
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
                            text = "↑ 난이도 필터링",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        TutorialGuideCard(
                            title = "난이도 선택 필터 ⚡",
                            content = "상, 중, 하 난이도를 터치하여 지문 목록을 필터링할 수 있습니다. 처음 시작할 때는 '하' 난이도부터 차근차근 시작해보는 것을 권장합니다.",
                            currentStep = 1,
                            totalSteps = 2,
                            onNext = { currentTutorialStep = 2 },
                            onPrev = { currentTutorialStep = 0 }
                        )
                    }
                }
                2 -> {
                    val density = LocalDensity.current
                    val topOffset = remember(listBounds) {
                        if (listBounds != null) {
                            with(density) { listBounds!!.top.toDp() }
                        } else {
                            240.dp
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topOffset)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TutorialGuideCard(
                            title = "지문 카드 선택 📄",
                            content = "원하는 지문을 클릭하면 독해 타이머와 함께 요약 작성 화면이 열립니다. 권장 시간을 확인하고 도전해보세요!",
                            currentStep = 2,
                            totalSteps = 2,
                            onNext = {
                                sharedPrefs.edit().putBoolean("tutorial_reading_completed", true).apply()
                                showTutorial = false
                            },
                            onPrev = { currentTutorialStep = 1 }
                        )

                        Text(
                            text = "↓ 지문 목록 영역",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
}

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

@Composable
private fun LoadingAnimationOverlay() {
    val loadingSteps = listOf(
        "AI가 지문과 요약문의 핵심 맥락을 읽어오고 있습니다...",
        "요약문의 논리적 정합성 및 인과관계를 비교 분석하고 있습니다...",
        "지문 속 기본 전제와 숨겨진 추론의 일관성을 검증하는 중입니다...",
        "더 직관적이고 완성도 높은 맞춤형 첨삭 제안을 작성하고 있습니다...",
        "최종 채점 점수와 심층 피드백을 생성하고 있습니다..."
    )
    
    var currentStepIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentStepIndex = (currentStepIndex + 1) % loadingSteps.size
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "loading_anim")
    
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = 0.2f
                            }
                            .background(PrimaryBlue.copy(alpha = 0.4f), CircleShape)
                    )
                    
                    Canvas(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer { rotationZ = angle }
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(PrimaryBlue, Color(0xFF00E5FF), PrimaryBlue.copy(alpha = 0.1f))
                            ),
                            startAngle = 0f,
                            sweepAngle = 300f,
                            useCenter = false,
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = scale * 0.95f
                                    scaleY = scale * 0.95f
                                }
                        )
                    }
                }

                Text(
                    text = "AI 요약 채점 및 정밀 분석 중",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = loadingSteps[currentStepIndex],
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                LinearProgressIndicator(
                    color = PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                Text(
                    text = "분석이 완료되면 자동으로 결과 창으로 이동합니다.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Helpers
private fun startSpeechRecognition(
    context: Context,
    speechRecognizer: SpeechRecognizer,
    viewModel: ReadingViewModel
) {
    try {
        speechRecognizer.cancel() // Cancel any ongoing session to release the mic and reset state
    } catch (e: Exception) {}

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "요약 내용을 말씀해 주세요.")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    try {
        speechRecognizer.startListening(intent)
        viewModel.setRecordingState(true)
    } catch (e: Exception) {
        Toast.makeText(context, "음성 인식을 시작할 수 없습니다: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatTimer(totalSecs: Int): String {
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

@Composable
private fun SoundWaveVisualizer(
    volume: Float,
    modifier: Modifier = Modifier
) {
    val normalizedVolume = remember(volume) {
        // Normalizes volume from (~ -2..12) to (0.1f..1.0f)
        ((volume + 2f) / 14f).coerceIn(0.1f, 1.0f)
    }
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
