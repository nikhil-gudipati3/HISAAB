package app.hisaab.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ds by preferencesDataStore("hisaab_settings")

enum class BudgetMode { BUCKETS, DAILY }
enum class LeftoverRule { SWEEP, ROLL }

data class Settings(
    val mode: BudgetMode = BudgetMode.BUCKETS,
    val monthlyIncomePaise: Long = 0L,
    val paydayDay: Int = 1,
    val savingsTargetPaise: Long = 0L,
    val leftoverRule: LeftoverRule = LeftoverRule.SWEEP,
    val strictMode: Boolean = false,
    val appLockEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val hideInRecents: Boolean = false,
    val onboardingDone: Boolean = false
)

private object K {
    val MODE = stringPreferencesKey("mode")
    val INCOME = longPreferencesKey("income")
    val PAYDAY = intPreferencesKey("payday")
    val SAVINGS = longPreferencesKey("savings")
    val RULE = stringPreferencesKey("rule")
    val STRICT = booleanPreferencesKey("strict")
    val LOCK = booleanPreferencesKey("lock")
    val HAPTICS = booleanPreferencesKey("haptics")
    val MOTION = booleanPreferencesKey("motion")
    val HIDE = booleanPreferencesKey("hide")
    val ONBOARD = booleanPreferencesKey("onboard")
}

@Singleton
class SettingsRepo @Inject constructor(@ApplicationContext ctx: Context) {
    private val ds = ctx.ds
    val flow: Flow<Settings> = ds.data.map { p ->
        Settings(
            mode = runCatching { BudgetMode.valueOf(p[K.MODE] ?: "BUCKETS") }.getOrDefault(BudgetMode.BUCKETS),
            monthlyIncomePaise = p[K.INCOME] ?: 0L,
            paydayDay = p[K.PAYDAY] ?: 1,
            savingsTargetPaise = p[K.SAVINGS] ?: 0L,
            leftoverRule = runCatching { LeftoverRule.valueOf(p[K.RULE] ?: "SWEEP") }.getOrDefault(LeftoverRule.SWEEP),
            strictMode = p[K.STRICT] ?: false,
            appLockEnabled = p[K.LOCK] ?: false,
            hapticsEnabled = p[K.HAPTICS] ?: true,
            reduceMotion = p[K.MOTION] ?: false,
            hideInRecents = p[K.HIDE] ?: false,
            onboardingDone = p[K.ONBOARD] ?: false
        )
    }
    suspend fun update(transform: (Settings) -> Settings) {
        // Read current via flow first value is handled by caller using edit with snapshot below.
        ds.edit { p ->
            val cur = Settings(
                mode = runCatching { BudgetMode.valueOf(p[K.MODE] ?: "BUCKETS") }.getOrDefault(BudgetMode.BUCKETS),
                monthlyIncomePaise = p[K.INCOME] ?: 0L, paydayDay = p[K.PAYDAY] ?: 1,
                savingsTargetPaise = p[K.SAVINGS] ?: 0L,
                leftoverRule = runCatching { LeftoverRule.valueOf(p[K.RULE] ?: "SWEEP") }.getOrDefault(LeftoverRule.SWEEP),
                strictMode = p[K.STRICT] ?: false, appLockEnabled = p[K.LOCK] ?: false,
                hapticsEnabled = p[K.HAPTICS] ?: true, reduceMotion = p[K.MOTION] ?: false,
                hideInRecents = p[K.HIDE] ?: false, onboardingDone = p[K.ONBOARD] ?: false
            )
            val n = transform(cur)
            p[K.MODE] = n.mode.name; p[K.INCOME] = n.monthlyIncomePaise; p[K.PAYDAY] = n.paydayDay
            p[K.SAVINGS] = n.savingsTargetPaise; p[K.RULE] = n.leftoverRule.name
            p[K.STRICT] = n.strictMode; p[K.LOCK] = n.appLockEnabled; p[K.HAPTICS] = n.hapticsEnabled
            p[K.MOTION] = n.reduceMotion; p[K.HIDE] = n.hideInRecents; p[K.ONBOARD] = n.onboardingDone
        }
    }
    suspend fun setOnboardingDone() = update { it.copy(onboardingDone = true) }
}
