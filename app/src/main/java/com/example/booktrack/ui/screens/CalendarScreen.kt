package com.example.booktrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.booktrack.data.local.ReadingSessionEntity
import com.example.booktrack.ui.BookViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: BookViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val allSessions = uiState.allSessions

    val todayCal = remember { Calendar.getInstance() }
    var displayYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }

    val sessionsByDay = remember(allSessions) {
        allSessions.groupBy { normalizeToMidnight(it.startTimeMs) }
    }

    var selectedDayMs by remember { mutableStateOf<Long?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val monthLabel = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance().apply { set(displayYear, displayMonth, 1) }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Calendar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthNavRow(
                label = monthLabel,
                onPrev = {
                    if (displayMonth == 0) {
                        displayMonth = 11
                        displayYear -= 1
                    } else {
                        displayMonth -= 1
                    }
                },
                onNext = {
                    if (displayMonth == 11) {
                        displayMonth = 0
                        displayYear += 1
                    } else {
                        displayMonth += 1
                    }
                }
            )

            DayOfWeekHeader()

            MonthGrid(
                year = displayYear,
                month = displayMonth,
                todayCal = todayCal,
                sessionsByDay = sessionsByDay,
                onDayClick = { dayMs ->
                    if (sessionsByDay.containsKey(dayMs)) {
                        selectedDayMs = dayMs
                        showSheet = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            CalendarLegend()
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                DayDetailContent(
                    dayMs = selectedDayMs ?: return@ModalBottomSheet,
                    sessions = sessionsByDay[selectedDayMs] ?: emptyList()
                )
            }
        }
    }
}

@Composable
private fun MonthNavRow(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.testTag("calendar_prev_month")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("calendar_month_label")
        )
        IconButton(onClick = onNext, modifier = Modifier.testTag("calendar_next_month")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        dayLabels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    todayCal: Calendar,
    sessionsByDay: Map<Long, List<ReadingSessionEntity>>,
    onDayClick: (Long) -> Unit
) {
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val firstDayCal = Calendar.getInstance().apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells: List<Int?> = List(firstDayOfWeek) { null } + (1..daysInMonth).toList()
    val padded = cells + List((7 - cells.size % 7) % 7) { null }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        padded.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val dayCal = Calendar.getInstance().apply {
                                set(year, month, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val dayMs = dayCal.timeInMillis
                            val isToday = year == todayYear && month == todayMonth && day == todayDay
                            val hasSessions = sessionsByDay.containsKey(dayMs)
                            DayCell(
                                day = day,
                                isToday = isToday,
                                hasSessions = hasSessions,
                                onClick = { onDayClick(dayMs) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    hasSessions: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        hasSessions -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        hasSessions -> MaterialTheme.colorScheme.onPrimaryContainer
        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(bgColor)
            .testTag("day_cell_$day")
            .clickable(enabled = hasSessions, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday || hasSessions) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primaryContainer, label = "Read")
        LegendItem(color = MaterialTheme.colorScheme.secondaryContainer, label = "Today")
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayDetailContent(dayMs: Long, sessions: List<ReadingSessionEntity>) {
    val dateLabel = remember(dayMs) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(dayMs))
    }
    val totalMinutes = sessions.sumOf { it.durationMs } / 60_000
    val totalPages = sessions.sumOf { (it.endPage - it.startPage).coerceAtLeast(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$totalPages pages · ${totalMinutes}m total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(sessions, key = { it.id }) { session ->
                    DaySessionRow(session)
                    if (session != sessions.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DaySessionRow(session: ReadingSessionEntity) {
    val pagesRead = (session.endPage - session.startPage).coerceAtLeast(0)
    val durationText = run {
        val totalSeconds = session.durationMs / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = session.bookTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pages ${session.startPage} → ${session.endPage}  ($pagesRead pages)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun normalizeToMidnight(ms: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ms
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
