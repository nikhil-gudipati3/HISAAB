package app.hisaab

import app.hisaab.core.money.*
import app.hisaab.core.time.*
import app.hisaab.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun indianGrouping() {
        assertEquals("12,34,567.89", formatIndian(BigDecimal("1234567.89")))
        assertEquals("1,00,000.00", formatIndian(BigDecimal("100000")))
        assertEquals("999.00", formatIndian(BigDecimal("999")))
        assertEquals("1,000.00", formatIndian(BigDecimal("1000")))
        assertEquals("1,00,00,000.00", formatIndian(BigDecimal("10000000")))
    }
    @Test fun formatPaiseDisplayRules() {
        assertEquals("3,333", formatPaise(333300))
        assertEquals("120.50", formatPaise(12050)) // <1000 with paise shows paise
        assertEquals("1,200", formatPaise(120050)) // >=1000 hides paise by default
        assertEquals("1,200.50", formatPaise(120050, detail = true))
        assertEquals("0", formatPaise(0))
    }
    @Test fun parsePaiseRejects() {
        assertEquals(12000L, parsePaise("120"))
        assertEquals(12050L, parsePaise("120.50"))
        assertEquals(10000000L, parsePaise("1,00,000"))
        assertNull(parsePaise("120.555"))
        assertNull(parsePaise("abc"))
        assertNull(parsePaise(""))
    }
}

class CycleTest {
    @Test fun payday31In30DayMonth() {
        // Payday 31, today 15 Apr 2026 (April has 30 days): cycle started 31 Mar, ends 30 Apr
        val c = currentCycle(31, LocalDate.of(2026, 4, 15))
        assertEquals(LocalDate.of(2026, 3, 31), c.start)
        assertEquals(LocalDate.of(2026, 4, 30), c.endExclusive)
        assertEquals(30L, c.daysInCycle)
    }
    @Test fun payday31InFebruary() {
        val c = currentCycle(31, LocalDate.of(2026, 2, 15))
        assertEquals(LocalDate.of(2026, 1, 31), c.start)
        assertEquals(LocalDate.of(2026, 2, 28), c.endExclusive)
        assertEquals(28L, c.daysInCycle)
    }
    @Test fun payday31AfterFebEnd() {
        val c = currentCycle(31, LocalDate.of(2026, 3, 5))
        assertEquals(LocalDate.of(2026, 2, 28), c.start)
        assertEquals(LocalDate.of(2026, 3, 31), c.endExclusive)
    }
    @Test fun payday1Normal() {
        val c = currentCycle(1, LocalDate.of(2026, 10, 14))
        assertEquals(LocalDate.of(2026, 10, 1), c.start)
        assertEquals(LocalDate.of(2026, 11, 1), c.endExclusive)
        assertEquals(31L, c.daysInCycle)
    }
}

class DailyTest {
    @Test fun oneLakhOver30DaysSumsExactly() {
        val spendable = 10_000_000L; val days = 30L
        var sum = 0L
        for (i in 0 until days) sum += baseFor(spendable, days, i)
        assertEquals(10_000_000L, sum)
        // First 10 days get the extra paise (10000000 % 30 = 10)
        assertEquals(333334L, baseFor(spendable, days, 0))
        assertEquals(333333L, baseFor(spendable, days, 10))
    }
    @Test fun sweepAndRoll10DaySampleWithOverspend() {
        // 10-day cycle starting 1 Oct, today = day 5 (index 4). Spendable 100000 paise = 10000/day.
        val cycle = Cycle(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11))
        val today = LocalDate.of(2026, 10, 5)
        val spentByDay = mapOf(
            LocalDate.of(2026, 10, 1) to 8000L,   // underspend 2000 swept
            LocalDate.of(2026, 10, 2) to 12000L,  // overspend 2000 -> debt
            LocalDate.of(2026, 10, 3) to 10000L,
            LocalDate.of(2026, 10, 4) to 10000L
        )
        val sweep = dailySweep(100000L, 0L, cycle, today, spentByDay)
        assertTrue(sweep.swept >= 0)
        assertTrue(sweep.todayAllowance > 0)
        // ROLL: (100000 - 40000) / 6 remaining days = 10000
        val roll = dailyRoll(100000L, 0L, cycle, today, 40000L, 0L)
        assertEquals(10000L, roll.todayAllowance)
        // ROLL overspend case: spent 70000 before today -> (100000-70000)/6 = 5000
        val roll2 = dailyRoll(100000L, 0L, cycle, today, 70000L, 0L)
        assertEquals(5000L, roll2.todayAllowance)
        // Sweep with no spend: swept = sum of first 4 allowances
        val sweepClean = dailySweep(100000L, 0L, cycle, today, emptyMap())
        assertEquals(40000L, sweepClean.swept)
        assertEquals(10000L, sweepClean.todayAllowance)
    }
    @Test fun bucketMathRolloverAndHeld() {
        val s = bucketSummary(50000L, 30000L, 5000L, 4000L)
        assertEquals(19000L, s.remaining) // 50000+4000-30000-5000
        assertEquals(0L, rolloverIn(50000L, 30000L, 0L, false))
        assertEquals(20000L, rolloverIn(50000L, 30000L, 0L, true))
        assertEquals(0L, rolloverIn(30000L, 40000L, 0L, true)) // negative clamped
    }
    @Test fun paceAndStreak() {
        assertEquals(50000L, paceLeftOnPayday(100000L, 25000L, 15L, 30L))
        // Streak counts backwards from the last day: overspend on the last day -> 0
        assertEquals(0, streakDays(listOf(10000L, 10000L, 10000L), listOf(9000L, 8000L, 11000L)))
        assertEquals(2, streakDays(listOf(10000L, 10000L, 10000L), listOf(11000L, 9000L, 8000L)))
    }
}

