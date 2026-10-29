package app.hisaab.core.time

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class Cycle(val start: LocalDate, val endExclusive: LocalDate) {
    val daysInCycle: Long get() = ChronoUnit.DAYS.between(start, endExclusive)
    fun dayIndex(date: LocalDate): Int = ChronoUnit.DAYS.between(start, date).toInt()
    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && date.isBefore(endExclusive)
}

fun effectiveDay(paydayDay: Int, month: YearMonth): Int =
    minOf(paydayDay.coerceIn(1, 31), month.lengthOfMonth())

/** Latest effective payday date <= today; end is the next one. */
fun currentCycle(paydayDay: Int, today: LocalDate): Cycle {
    fun eff(y: Int, m: Int): LocalDate {
        val ym = YearMonth.of(y, m)
        return LocalDate.of(y, m, effectiveDay(paydayDay, ym))
    }
    var y = today.year; var m = today.monthValue
    var start = eff(y, m)
    if (start.isAfter(today)) {
        if (m == 1) { y -= 1; m = 12 } else m -= 1
        start = eff(y, m)
    }
    var ny = start.year; var nm = start.monthValue
    if (nm == 12) { ny += 1; nm = 1 } else nm += 1
    return Cycle(start, eff(ny, nm))
}

fun previousCycle(paydayDay: Int, today: LocalDate): Cycle {
    val cur = currentCycle(paydayDay, today)
    return currentCycle(paydayDay, cur.start.minusDays(1))
}
