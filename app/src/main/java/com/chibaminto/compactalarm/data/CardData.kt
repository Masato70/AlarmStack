package com.chibaminto.compactalarm.data

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Immutable
data class CardData(
    val id: String = UUID.randomUUID().toString(),
    val isParent: Boolean = true,
    val parentId: String? = null,
    val alarmTime: LocalTime,
    val isEnabled: Boolean = true,
    val selectedWeekdays: List<DayOfWeek> = emptyList(),
    val label: String = "",
    val isVibrationOnly: Boolean = false
) {
    companion object {
        fun createParent(
            alarmTime: LocalTime,
            selectedWeekdays: List<DayOfWeek> = emptyList(),
            label: String = ""
        ): CardData = CardData(
            isParent = true,
            parentId = null,
            alarmTime = alarmTime.withSecond(0).withNano(0),
            selectedWeekdays = selectedWeekdays,
            label = label
        )

        fun createChild(
            parentId: String,
            alarmTime: LocalTime,
            selectedWeekdays: List<DayOfWeek> = emptyList()
        ): CardData = CardData(
            isParent = false,
            parentId = parentId,
            alarmTime = alarmTime.withSecond(0).withNano(0),
            selectedWeekdays = selectedWeekdays
        )
    }
}