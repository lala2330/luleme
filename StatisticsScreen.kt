package com.luleme.ui.screens.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.luleme.ui.components.CuteCard
import com.luleme.ui.theme.CutePink
import com.luleme.ui.theme.CuteYellow
import com.luleme.ui.theme.PrimaryLight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("本周", "本月", "全部")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> WeekView(
                weekData = uiState.weekData,
                weekLabel = uiState.weekLabel,
                currentWeekOffset = uiState.currentWeekOffset,
                onWeekChanged = { viewModel.navigateWeek(it) }
            )
            1 -> MonthView(
                monthData = uiState.monthData,
                monthLabel = uiState.monthLabel,
                currentMonthOffset = uiState.currentMonthOffset,
                onMonthChanged = { viewModel.navigateMonth(it) }
            )
            2 -> AllTimeView(uiState)
        }
    }
}

@Composable
fun WeekView(
    weekData: Map<DayOfWeek, Int>,
    weekLabel: String,
    currentWeekOffset: Int,
    onWeekChanged: (Int) -> Unit
) {
    val pageOffset = 2000
    val pagerState = rememberPagerState(
        initialPage = currentWeekOffset + pageOffset,
        pageCount = { 4000 }
    )

    // Sync pager position when offset changes externally
    LaunchedEffect(currentWeekOffset) {
        val targetPage = currentWeekOffset + pageOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Notify ViewModel when user swipes to a different page
    LaunchedEffect(pagerState.currentPage) {
        val newOffset = pagerState.currentPage - pageOffset
        if (newOffset != currentWeekOffset) {
            onWeekChanged(newOffset)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        WeekContent(weekData = weekData, weekLabel = weekLabel)
    }
}

@Composable
private fun WeekContent(weekData: Map<DayOfWeek, Int>, weekLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Week label
        Text(
            text = weekLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        val total = weekData.values.sum()
        Text(
            text = "$total",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("周记录", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))

        if (weekData.isNotEmpty()) {
            val maxCount = weekData.values.maxOrNull() ?: 1
            var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                DayOfWeek.entries.forEach { day ->
                    val count = weekData[day] ?: 0
                    val isSelected = selectedDay == day
                    val heightFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
                    
                    var animatedHeight by remember { mutableStateOf(0f) }
                    
                    LaunchedEffect(count) {
                        animatedHeight = heightFraction
                    }
                    
                    val animatedFraction by animateFloatAsState(
                        targetValue = animatedHeight,
                        animationSpec = tween(durationMillis = 800, delayMillis = day.ordinal * 100)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDay = if (isSelected) null else day }
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .fillMaxHeight(if (animatedFraction < 0.05f && count > 0) 0.05f else animatedFraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (LocalDate.now().dayOfWeek == day) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = if (LocalDate.now().dayOfWeek == day) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthView(
    monthData: Map<LocalDate, Int>,
    monthLabel: String,
    currentMonthOffset: Int,
    onMonthChanged: (Int) -> Unit
) {
    val pageOffset = 2000
    val pagerState = rememberPagerState(
        initialPage = currentMonthOffset + pageOffset,
        pageCount = { 4000 }
    )

    // Sync pager position when offset changes externally
    LaunchedEffect(currentMonthOffset) {
        val targetPage = currentMonthOffset + pageOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Notify ViewModel when user swipes
    LaunchedEffect(pagerState.currentPage) {
        val newOffset = pagerState.currentPage - pageOffset
        if (newOffset != currentMonthOffset) {
            onMonthChanged(newOffset)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        MonthContent(monthData = monthData, monthLabel = monthLabel)
    }
}

@Composable
private fun MonthContent(monthData: Map<LocalDate, Int>, monthLabel: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        CuteCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📅 发射足迹", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        monthLabel, 
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Days header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { 
                        Text(
                            text = it, 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val dates = monthData.keys.sorted()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.height(300.dp)
                ) {
                    val firstDay = dates.firstOrNull()
                    if (firstDay != null) {
                        val paddingDays = firstDay.dayOfWeek.value - 1
                        items(paddingDays) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        }
                    }

                    items(dates) { date ->
                        val count = monthData[date] ?: 0
                        val isToday = date == LocalDate.now()
                        val textColor = when {
                            count >= 3 -> Color.White
                            count == 2 -> MaterialTheme.colorScheme.onTertiary
                            count == 1 -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    color = when {
                                        count >= 3 -> MaterialTheme.colorScheme.error
                                        count == 2 -> MaterialTheme.colorScheme.tertiary
                                        count == 1 -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count >= 3) {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else if (count > 0) {
                                // Empty for 1-2, just color
                            }

                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            
                            if (isToday && count == 0) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllTimeView(state: StatisticsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(title = "累计记录", value = "${state.totalCount}", unit = "次")
        StatCard(title = "最长连续", value = "${state.maxStreak}", unit = "天")
        StatCard(title = "平均频率", value = String.format("%.1f", state.averageFrequency), unit = "次/周")
        
        AdviceCard(frequency = state.averageFrequency)
    }
}

@Composable
fun AdviceCard(frequency: Float) {
    val message = when {
        frequency < 1.0f -> "频率控制得很好，继续保持！👍"
        frequency in 1.0f..3.0f -> "频率适中，生活很健康哦~ ✨"
        frequency > 3.0f -> "最近有点频繁，要注意身体休息哦 🛌"
        else -> "开始记录你的生活吧！"
    }
    
    CuteCard(
        backgroundColor = if (frequency > 3.0f) MaterialTheme.colorScheme.tertiaryContainer 
                         else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column {
            Text(
                text = "💡 近期建议",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (frequency > 3.0f) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (frequency > 3.0f) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, unit: String) {
    CuteCard {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
