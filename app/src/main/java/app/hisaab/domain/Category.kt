package app.hisaab.domain

val CATEGORIES = listOf(
    "Groceries", "Food and drink", "Transport", "Fuel", "Shopping",
    "Bills", "Health", "Entertainment", "Education", "People", "Other"
)

private val MCC_MAP = mapOf(
    "5411" to "Groceries", "5422" to "Groceries", "5499" to "Groceries",
    "5812" to "Food and drink", "5814" to "Food and drink", "5811" to "Food and drink",
    "5541" to "Fuel", "5542" to "Fuel",
    "4111" to "Transport", "4121" to "Transport", "4131" to "Transport",
    "4900" to "Transport", "4789" to "Transport",
    "5311" to "Shopping", "5411" to "Groceries", "5651" to "Shopping", "5732" to "Shopping",
    "4900" to "Transport", "4814" to "Bills", "4812" to "Bills", "4899" to "Bills",
    "8011" to "Health", "8021" to "Health", "8043" to "Health",
    "7832" to "Entertainment", "7841" to "Entertainment", "7999" to "Entertainment",
    "8211" to "Education", "8220" to "Education", "8299" to "Education"
)

fun categoryForMcc(mcc: String?): String = MCC_MAP[mcc?.trim()] ?: "Other"

/** Payee memory: last bucket + category per VPA (lowercased key). */
class PayeeMemory {
    private val map = mutableMapOf<String, Pair<Long?, String?>>()
    fun remember(vpa: String, bucketId: Long?, categoryKey: String?) {
        map[vpa.lowercase()] = bucketId to categoryKey
    }
    fun lookup(vpa: String): Pair<Long?, String?>? = map[vpa.lowercase()]
}
