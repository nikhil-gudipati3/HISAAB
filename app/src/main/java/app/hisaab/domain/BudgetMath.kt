package app.hisaab.domain

import app.hisaab.core.time.Cycle
import java.time.LocalDate
import kotlin.math.max

/** Base daily shares: first `rem` days get 1 extra paise; sums exactly to spendable. */
fun baseFor(spendable: Long, days: Long, index: Long): Long {
    if (days <= 0) return 0L
    val base = spendable / days
    val rem = spendable % days
    return base + if (index < rem) 1 else 0L
}

data class DailyResult(
    val todayAllowance: Long, val spentToday: Long, val remainingToday: Long,
    val daysLeft: Long, val savingsPool: Long, val swept: Long, val debt: Long
)

/** SWEEP rule, exactly as spec 6.3. spentByDay: date -> SUCCESS+PENDING? No: spend counts SUCCESS only
 *  for allowance history, but held PENDING is counted separately on bucket screens. For daily,
 *  callers pass spent (SUCCESS) per day; PENDING today is shown via remainingToday adjustment in UI
 *  if needed. Here we follow spec: spentByDay is actual spend per day. */
fun dailySweep(
    spendable: Long, savingsTarget: Long, cycle: Cycle, today: LocalDate,
    spentByDay: Map<LocalDate, Long>
): DailyResult {
    val days = cycle.daysInCycle
    val todayIdx = cycle.dayIndex(today).coerceIn(0, (days - 1).toInt())
    var debt = 0L; var swept = 0L
    for (i in 0 until todayIdx) {
        val remaining = days - i
        val share = if (remaining > 0) debt / remaining else 0L
        val allowance = max(0L, baseFor(spendable, days, i.toLong()) - share)
        val spent = spentByDay[cycle.start.plusDays(i.toLong())] ?: 0L
        debt = debt - share + max(0L, spent - allowance)
        swept += max(0L, allowance - spent)
    }
    val todayAllowance = max(0L, baseFor(spendable, days, todayIdx.toLong()) - debt / max(1L, days - todayIdx))
    val spentToday = spentByDay[today] ?: 0L
    return DailyResult(todayAllowance, spentToday, max(0L, todayAllowance - spentToday),
        days - todayIdx, savingsTarget + swept, swept, debt)
}

/** ROLL rule, exactly as spec 6.3. */
fun dailyRoll(
    spendable: Long, savingsTarget: Long, cycle: Cycle, today: LocalDate,
    spentBeforeToday: Long, spentToday: Long
): DailyResult {
    val days = cycle.daysInCycle
    val todayIdx = cycle.dayIndex(today).coerceIn(0, (days - 1).toInt())
    val remainingDays = max(1L, days - todayIdx)
    val todayAllowance = max(0L, (spendable - spentBeforeToday) / remainingDays)
    return DailyResult(todayAllowance, spentToday, max(0L, todayAllowance - spentToday),
        remainingDays, savingsTarget, 0L, 0L)
}

data class BucketSummary(
    val budget: Long, val spent: Long, val held: Long,
    val rolloverIn: Long, val remaining: Long
)

fun bucketSummary(budget: Long, spent: Long, held: Long, rolloverIn: Long): BucketSummary =
    BucketSummary(budget, spent, held, rolloverIn, budget + rolloverIn - spent - held)

/** Previous cycle positive remaining, only if rollover enabled. */
fun rolloverIn(prevBudget: Long, prevSpent: Long, prevHeld: Long, rollover: Boolean): Long =
    if (!rollover) 0L else max(0L, prevBudget - prevSpent - prevHeld)

/** Linear pace projection: money left on payday if spend continues at current daily rate. */
fun paceLeftOnPayday(spendable: Long, spentSoFar: Long, elapsedDays: Long, totalDays: Long): Long {
    if (elapsedDays <= 0) return spendable - spentSoFar
    val projected = spentSoFar * totalDays / elapsedDays
    return spendable - projected
}

/** Consecutive days (ending today, going backwards) at or under allowance. */
fun streakDays(dailyAllowances: List<Long>, dailySpent: List<Long>): Int {
    var streak = 0
    for (i in dailyAllowances.indices.reversed()) {
        if (dailySpent[i] <= dailyAllowances[i]) streak++ else break
    }
    return streak
}
