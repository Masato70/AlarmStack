package com.chibaminto.compactalarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// DataStore拡張
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alarm_preferences")

@Singleton
class AlarmDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cardsKey = stringPreferencesKey("cards_data")
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ ->
            JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME))
        })
        .registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json, _, _ ->
            LocalTime.parse(json.asJsonPrimitive.asString, DateTimeFormatter.ISO_LOCAL_TIME)
        })
        .create()

    /**
     * カードリストをFlowとして取得
     */
    val cardsFlow: Flow<List<CardData>> = context.dataStore.data.map { preferences ->
        val jsonData = preferences[cardsKey] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<CardData>>() {}.type
            gson.fromJson(jsonData, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * カードリストを保存
     */
    suspend fun saveCards(cards: List<CardData>) {
        context.dataStore.edit { preferences ->
            preferences[cardsKey] = gson.toJson(cards)
        }
    }

    /**
     * 特定のカードの有効/無効を切り替え
     */
    suspend fun updateCardEnabled(cardId: String, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            val jsonData = preferences[cardsKey] ?: return@edit
            try {
                val type = object : TypeToken<List<CardData>>() {}.type
                val cards: List<CardData> = gson.fromJson(jsonData, type) ?: return@edit
                val updatedCards = cards.map { card ->
                    if (card.id == cardId) card.copy(isEnabled = isEnabled)
                    else card
                }
                preferences[cardsKey] = gson.toJson(updatedCards)
            } catch (e: Exception) {
                // エラー時は何もしない
            }
        }
    }
}
