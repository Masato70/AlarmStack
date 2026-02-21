package com.chibaminto.compactalarm.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibaminto.compactalarm.R
import com.chibaminto.compactalarm.utils.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CardData>>(emptyList())
    val cards: StateFlow<List<CardData>> = _cards

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var isInitialLoad = true

    // 元に戻す用
    private var lastDeletedCards: List<CardData>? = null
    private val _snackbarEvent = MutableSharedFlow<Int>()
    val snackbarEvent: SharedFlow<Int> = _snackbarEvent.asSharedFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            repository.cards.collect { cardsList ->
                _cards.value = cardsList
                if (isInitialLoad) {
                    scheduleEnabledAlarms(cardsList)
                    isInitialLoad = false
                }
            }
        }
    }

    private fun scheduleEnabledAlarms(cards: List<CardData>) {
        cards.filter { it.isEnabled }.forEach { card ->
            alarmScheduler.scheduleAlarm(card.id, card.alarmTime, card.selectedWeekdays)
        }
    }

    fun addParentCard(alarmTime: LocalTime, weekdays: List<DayOfWeek> = emptyList()) {
        viewModelScope.launch {
            val card = CardData.createParent(alarmTime, weekdays)
            _cards.value = repository.addCard(card, _cards.value)
            alarmScheduler.scheduleAlarm(card.id, card.alarmTime, card.selectedWeekdays)
        }
    }

    fun addChildCard(parentId: String, alarmTime: LocalTime) {
        viewModelScope.launch {
            val parent = _cards.value.find { it.id == parentId }
            val weekdays = parent?.selectedWeekdays ?: emptyList()
            val parentEnabled = parent?.isEnabled ?: true
            val isVibrationOnly = parent?.isVibrationOnly ?: false
            val card = CardData.createChild(parentId, alarmTime, weekdays)
                .copy(isEnabled = parentEnabled, isVibrationOnly = isVibrationOnly)
            _cards.value = repository.addCard(card, _cards.value)
            if (parentEnabled) {
                alarmScheduler.scheduleAlarm(card.id, card.alarmTime, card.selectedWeekdays)
            }
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch {
            val cardsToDelete = _cards.value.filter { it.id == cardId || it.parentId == cardId }
            cardsToDelete.forEach { card ->
                alarmScheduler.cancelAlarm(card.id)
            }
            _cards.value = repository.removeCard(cardId, _cards.value)
            lastDeletedCards = cardsToDelete
            _snackbarEvent.emit(R.string.alarm_deleted)
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            val deletedCards = lastDeletedCards ?: return@launch
            lastDeletedCards = null
            var currentCards = _cards.value
            deletedCards.forEach { card -> currentCards = currentCards + card }
            repository.saveCards(currentCards)
            _cards.value = currentCards
            deletedCards.filter { it.isEnabled }.forEach { card ->
                alarmScheduler.scheduleAlarm(card.id, card.alarmTime, card.selectedWeekdays)
            }
        }
    }

    fun toggleCardEnabled(cardId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val card = _cards.value.find { it.id == cardId } ?: return@launch

            if (isEnabled) {
                alarmScheduler.scheduleAlarm(card.id, card.alarmTime, card.selectedWeekdays)
            } else {
                alarmScheduler.cancelAlarm(card.id)
            }

            _cards.value = repository.toggleCardEnabled(cardId, isEnabled, _cards.value)

            if (card.isParent) {
                _cards.value.filter { it.parentId == cardId }.forEach { child ->
                    if (isEnabled) {
                        alarmScheduler.scheduleAlarm(child.id, child.alarmTime, child.selectedWeekdays)
                    } else {
                        alarmScheduler.cancelAlarm(child.id)
                    }
                }
            }
        }
    }

    fun updateWeekdays(cardId: String, weekdays: List<DayOfWeek>) {
        viewModelScope.launch {
            _cards.value = repository.updateWeekdays(cardId, weekdays, _cards.value)
            _cards.value
                .filter { it.id == cardId || it.parentId == cardId }
                .filter { it.isEnabled }
                .forEach { card ->
                    alarmScheduler.scheduleAlarm(card.id, card.alarmTime, weekdays)
                }
        }
    }

    fun updateAlarmTime(cardId: String, newTime: LocalTime) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(cardId)
            _cards.value = repository.updateAlarmTime(cardId, newTime, _cards.value)
            val updatedCard = _cards.value.find { it.id == cardId }
            if (updatedCard != null && updatedCard.isEnabled) {
                alarmScheduler.scheduleAlarm(
                    updatedCard.id, updatedCard.alarmTime, updatedCard.selectedWeekdays
                )
            }
        }
    }

    fun updateLabel(cardId: String, label: String) {
        viewModelScope.launch {
            _cards.value = repository.updateLabel(cardId, label, _cards.value)
        }
    }

    fun updateVibrationOnly(cardId: String, isVibrationOnly: Boolean) {
        viewModelScope.launch {
            _cards.value = repository.updateVibrationOnly(cardId, isVibrationOnly, _cards.value)
        }
    }

    fun getChildAlarms(parentId: String): List<CardData> {
        return _cards.value
            .filter { it.parentId == parentId }
            .sortedBy { it.alarmTime }
    }

    fun getParentCards(): List<CardData> {
        return _cards.value
            .filter { it.isParent }
            .sortedBy { it.alarmTime }
    }
}