package app.hisaab.feature.buckets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hisaab.core.design.*
import app.hisaab.core.money.formatPaise
import app.hisaab.core.money.parsePaise
import app.hisaab.data.BucketEntity
import app.hisaab.feature.home.AppViewModel

@Composable
fun BucketsScreen(vm: AppViewModel, onDetail: (Long) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showNew by remember { mutableStateOf(false) }
    val buckets = ui.bucketUis()
    LazyColumn(Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)) {
        item {
            Text("Buckets", style = HType.Headline, color = HColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Income ₹${formatPaise(ui.settings.monthlyIncomePaise)} · buckets ₹${formatPaise(ui.totalBudget)}",
                style = HType.Caption, color = HColors.TextSecondary)
            if (ui.totalBudget > ui.settings.monthlyIncomePaise && ui.settings.monthlyIncomePaise > 0) {
                Text("Buckets add up to ₹${formatPaise(ui.totalBudget - ui.settings.monthlyIncomePaise)} more than your income.",
                    style = HType.Caption, color = HColors.TextSecondary)
            } else if (ui.unallocated > 0) {
                Text("₹${formatPaise(ui.unallocated)} not assigned to a bucket.", style = HType.Caption, color = HColors.TextSecondary)
            }
            Spacer(Modifier.height(20.dp))
        }
        items(buckets) { b ->
            GlowCard(false, { onDetail(b.bucket.id) }, Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(b.bucket.name, style = HType.Title, color = HColors.TextPrimary)
                    Text("₹${formatPaise(b.remaining)} left", style = HType.NumList, color = HColors.TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                TickMeter(b.remaining.coerceAtLeast(0), (b.bucket.monthlyBudgetPaise + b.rolloverIn).coerceAtLeast(1), overBudget = b.remaining < 0)
                Spacer(Modifier.height(8.dp))
                Text("Spent ₹${formatPaise(b.spent)} of ₹${formatPaise(b.bucket.monthlyBudgetPaise + b.rolloverIn)}" +
                    if (b.held > 0) " · ₹${formatPaise(b.held)} held" else "",
                    style = HType.Caption, color = HColors.TextSecondary)
                if (b.remaining < 0) Text("Over by ₹${formatPaise(-b.remaining)}", style = HType.Caption, color = HColors.TextSecondary)
            }
        }
        item { SecondaryButton("New bucket") { showNew = true } }
    }
    if (showNew) BucketFormSheet(null, { showNew = false }, { name, budget, roll, savings ->
        vm.addBucket(BucketEntity(name = name, monthlyBudgetPaise = budget, rollover = roll, isSavings = savings,
            sortOrder = ui.buckets.size)); showNew = false
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketFormSheet(existing: BucketEntity?, onDismiss: () -> Unit, onSave: (String, Long, Boolean, Boolean) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amt by remember { mutableStateOf(existing?.let { formatPaise(it.monthlyBudgetPaise, detail = true) } ?: "") }
    var roll by remember { mutableStateOf(existing?.rollover ?: false) }
    var savings by remember { mutableStateOf(existing?.isSavings ?: false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HColors.SurfaceHigh) {
        Column(Modifier.padding(20.dp)) {
            Text(if (existing == null) "New bucket" else "Edit bucket", style = HType.Headline, color = HColors.TextPrimary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(amt, { amt = it }, label = { Text("Monthly budget (₹)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row { Checkbox(roll, { roll = it }); Text("Unspent carries to next cycle", style = HType.Body, color = HColors.TextPrimary) }
            Row { Checkbox(savings, { savings = it }); Text("This is a savings bucket", style = HType.Body, color = HColors.TextPrimary) }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Save bucket") {
                val budget = parsePaise(amt) ?: return@PrimaryButton
                if (name.isNotBlank()) onSave(name.trim(), budget, roll, savings)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
