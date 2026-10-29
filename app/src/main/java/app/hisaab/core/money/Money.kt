package app.hisaab.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/** All money is Long paise. Never Float/Double. */
/** Format rupees (BigDecimal value in rupees) with Indian grouping: 1234567.89 -> 12,34,567.89 */
fun formatIndian(rupees: BigDecimal): String {
    val scaled = rupees.setScale(2, RoundingMode.HALF_UP)
    val negative = scaled.signum() < 0
    val abs = scaled.abs()
    val intPart = abs.toBigInteger().toString()
    val frac = abs.remainder(BigDecimal.ONE).movePointRight(2).toInt().toString().padStart(2, '0')
    val grouped = groupIndian(intPart)
    return (if (negative) "-" else "") + grouped + "." + frac
}

fun groupIndian(intPart: String): String {
    if (intPart.length <= 3) return intPart
    val last3 = intPart.takeLast(3)
    var rest = intPart.dropLast(3)
    val parts = mutableListOf<String>()
    while (rest.length > 2) { parts.add(0, rest.takeLast(2)); rest = rest.dropLast(2) }
    if (rest.isNotEmpty()) parts.add(0, rest)
    parts.add(last3)
    return parts.joinToString(",")
}

/** Display: whole rupees by default; paise only when non-zero AND amount < Rs 1000, or detail=true. */
fun formatPaise(paise: Long, detail: Boolean = false): String {
    val abs = kotlin.math.abs(paise)
    val sign = if (paise < 0) "-" else ""
    val rupees = abs / 100
    val rem = abs % 100
    return if (detail || (rem != 0L && abs < 100_000L)) {
        sign + groupIndian(rupees.toString()) + "." + rem.toString().padStart(2, '0')
    } else sign + groupIndian(rupees.toString())
}

/** Digits only (no rupee sign, no sign) for the serif part of Amount(). */
fun digitsPaise(paise: Long, detail: Boolean = false): String = formatPaise(paise, detail).removePrefix("-")

/** Parse user input like "1,00,000.50" or "120" to paise. Null if invalid or >2 decimals. */
fun parsePaise(input: String): Long? {
    val clean = input.trim().replace(",", "").replace("₹", "").trim()
    if (clean.isEmpty()) return null
    return try {
        val bd = BigDecimal(clean)
        if (bd.scale() > 2) return null
        // Reject values that lose precision / overflow
        bd.movePointRight(2).longValueExact()
    } catch (e: Exception) { null }
}

/** Parse a UPI amount string strictly: >2 decimals rejected (longValueExact throws). */
fun parseUpiAmount(raw: String): Long? = try {
    BigDecimal(raw.trim()).movePointRight(2).longValueExact().takeIf { it > 0 }
} catch (e: Exception) { null }
