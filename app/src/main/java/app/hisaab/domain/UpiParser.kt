package app.hisaab.domain

import app.hisaab.core.money.parseUpiAmount
import java.net.URLDecoder

data class UpiPayee(
    val vpa: String, val name: String?, val amountPaise: Long?,
    val note: String?, val ref: String?, val mcc: String?
)

private val VPA = Regex("^[A-Za-z0-9.\\-_]{2,256}@[A-Za-z][A-Za-z0-9]{1,64}$")

private fun queryParams(raw: String): Map<String, String> {
    val q = raw.substringAfter('?', "")
    if (q.isEmpty()) return emptyMap()
    val out = LinkedHashMap<String, String>()
    for (part in q.split('&')) {
        val idx = part.indexOf('=')
        if (idx < 0) continue
        val k = URLDecoder.decode(part.substring(0, idx), "UTF-8")
        val v = URLDecoder.decode(part.substring(idx + 1), "UTF-8")
        out.putIfAbsent(k, v)
    }
    return out
}

/** Pure-JVM UPI parser (no android.net.Uri) so unit tests run on the JVM. Behaviour matches spec 8.2. */
fun parseUpi(rawInput: String): UpiPayee? {
    val raw = rawInput.trim()
    val schemeSplit = raw.indexOf("://")
    if (schemeSplit < 0) return null
    val scheme = raw.substring(0, schemeSplit)
    if (!scheme.equals("upi", ignoreCase = true)) return null
    val rest = raw.substring(schemeSplit + 3)
    val host = rest.substringBefore('?').substringBefore('/').substringBefore('#')
    if (!host.equals("pay", ignoreCase = true)) return null
    val params = try { queryParams(raw) } catch (e: Exception) { return null }
    val pa = params["pa"]?.takeIf { VPA.matches(it) } ?: return null
    params["cu"]?.let { if (!it.equals("INR", ignoreCase = true)) return null }
    val am = params["am"]?.let { parseUpiAmount(it) }
    // am present but invalid (0, negative, >2 decimals, non-numeric): spec sample returns null amount
    // only via takeIf>0 in parseUpiAmount; a malformed am string must reject the amount, not the payee.
    // However an explicit am that fails parsing means: amount unknown -> null amount is correct,
    // EXCEPT 3-decimal case must not silently become a valid amount (it won't: parse returns null).
    val name = params["pn"]?.trim()?.ifBlank { null }
    return UpiPayee(pa, name, am, params["tn"], params["tr"], params["mc"])
}

/** Build a upi://pay URI string with proper encoding (never string concatenation of raw values). */
fun buildUpiUri(vpa: String, name: String?, amountPaise: Long, note: String?): String {
    fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    val amt = java.math.BigDecimal(amountPaise).movePointLeft(2).setScale(2).toPlainString()
    val sb = StringBuilder("upi://pay?pa=").append(enc(vpa))
    if (!name.isNullOrBlank()) sb.append("&pn=").append(enc(name))
    sb.append("&am=").append(amt).append("&cu=INR")
    if (!note.isNullOrBlank()) sb.append("&tn=").append(enc(note))
    return sb.toString()
}

/** Read a UPI result case-insensitively. SUCCESS->true, FAILURE/FAILED->false, else null (ask user). */
fun interpretUpiResult(extras: Map<String, String?>): Boolean? {
    val flat = extras.entries.associate { it.key.lowercase() to (it.value ?: "") }
    val status = flat["status"] ?: flat["response"] ?: ""
    return when {
        status.contains("success", ignoreCase = true) -> true
        status.contains("fail", ignoreCase = true) -> false
        else -> null
    }
}
