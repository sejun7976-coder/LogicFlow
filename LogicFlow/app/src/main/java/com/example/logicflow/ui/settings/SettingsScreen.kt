package com.example.logicflow.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.logicflow.ui.components.LogicFlowTopAppBar
import com.example.logicflow.ui.components.SquircleCard
import com.example.logicflow.ui.theme.PrimaryBlue
import com.example.logicflow.ui.theme.SuccessEmerald
import com.example.logicflow.ui.theme.WarningOrange
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val savedApiKey by viewModel.apiKey.collectAsState()
    val savedAiHubApiKey by viewModel.aiHubApiKey.collectAsState()
    val savedModelName by viewModel.modelName.collectAsState()
    val testState by viewModel.apiTestState.collectAsState()
    val savedNotificationEnabled by viewModel.notificationEnabled.collectAsState()
    val savedNotificationHour by viewModel.notificationHour.collectAsState()

    var apiKeyText by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var aiHubApiKeyText by remember(savedAiHubApiKey) { mutableStateOf(savedAiHubApiKey) }
    var modelNameText by remember(savedModelName) { mutableStateOf(savedModelName) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isAiHubPasswordVisible by remember { mutableStateOf(false) }
    var showSavedToast by remember { mutableStateOf(false) }
    var showLevelUpPreview by remember { mutableStateOf(false) }

    var notificationEnabledState by remember(savedNotificationEnabled) { mutableStateOf(savedNotificationEnabled) }
    var notificationHourState by remember(savedNotificationHour) { mutableStateOf(savedNotificationHour) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationEnabledState = true
        } else {
            notificationEnabledState = false
        }
    }

    val onNotificationToggle: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    notificationEnabledState = true
                } else {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                notificationEnabledState = true
            }
        } else {
            notificationEnabledState = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            LogicFlowTopAppBar(
                title = "설정",
                onMenuClick = onMenuClick,
                onNotificationClick = onNotificationClick
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "알림 설정",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            SquircleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "학습 알림받기",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "매일 지정 시간까지 학습하지 않을 경우 알림",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = notificationEnabledState,
                        onCheckedChange = onNotificationToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = PrimaryBlue
                        )
                    )
                }

                if (notificationEnabledState) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "알림 시간 설정: 매일 ${notificationHourState}시까지",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = notificationHourState.toFloat(),
                        onValueChange = { notificationHourState = it.toInt() },
                        valueRange = 1f..24f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1시", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("12시", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("24시", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "API 서비스 설정",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            // ── Gemini API 키 입력 ──────────────────────────────────
            SquircleCard {
                Text(
                    text = "LLM (Google Gemini) API 키",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        viewModel.resetTestState()
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "비밀번호 보이기/숨기기"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── API 연결 테스트 버튼 ────────────────────────────
                Button(
                    onClick = {
                        viewModel.testApiConnection(apiKeyText, modelNameText)
                    },
                    enabled = testState !is ApiTestState.Loading && apiKeyText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = PrimaryBlue,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    if (testState is ApiTestState.Loading) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "연결 테스트 중...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Icon(Icons.Default.Wifi, contentDescription = "테스트", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "API 연결 테스트",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // ── 테스트 결과 표시 ────────────────────────────────
                AnimatedVisibility(
                    visible = testState is ApiTestState.Success || testState is ApiTestState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    when (val state = testState) {
                        is ApiTestState.Success -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        SuccessEmerald.copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "성공",
                                    tint = SuccessEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = SuccessEmerald
                                )
                            }
                        }
                        is ApiTestState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "오류",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }

            // ── AI Hub API 키 ────────────────────────────────────────
            SquircleCard {
                Text(
                    text = "공공데이터 (AI Hub) API 키",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = aiHubApiKeyText,
                    onValueChange = { aiHubApiKeyText = it },
                    label = { Text("AI Hub API Key") },
                    placeholder = { Text("인증키 입력...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (isAiHubPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isAiHubPasswordVisible = !isAiHubPasswordVisible }) {
                            Icon(
                                imageVector = if (isAiHubPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "비밀번호 보이기/숨기기"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue
                    )
                )
            }

            // ── 모델 선택 ────────────────────────────────────────────
            SquircleCard {
                Text(
                    text = "사용할 AI 모델 선택",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val models = listOf("gemini-3.5-flash", "gemini-2.5-flash")
                    models.forEach { model ->
                        val isSelected = modelNameText == model
                        Button(
                            onClick = {
                                modelNameText = model
                                viewModel.resetTestState()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = when (model) {
                                    "gemini-3.5-flash" -> "3.5 Flash"
                                    "gemini-2.5-flash" -> "2.5 Flash"
                                    else -> model
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // 429 안내
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarningOrange.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "안내",
                        tint = WarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "429 오류(한도 초과) 발생 시 자동으로 최대 3회 재시도합니다. 무료 키는 분당 요청 횟수 제한이 있습니다.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = WarningOrange
                    )
                }
            }

            // ── 튜토리얼 설정 카드 ──────────────────────────────────
            var showTutorialResetToast by remember { mutableStateOf(false) }

            Text(
                text = "학습 가이드 및 도움말",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            SquircleCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "대시보드 튜토리얼",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "대시보드의 구성과 사용법을 다시 확인하고 싶으시다면 아래 버튼을 눌러주세요. 대시보드로 돌아갔을 때 다시 친절한 가이드가 표시됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Button(
                        onClick = {
                            viewModel.resetTutorial()
                            showTutorialResetToast = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Text(
                            text = "대시보드 튜토리얼 다시 보기",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (showTutorialResetToast) {
                        Text(
                            text = "튜토리얼이 재설정되었습니다. 대시보드에서 가이드가 실행됩니다.",
                            color = SuccessEmerald,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SquircleCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "등급 승급 연출",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "학습 결과를 쌓아 등급이 상승했을 때 나타나는 특별한 축하 연출을 미리 감상할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Button(
                        onClick = {
                            showLevelUpPreview = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Text(
                            text = "등급 승급 연출 미리보기",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 안내 카드 ────────────────────────────────────────────
            SquircleCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "안내",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "API Key가 비어있는 경우 '데모 모드'로 실행됩니다. 데모 모드에서는 로컬의 미리 구성된 피드백 데이터가 제공되므로 안전하게 앱을 둘러보실 수 있습니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showSavedToast) {
                Text(
                    text = "설정이 성공적으로 저장되었습니다.",
                    color = SuccessEmerald,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    viewModel.saveSettings(apiKeyText, aiHubApiKeyText, modelNameText, notificationEnabledState, notificationHourState)
                    showSavedToast = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = "저장")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "설정 저장하기",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLevelUpPreview) {
        val infiniteTransition = rememberInfiniteTransition(label = "levelup_preview_anim")
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
                        Text(
                            text = "논리 전문가",
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
                            text = "논리 마스터",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showLevelUpPreview = false },
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
}
}
