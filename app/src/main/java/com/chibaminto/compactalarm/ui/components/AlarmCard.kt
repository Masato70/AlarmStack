package com.chibaminto.compactalarm.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chibaminto.compactalarm.R
import com.chibaminto.compactalarm.data.CardData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AlarmCard(
    cardData: CardData,
    childAlarms: List<CardData>,
    onToggleEnabled: (String, Boolean) -> Unit,
    onAddTime: (String) -> Unit,
    onDelete: (String) -> Unit,
    onWeekdaysChanged: (String, List<DayOfWeek>) -> Unit,
    onEditTime: (String, LocalTime) -> Unit,
    onEditLabel: (String, String) -> Unit,
    onToggleVibrationOnly: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLabelDialog by remember { mutableStateOf(false) }

    SwipeToDeleteContainer(
        onDelete = { onDelete(cardData.id) },
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0xFF00E5FF).copy(alpha = 0.15f)
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2A2A2A),
                                Color(0xFF1F1F1F),
                                Color(0xFF252525)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // メインの時刻とスイッチ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatTime(cardData.alarmTime),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (cardData.isEnabled) Color.White
                                else Color.White.copy(alpha = 0.38f),
                                modifier = Modifier.clickable {
                                    onEditTime(cardData.id, cardData.alarmTime)
                                }
                            )
                            if (cardData.isEnabled) {
                                val nextDesc = getNextAlarmDescription(
                                    context, cardData.alarmTime, cardData.selectedWeekdays
                                )
                                if (nextDesc.isNotEmpty()) {
                                    Text(
                                        text = nextDesc,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF00E5FF).copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = Color.White.copy(alpha = 0.2f)
                                )
                                .background(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Switch(
                                checked = cardData.isEnabled,
                                onCheckedChange = { onToggleEnabled(cardData.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E5FF),
                                    checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ラベル＋バイブレーション切替
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ラベル
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showLabelDialog = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = null,
                                tint = if (cardData.label.isNotEmpty())
                                    Color(0xFF00E5FF).copy(alpha = 0.7f)
                                else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cardData.label.ifEmpty { stringResource(R.string.add_label) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (cardData.label.isNotEmpty())
                                    Color.White.copy(alpha = 0.8f)
                                else Color.White.copy(alpha = 0.3f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // バイブレーションのみ切替
                        Surface(
                            onClick = {
                                onToggleVibrationOnly(cardData.id, !cardData.isVibrationOnly)
                            },
                            color = if (cardData.isVibrationOnly)
                                Color(0xFFFF9800).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (cardData.isVibrationOnly)
                                        Color(0xFFFF9800).copy(alpha = 0.5f)
                                    else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (cardData.isVibrationOnly)
                                        Color(0xFFFF9800)
                                    else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (cardData.isVibrationOnly)
                                        stringResource(R.string.vibration_only)
                                    else
                                        stringResource(R.string.sound),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cardData.isVibrationOnly)
                                        Color(0xFFFF9800)
                                    else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 曜日セレクター
                    WeekdaySelector(
                        selectedWeekdays = cardData.selectedWeekdays,
                        onWeekdaysChanged = { onWeekdaysChanged(cardData.id, it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 時間追加ボタン
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        TextButton(
                            onClick = { onAddTime(cardData.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.add_time),
                                color = Color(0xFF00E5FF),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // 子アラーム
                    AnimatedVisibility(
                        visible = childAlarms.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            childAlarms.forEach { child ->
                                key(child.id) {
                                    ChildAlarmItem(
                                        cardData = child,
                                        onToggleEnabled = onToggleEnabled,
                                        onDelete = onDelete,
                                        onEditTime = onEditTime
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ラベル編集ダイアログ
    if (showLabelDialog) {
        LabelEditDialog(
            currentLabel = cardData.label,
            onConfirm = { newLabel ->
                onEditLabel(cardData.id, newLabel)
                showLabelDialog = false
            },
            onDismiss = { showLabelDialog = false }
        )
    }
}

// ───────── ラベル編集ダイアログ ─────────

@Composable
private fun LabelEditDialog(
    currentLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 30) text = it },
                placeholder = { Text(stringResource(R.string.label_placeholder)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color(0xFF00E5FF),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text(stringResource(R.string.save), color = Color(0xFF00E5FF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = Color(0xFF2A2A2A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// ───────── スワイプ削除コンテナ ─────────

@Composable
private fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val deleteThreshold = with(LocalDensity.current) { 250.dp.toPx() }
    var isDeleting by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isDeleting) {
            if (offsetX < 0) -deleteThreshold * 2 else deleteThreshold * 2
        } else offsetX,
        label = "swipe_offset",
        finishedListener = { if (isDeleting) onDelete() }
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (abs(offsetX) > deleteThreshold / 2) Color(0xFFEF5350)
        else Color(0xFFEF5350).copy(alpha = 0.3f),
        label = "background_color"
    )

    Box(modifier = modifier) {
        if (offsetX < 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete, stringResource(R.string.delete),
                        tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                    Text(
                        stringResource(R.string.delete_label),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        if (offsetX > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                    .padding(start = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete, stringResource(R.string.delete),
                        tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                    Text(
                        stringResource(R.string.delete_label),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(offsetX) > deleteThreshold) isDeleting = true
                            else offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount)
                                .coerceIn(-deleteThreshold * 1.5f, deleteThreshold * 1.5f)
                        }
                    )
                }
        ) {
            content()
        }
    }
}

// ───────── 子アラーム ─────────

@Composable
private fun ChildAlarmItem(
    cardData: CardData,
    onToggleEnabled: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onEditTime: (String, LocalTime) -> Unit
) {
    val context = LocalContext.current

    SwipeToDeleteContainer(
        onDelete = { onDelete(cardData.id) },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatTime(cardData.alarmTime),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (cardData.isEnabled) Color.White
                        else Color.White.copy(alpha = 0.38f),
                        modifier = Modifier.clickable {
                            onEditTime(cardData.id, cardData.alarmTime)
                        }
                    )
                    if (cardData.isEnabled) {
                        val nextDesc = getNextAlarmDescription(
                            context, cardData.alarmTime, cardData.selectedWeekdays
                        )
                        if (nextDesc.isNotEmpty()) {
                            Text(
                                text = nextDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E5FF).copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Switch(
                        checked = cardData.isEnabled,
                        onCheckedChange = { onToggleEnabled(cardData.id, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

// ───────── 曜日セレクター ─────────

@Composable
fun WeekdaySelector(
    selectedWeekdays: List<DayOfWeek>,
    onWeekdaysChanged: (List<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = remember { DayOfWeek.entries.toList() }
    val selected = remember(selectedWeekdays) {
        mutableStateListOf<DayOfWeek>().apply { addAll(selectedWeekdays) }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEach { day ->
            val isSelected = day in selected

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = if (isSelected) 6.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.5f)
                        else Color.Transparent
                    )
                    .background(
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF3A3A3A),
                        shape = CircleShape
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF555555),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable {
                        if (isSelected) selected.remove(day) else selected.add(day)
                        onWeekdaysChanged(selected.toList())
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center,
                    color = if (isSelected) Color.Black else Color(0xFFAAAAAA)
                )
            }
        }
    }
}

// ───────── ユーティリティ ─────────

private fun getNextAlarmDescription(
    context: Context,
    alarmTime: LocalTime,
    weekdays: List<DayOfWeek>
): String {
    val now = LocalTime.now()
    val today = LocalDate.now()

    val daysUntil: Int
    val dayLabel: String

    if (weekdays.isEmpty()) {
        if (alarmTime.isAfter(now)) {
            daysUntil = 0
            dayLabel = context.getString(R.string.today)
        } else {
            daysUntil = 1
            dayLabel = context.getString(R.string.tomorrow)
        }
    } else {
        var found = false
        var tempDays = 0
        var tempLabel = ""
        for (i in 0..7) {
            val checkDate = today.plusDays(i.toLong())
            if (weekdays.contains(checkDate.dayOfWeek)) {
                if (i == 0 && !alarmTime.isAfter(now)) continue
                tempDays = i
                tempLabel = when (i) {
                    0 -> context.getString(R.string.today)
                    1 -> context.getString(R.string.tomorrow)
                    else -> checkDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                found = true
                break
            }
        }
        if (!found) return ""
        daysUntil = tempDays
        dayLabel = tempLabel
    }

    val nowDateTime = java.time.LocalDateTime.of(today, now)
    val alarmDateTime = java.time.LocalDateTime.of(today.plusDays(daysUntil.toLong()), alarmTime)
    val duration = java.time.Duration.between(nowDateTime, alarmDateTime)

    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val remaining = when {
        hours > 0 && minutes > 0 -> context.getString(R.string.remaining_hours_minutes, hours, minutes)
        hours > 0 -> context.getString(R.string.remaining_hours, hours)
        minutes > 0 -> context.getString(R.string.remaining_minutes, minutes)
        else -> context.getString(R.string.remaining_soon)
    }

    return context.getString(R.string.next_alarm_description, dayLabel, remaining)
}

private fun formatTime(time: LocalTime): String {
    return String.format(Locale.getDefault(), "%02d:%02d", time.hour, time.minute)
}