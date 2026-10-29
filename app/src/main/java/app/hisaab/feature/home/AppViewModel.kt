package app.hisaab.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hisaab.core.time.Cycle
import app.hisaab.core.time.currentCycle
import app.hisaab.core.time.previousCycle
import app.hisaab.data.*
import app.hisaab.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class BucketUi(
    val bucket: BucketEntity, val spent: Long, val held: Long,
    val rolloverIn: Long, val remaining: Long
)

data class AppUi(
    val settings: Settings = Settings(),
    val accounts: List<AccountEntity> = emptyList(),
    val buckets: List<BucketEntity> = emptyList(),
    val txns: List<TxnEntity> = emptyList(),
    val cycle: Cycle = currentCycle(1, LocalDate.now()),
    val prevCycle: Cycle = previousCycle(1, LocalDate.now()),
    val today: LocalDate = LocalDate.now()
) {
    fun dateOf(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
    fun cycleTxns(c: Cycle): List<TxnEntity> = txns.filter { c.contains(dateOf(it.timestamp)) }
    fun bucketUis(c: Cycle = cycle, prev: Cycle = prevCycle): List<BucketUi> {
        val cur = cycleTxns(c); val prv = cycleTxns(prev)
        return buckets.map { b ->
            val spent = cur.filter { it.bucketId == b.id && it.status == "SUCCESS" }.sumOf { it.amountPaise }
            val held = cur.filter { it.bucketId == b.id && it.status == "PENDING" }.sumOf { it.amountPaise }
            val pSpent = prv.filter { it.bucketId == b.id && it.status == "SUCCESS" }.sumOf { it.amountPaise }
            val pHeld = prv.filter { it.bucketId == b.id && it.status == "PENDING" }.sumOf { it.amountPaise }
            val roll = rolloverIn(b.monthlyBudgetPaise, pSpent, pHeld, b.rollover)
            BucketUi(b, spent, held, roll, b.monthlyBudgetPaise + roll - spent - held)
        }
    }
    val totalBudget: Long get() = buckets.sumOf { it.monthlyBudgetPaise }
    val totalRemaining: Long get() = bucketUis().sumOf { it.remaining }
    val totalSpentCycle: Long get() = cycleTxns(cycle).filter { it.status == "SUCCESS" }.sumOf { it.amountPaise }
    val pending: List<TxnEntity> get() = txns.filter { it.status == "PENDING" }
    fun daily(): DailyResult {
        val spendable = (settings.monthlyIncomePaise - settings.savingsTargetPaise).coerceAtLeast(0)
        val byDay = cycleTxns(cycle).filter { it.status == "SUCCESS" }
            .groupBy { dateOf(it.timestamp) }.mapValues { e -> e.value.sumOf { it.amountPaise } }
        val beforeToday = byDay.filterKeys { it.isBefore(today) }.values.sum()
        return if (settings.leftoverRule == LeftoverRule.SWEEP) dailySweep(spendable, settings.savingsTargetPaise, cycle, today, byDay)
        else dailyRoll(spendable, settings.savingsTargetPaise, cycle, today, beforeToday, byDay[today] ?: 0L)
    }
    val unallocated: Long get() = settings.monthlyIncomePaise - totalBudget
}

@HiltViewModel
class AppViewModel @Inject constructor(
    val repo: HisaabRepo, val settingsRepo: SettingsRepo
) : ViewModel() {
    val ui: StateFlow<AppUi> = combine(
        settingsRepo.flow, repo.accounts(), repo.buckets(), repo.txns()
    ) { s, a, b, t ->
        val today = LocalDate.now()
        AppUi(s, a, b, t, currentCycle(s.paydayDay, today), previousCycle(s.paydayDay, today), today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppUi())

    fun updateSettings(f: (Settings) -> Settings) = viewModelScope.launch { settingsRepo.update(f) }
    fun addTxn(t: TxnEntity) = viewModelScope.launch { repo.addTxn(t) }
    suspend fun addTxnNow(t: TxnEntity): Long = repo.addTxn(t)
    fun updateTxn(t: TxnEntity) = viewModelScope.launch { repo.updateTxn(t) }
    fun deleteTxn(id: Long) = viewModelScope.launch { repo.deleteTxn(id) }
    fun resolveTxn(id: Long, success: Boolean) = viewModelScope.launch {
        repo.txnById(id)?.let { repo.updateTxn(it.copy(status = if (success) "SUCCESS" else "FAILED")) }
    }
    fun addBucket(b: BucketEntity) = viewModelScope.launch { repo.addBucket(b) }
    fun updateBucket(b: BucketEntity) = viewModelScope.launch { repo.updateBucket(b) }
    fun archiveBucket(id: Long) = viewModelScope.launch { repo.archiveBucket(id) }
    fun addAccount(a: AccountEntity) = viewModelScope.launch { repo.addAccount(a) }
    fun updateAccount(a: AccountEntity) = viewModelScope.launch { repo.updateAccount(a) }
    fun archiveAccount(id: Long) = viewModelScope.launch { repo.archiveAccount(id) }
    fun deleteAll() = viewModelScope.launch {
        repo.deleteAllTxns(); repo.deleteAllBuckets()
        // Accounts are labels; delete-all in spec covers data: archive accounts too via direct db access
        repo.accountsOnce().forEach { repo.archiveAccount(it.id) }
    }
    /** Payee memory from Room: last SUCCESS txn for this VPA. */
    suspend fun memoryFor(vpa: String) = repo.lastSuccessForPayee(vpa)
}
