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

    // Observe evaluation state to navigate
    LaunchedEffect(evalState) {
        if (evalState is EvaluationUiState.Success) {
            val resultId = (evalState as EvaluationUiState.Success).resultId
            onResultGenerated(resultId)
            viewModel.resetEvaluationState()
            viewModel.selectPassage(null)
        }
    }

    if (selectedPassage == null) {
        // Mode A: Passage Selection Screen
        Scaffold(
            topBar = {
                LogicFlowTopAppBar(
                    title = "요약할 지문 선택",
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
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Difficulty Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.weight(1f),
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
