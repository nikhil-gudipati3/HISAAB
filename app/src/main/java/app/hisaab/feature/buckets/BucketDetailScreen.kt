package app.hisaab.feature.buckets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hisaab.core.design.*
import app.hisaab.core.money.formatPaise
import app.hisaab.feature.home.AppViewModel

@Composable
fun BucketDetailScreen(vm: AppViewModel, bucketId: Long, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val b = ui.bucketUis().firstOrNull { it.bucket.id == bucketId }
    var editing by remember { mutableStateOf(false) }
    if (b == null) { Text("Bucket not found", style = HType.Body, color = HColors.TextSecondary); return }
    val txns = ui.cycleTxns(ui.cycle).filter { it.bucketId == bucketId }
    LazyColumn(Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)) {
        item {
            Text("Back", style = HType.Label, color = HColors.Accent, modifier = Modifier.clickableBack { onBack() })
            Spacer(Modifier.height(16.dp))
            Text(b.bucket.name, style = HType.Headline, color = HColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Amount(b.remaining, HType.HeroAmount)
            Text("Spent ₹${formatPaise(b.spent)} of ₹${formatPaise(b.bucket.monthlyBudgetPaise + b.rolloverIn)}" +
                if (b.held > 0) " · ₹${formatPaise(b.held)} held in pending payments" else "",
                style = HType.Caption, color = HColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            TickMeter(b.remaining.coerceAtLeast(0), (b.bucket.monthlyBudgetPaise + b.rolloverIn).coerceAtLeast(1), overBudget = b.remaining < 0)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { SecondaryButton("Edit") { editing = true } }
                Box(Modifier.weight(1f)) { SecondaryButton("Archive") { vm.archiveBucket(bucketId); onBack() } }
            }
            Spacer(Modifier.height(28.dp))
            Text("This cycle", style = HType.Title, color = HColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
        }
        if (txns.isEmpty()) item { Text("No payments in this bucket this cycle.", style = HType.Body, color = HColors.TextSecondary) }
        items(txns) { t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(t.payeeName ?: t.payeeVpa ?: "Payment", style = HType.Label, color = HColors.TextPrimary)
                    Text("${fmtDate(ui.dateOf(t.timestamp))} · ${t.status.lowercase()}", style = HType.Caption, color = HColors.TextSecondary) }
                Text("−₹${formatPaise(t.amountPaise)}", style = HType.NumList, color = HColors.TextPrimary)
            }
        }
    }
    if (editing) BucketFormSheet(b.bucket, { editing = false }, { name, budget, roll, savings ->
        vm.updateBucket(b.bucket.copy(name = name, monthlyBudgetPaise = budget, rollover = roll, isSavings = savings)); editing = false
    })
}

private fun Modifier.clickableBack(onClick: () -> Unit) = this.clickable { onClick() }
