package com.chibaminto.compactalarm.data

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val dataStore: AlarmDataStore
) {
    val cards: Flow<List<CardData>> = dataStore.cardsFlow

    suspend fun saveCards(cards: List<CardData>) {
        dataStore.saveCards(cards)
    }

    suspend fun addCard(card: CardData, existingCards: List<CardData>): List<CardData> {
        val newCards = existingCards + card
        saveCards(newCards)
        return newCards
    }

    suspend fun removeCard(cardId: String, existingCards: List<CardData>): List<CardData> {
        val newCards = existingCards.filterNot { card ->
            card.id == cardId || card.parentId == cardId
        }
        saveCards(newCards)
        return newCards
    }

    suspend fun toggleCardEnabled(
        cardId: String,
        isEnabled: Boolean,
        existingCards: List<CardData>
    ): List<CardData> {
        val newCards = existingCards.map { card ->
            when {
                card.id == cardId -> card.copy(isEnabled = isEnabled)
                card.parentId == cardId -> card.copy(isEnabled = isEnabled)
                else -> card
            }
        }
        saveCards(newCards)
        return newCards
    }

    /**
     * 曜日選択を更新（親カードの場合、子カードも同時に更新）
     */
    suspend fun updateWeekdays(
        cardId: String,
        weekdays: List<DayOfWeek>,
        existingCards: List<CardData>
    ): List<CardData> {
        val newCards = existingCards.map { card ->
            if (card.id == cardId || card.parentId == cardId) {
                card.copy(selectedWeekdays = weekdays)
            } else {
                card
            }
        }
        saveCards(newCards)
        return newCards
    }

    /**
     * アラーム時刻を更新
     */
    suspend fun updateAlarmTime(
        cardId: String,
        newTime: LocalTime,
        existingCards: List<CardData>
    ): List<CardData> {
        val newCards = existingCards.map { card ->
            if (card.id == cardId) card.copy(alarmTime = newTime.withSecond(0).withNano(0))
            else card
        }
        saveCards(newCards)
        return newCards
    }

    /**
     * ラベルを更新
     */
    suspend fun updateLabel(
        cardId: String,
        label: String,
        existingCards: List<CardData>
    ): List<CardData> {
        val newCards = existingCards.map { card ->
            if (card.id == cardId) card.copy(label = label)
            else card
        }
        saveCards(newCards)
        return newCards
    }

    /**
     * バイブレーションのみモードを更新（親の場合、子カードも同時に更新）
     */
    suspend fun updateVibrationOnly(
        cardId: String,
        isVibrationOnly: Boolean,
        existingCards: List<CardData>
    ): List<CardData> {
        val newCards = existingCards.map { card ->
            if (card.id == cardId || card.parentId == cardId) {
                card.copy(isVibrationOnly = isVibrationOnly)
            } else {
                card
            }
        }
        saveCards(newCards)
        return newCards
    }
}