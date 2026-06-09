package com.example.logicflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logicflow.ui.theme.BorderLight
import com.example.logicflow.ui.theme.PrimaryBlue

@Composable
fun LogicFlowTopAppBar(
    title: String,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    showMenu: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(width = 0.5.dp, color = BorderLight.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showMenu) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "메뉴",
                        tint = PrimaryBlue
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "알림",
                    tint = PrimaryBlue
                )
            }
        }
    }
}

@Composable
fun LogicFlowBottomNavBar(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .border(width = 0.5.dp, color = BorderLight.copy(alpha = 0.5f))
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                label = "대시보드",
                icon = Icons.Default.Dashboard,
                isActive = currentRoute == "home",
                onClick = { onTabSelected("home") }
            )
            NavBarItem(
                label = "읽기",
                icon = Icons.Default.MenuBook,
                isActive = currentRoute == "reading",
                onClick = { onTabSelected("reading") }
            )
            NavBarItem(
                label = "결과",
                icon = Icons.Default.BarChart,
                isActive = currentRoute == "stats" || currentRoute.startsWith("analysis/"),
                onClick = { onTabSelected("stats") }
            )
        }
    }
}

@Composable
private fun RowScope.NavBarItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isActive) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent)
                .padding(vertical = 6.dp, horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isActive) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SquircleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, RoundedCornerShape(20.dp)) else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun LogicFlowNavigationDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "LogicFlow",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "대시보드") },
                    label = { Text("대시보드", style = MaterialTheme.typography.bodyMedium) },
                    selected = currentRoute == "home",
                    onClick = {
                        onNavigate("home")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "저장된 노트") },
                    label = { Text("저장된 노트", style = MaterialTheme.typography.bodyMedium) },
                    selected = currentRoute == "history",
                    onClick = {
                        onNavigate("history")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "학습 통계") },
                    label = { Text("학습 통계", style = MaterialTheme.typography.bodyMedium) },
                    selected = currentRoute == "stats",
                    onClick = {
                        onNavigate("stats")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.weight(1f))
                
                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
                    label = { Text("설정", style = MaterialTheme.typography.bodyMedium) },
                    selected = currentRoute == "settings",
                    onClick = {
                        onNavigate("settings")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        },
        content = content
    )
}
