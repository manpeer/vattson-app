package ee.vattson.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ee.vattson.data.model.AppTheme
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.DatePreference
import ee.vattson.data.model.GraphType
import ee.vattson.data.model.PriceNotification
import ee.vattson.data.model.PriceResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency_preference")
        val SHOW_VAT = booleanPreferencesKey("show_vat")
        val PRICE_RESOLUTION = stringPreferencesKey("price_resolution")
        val DATE_PREF_V3 = stringPreferencesKey("date_preference_v3")
        val GRAPH_TYPE = stringPreferencesKey("graph_type")
        val APP_THEME = stringPreferencesKey("app_theme")
        val WIDGET_THEME = stringPreferencesKey("widget_theme")
        val CHEAPEST_WINDOW_DURATION = intPreferencesKey("cheapest_window_duration")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PRICE_NOTIFICATIONS = stringPreferencesKey("price_notifications")
        val LAST_NOTIFIED_TIMESTAMP = longPreferencesKey("last_notified_timestamp")
        val LAST_FETCH_TIMESTAMP = longPreferencesKey("last_fetch_timestamp")

        // Legacy keys for migration
        val SHOW_TODAY_LEGACY = booleanPreferencesKey("show_today")
        val SHOW_TOMORROW_LEGACY = booleanPreferencesKey("show_tomorrow")
    }

    private val gson = Gson()

    /**
     * Helper to handle Enum mapping and IO errors consistently.
     */
    private inline fun <reified T : Enum<T>> getEnumFlow(
        key: Preferences.Key<String>,
        defaultValue: T
    ): Flow<T> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val name = preferences[key] ?: return@map defaultValue
            try {
                enumValueOf<T>(name)
            } catch (_: IllegalArgumentException) {
                defaultValue
            }
        }

    val currencyPreferenceFlow: Flow<CurrencyPreference> =
        getEnumFlow(PreferencesKeys.CURRENCY, CurrencyPreference.SENT_KWH)

    val showVatFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.SHOW_VAT] ?: true }

    val priceResolutionFlow: Flow<PriceResolution> =
        getEnumFlow(PreferencesKeys.PRICE_RESOLUTION, PriceResolution.HOUR_1)

    val datePreferenceFlow: Flow<DatePreference> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val prefName = preferences[PreferencesKeys.DATE_PREF_V3]
            if (prefName != null) {
                try {
                    DatePreference.valueOf(prefName)
                } catch (_: Exception) {
                    DatePreference.TODAY
                }
            } else {
                // Migration logic using direct key access
                val showToday = preferences[PreferencesKeys.SHOW_TODAY_LEGACY] ?: true
                val showTomorrow = preferences[PreferencesKeys.SHOW_TOMORROW_LEGACY] ?: false
                when {
                    showToday && showTomorrow -> DatePreference.BOTH
                    showTomorrow -> DatePreference.TOMORROW
                    else -> DatePreference.TODAY
                }
            }
        }

    val graphTypeFlow: Flow<GraphType> =
        getEnumFlow(PreferencesKeys.GRAPH_TYPE, GraphType.BAR)

    val appThemeFlow: Flow<AppTheme> =
        getEnumFlow(PreferencesKeys.APP_THEME, AppTheme.SYSTEM)

    val widgetThemeFlow: Flow<AppTheme> =
        getEnumFlow(PreferencesKeys.WIDGET_THEME, AppTheme.SYSTEM)

    val cheapestWindowDurationFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.CHEAPEST_WINDOW_DURATION] ?: 4 }

    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }

    val priceNotificationsFlow: Flow<List<PriceNotification>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val json = preferences[PreferencesKeys.PRICE_NOTIFICATIONS] ?: return@map emptyList()
            try {
                val type = object : TypeToken<List<PriceNotification>>() {}.type
                val list: List<PriceNotification> = gson.fromJson(json, type)
                // Ensure no null units or ids from old data (Gson can bypass Kotlin null safety)
                list.map { 
                    @Suppress("SENSELESS_COMPARISON", "UNNECESSARY_SAFE_CALL")
                    if (it.unit == null) {
                        it.copy(unit = CurrencyPreference.SENT_KWH)
                    } else {
                        it
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    val lastNotifiedTimestampFlow: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.LAST_NOTIFIED_TIMESTAMP] ?: 0L }

    val lastFetchTimestampFlow: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.LAST_FETCH_TIMESTAMP] ?: 0L }

    // Update methods
    suspend fun updateCurrencyPreference(preference: CurrencyPreference) =
        edit { it[PreferencesKeys.CURRENCY] = preference.name }

    suspend fun updateShowVat(showVat: Boolean) =
        edit { it[PreferencesKeys.SHOW_VAT] = showVat }

    suspend fun updatePriceResolution(resolution: PriceResolution) =
        edit { it[PreferencesKeys.PRICE_RESOLUTION] = resolution.name }

    suspend fun updateDatePreference(preference: DatePreference) =
        edit { it[PreferencesKeys.DATE_PREF_V3] = preference.name }

    suspend fun updateGraphType(type: GraphType) =
        edit { it[PreferencesKeys.GRAPH_TYPE] = type.name }

    suspend fun updateAppTheme(theme: AppTheme) =
        edit { it[PreferencesKeys.APP_THEME] = theme.name }

    suspend fun updateWidgetTheme(theme: AppTheme) =
        edit { it[PreferencesKeys.WIDGET_THEME] = theme.name }

    suspend fun updateCheapestWindowDuration(duration: Int) =
        edit { it[PreferencesKeys.CHEAPEST_WINDOW_DURATION] = duration }

    suspend fun updateNotificationsEnabled(enabled: Boolean) =
        edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }

    suspend fun updatePriceNotifications(notifications: List<PriceNotification>) =
        edit { it[PreferencesKeys.PRICE_NOTIFICATIONS] = gson.toJson(notifications) }

    suspend fun updateLastNotifiedTimestamp(timestamp: Long) =
        edit { it[PreferencesKeys.LAST_NOTIFIED_TIMESTAMP] = timestamp }

    suspend fun updateLastFetchTimestamp(timestamp: Long) =
        edit { it[PreferencesKeys.LAST_FETCH_TIMESTAMP] = timestamp }

    private suspend fun edit(action: (MutablePreferences) -> Unit) {
        dataStore.edit(action)
    }
}
