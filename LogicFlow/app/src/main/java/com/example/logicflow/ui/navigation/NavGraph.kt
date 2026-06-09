package com.example.logicflow.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.logicflow.data.di.AppContainer
import com.example.logicflow.ui.ViewModelFactory
import com.example.logicflow.ui.analysis.AnalysisResultScreen
import com.example.logicflow.ui.analysis.AnalysisResultViewModel
import com.example.logicflow.ui.components.LogicFlowNavigationDrawer
import com.example.logicflow.ui.history.HistoryScreen
import com.example.logicflow.ui.history.HistoryViewModel
import com.example.logicflow.ui.home.HomeScreen
import com.example.logicflow.ui.home.HomeViewModel
import com.example.logicflow.ui.reading.ReadingScreen
import com.example.logicflow.ui.reading.ReadingViewModel
import com.example.logicflow.ui.settings.SettingsScreen
import com.example.logicflow.ui.settings.SettingsViewModel
import com.example.logicflow.ui.stats.StatsScreen
import com.example.logicflow.ui.stats.StatsViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import com.example.logicflow.data.local.NotificationLogEntity
import com.example.logicflow.data.repository.LogicFlowRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogicFlowNavGraph(
    navController: NavHostController,
    appContainer: AppContainer
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showNotificationDialog by remember { mutableStateOf(false) }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val context = LocalContext.current
    val factory = remember(appContainer.repository) {
        ViewModelFactory(appContainer.repository, context)
    }

    val onNotificationClick: () -> Unit = {
        showNotificationDialog = true
    }

    val navigateTab: (String) -> Unit = { tab ->
        navController.navigate(tab) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LogicFlowNavigationDrawer(
        drawerState = drawerState,
        currentRoute = currentRoute,
        onNavigate = { route ->
            coroutineScope.launch { drawerState.close() }
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                val homeViewModel: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = homeViewModel,
                    onStartReading = { navigateTab("reading") },
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    currentRoute = currentRoute,
                    onTabSelected = navigateTab,
                    onNotificationClick = onNotificationClick
                )
            }

            composable("history") {
                val historyViewModel: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(
                    viewModel = historyViewModel,
                    onResultSelected = { resultId ->
                        navController.navigate("analysis/$resultId")
                    },
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    currentRoute = currentRoute,
                    onTabSelected = navigateTab,
                    onNotificationClick = onNotificationClick
                )
            }

            composable("stats") {
                val statsViewModel: StatsViewModel = viewModel(factory = factory)
                val historyViewModel: HistoryViewModel = viewModel(factory = factory)
                StatsScreen(
                    viewModel = statsViewModel,
                    historyViewModel = historyViewModel,
                    onResultSelected = { resultId ->
                        navController.navigate("analysis/$resultId")
                    },
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    currentRoute = currentRoute,
                    onTabSelected = navigateTab,
                    onNotificationClick = onNotificationClick
                )
            }

            composable("reading") {
                val readingViewModel: ReadingViewModel = viewModel(factory = factory)
                ReadingScreen(
                    viewModel = readingViewModel,
                    onResultGenerated = { resultId ->
                        navController.navigate("analysis/$resultId") {
                            popUpTo("home") { saveState = true }
                        }
                    },
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    currentRoute = currentRoute,
                    onTabSelected = navigateTab,
                    onNotificationClick = onNotificationClick
                )
            }

            composable(
                route = "analysis/{resultId}",
                arguments = listOf(navArgument("resultId") { type = NavType.StringType })
            ) { backStackEntry ->
                val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
                val analysisViewModel: AnalysisResultViewModel = viewModel(factory = factory)
                AnalysisResultScreen(
                    viewModel = analysisViewModel,
                    resultId = resultId,
                    onBackClick = { navController.popBackStack() },
                    currentRoute = currentRoute,
                    onTabSelected = navigateTab
                )
            }

            composable("settings") {
                val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() },
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    onNotificationClick = onNotificationClick
                )
            }
        }
    }

    if (showNotificationDialog) {
        NotificationStatusDialog(
            repository = appContainer.repository,
            onDismiss = { showNotificationDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationStatusDialog(
    repository: LogicFlowRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasStudied by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val isEnabled = remember { repository.isLearningNotificationEnabled() }
    val hour = remember { repository.getLearningNotificationHour() }

    LaunchedEffect(Unit) {
        hasStudied = repository.hasStudiedToday()
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "알림",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "오늘의 학습 및 알림 상태",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasStudied) com.example.logicflow.ui.theme.SuccessEmerald.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasStudied) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = if (hasStudied) "완료" else "미완료",
                                tint = if (hasStudied) com.example.logicflow.ui.theme.SuccessEmerald else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (hasStudied) "오늘 학습 완료!" else "오늘 학습 미완료",
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasStudied) com.example.logicflow.ui.theme.SuccessEmerald else MaterialTheme.colorScheme.error,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (hasStudied) "오늘의 독해 요약 과제를 완수하셨습니다. 훌륭합니다!" else "아직 요약 능력을 기르지 않으셨습니다. 서둘러보세요!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "알림 수신 내역 (스와이프하여 삭제)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    val logs by repository.getAllNotificationLogs().collectAsState(initial = emptyList())
                    val dateFormat = remember { SimpleDateFormat("MM월 dd일 HH:mm", Locale.KOREAN) }

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "알림 수신 내역이 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = logs,
                                key = { it.id }
                            ) { log ->
                                val currentLog = rememberUpdatedState(log)
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            coroutineScope.launch {
                                                repository.deleteNotificationLogById(currentLog.value.id)
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = MaterialTheme.colorScheme.errorContainer
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(color, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 16.dp),
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
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = log.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = dateFormat.format(Date(log.timestamp)),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = log.content,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val title = "학습 알림 테스트"
                        val content = "알림이 정상적으로 작동하고 있습니다! 오늘의 학습을 완료해 보세요."
                        com.example.logicflow.notification.NotificationHelper.showNotification(
                            context = context,
                            title = title,
                            content = content
                        )
                        coroutineScope.launch {
                            repository.insertNotificationLog(
                                NotificationLogEntity(
                                    id = UUID.randomUUID().toString(),
                                    title = title,
                                    content = content,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                ) {
                    Text("즉시 알림 테스트", color = com.example.logicflow.ui.theme.PrimaryBlue, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.logicflow.ui.theme.PrimaryBlue)
                ) {
                    Text("확인")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