class UpiTest {
    @Test fun valid() {
        val p = parseUpi("upi://pay?pa=chaipoint@okhdfcbank&pn=Chai%20Point&am=120.00&cu=INR&tn=Tea&mc=5812&tr=ABC123")
        assertNotNull(p); assertEquals("chaipoint@okhdfcbank", p!!.vpa); assertEquals("Chai Point", p.name)
        assertEquals(12000L, p.amountPaise); assertEquals("5812", p.mcc); assertEquals("ABC123", p.ref)
    }
    @Test fun missingAmount() {
        val p = parseUpi("upi://pay?pa=shop@okaxis&pn=Shop&cu=INR")
        assertNotNull(p); assertNull(p!!.amountPaise)
    }
    @Test fun threeDecimalsRejected() {
        val p = parseUpi("upi://pay?pa=shop@okaxis&am=120.555&cu=INR")
        assertNotNull(p); assertNull(p!!.amountPaise)
    }
    @Test fun nonInrRejected() {
        assertNull(parseUpi("upi://pay?pa=shop@okaxis&am=10&cu=USD"))
    }
    @Test fun encodedChars() {
        val p = parseUpi("upi://pay?pa=caf%C3%A9@okhdfcbank&pn=Caf%C3%A9%20Bar&am=50.25&cu=INR")
        // VPA with non-ASCII fails regex -> null (correct: invalid VPA rejected)
        assertNull(p)
        val p2 = parseUpi("upi://pay?pa=cafe@okhdfcbank&pn=Caf%C3%A9%20Bar&am=50.25&cu=INR")
        assertEquals("Café Bar", p2!!.name)
    }
    @Test fun garbageAndNonUpi() {
        assertNull(parseUpi("hello world"))
        assertNull(parseUpi("https://example.com/pay?pa=a@okhdfcbank"))
        assertNull(parseUpi("upi://pay?pa=notavpa&cu=INR"))
        assertNull(parseUpi("upi://pay?cu=INR"))
        assertNull(parseUpi(""))
    }
    @Test fun buildUriRoundTrip() {
        val uri = buildUpiUri("chaipoint@okhdfcbank", "Chai Point", 12000L, "Tea")
        val p = parseUpi(uri)
        assertEquals("chaipoint@okhdfcbank", p!!.vpa); assertEquals(12000L, p.amountPaise)
    }
    @Test fun resultInterpretation() {
        assertEquals(true, interpretUpiResult(mapOf("Status" to "SUCCESS")))
        assertEquals(false, interpretUpiResult(mapOf("status" to "FAILURE")))
        assertNull(interpretUpiResult(mapOf("status" to "SUBMITTED")))
        assertNull(interpretUpiResult(emptyMap()))
    }
}

class CategoryTest {
    @Test fun mccMap() {
        assertEquals("Groceries", categoryForMcc("5411"))
        assertEquals("Food and drink", categoryForMcc("5812"))
        assertEquals("Food and drink", categoryForMcc("5814"))
        assertEquals("Fuel", categoryForMcc("5541"))
        assertEquals("Transport", categoryForMcc("4111"))
        assertEquals("Other", categoryForMcc("9999"))
        assertEquals("Other", categoryForMcc(null))
    }
    @Test fun payeeMemory() {
        val m = PayeeMemory()
        m.remember("Shop@OkAxis", 7L, "Groceries")
        assertEquals(7L to "Groceries", m.lookup("shop@okaxis"))
        assertNull(m.lookup("other@okaxis"))
        m.remember("shop@okaxis", 9L, "Shopping")
        assertEquals(9L to "Shopping", m.lookup("SHOP@OKAXIS"))
    }
}
