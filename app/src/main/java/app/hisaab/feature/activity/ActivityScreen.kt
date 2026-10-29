package app.hisaab.feature.activity

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
import app.hisaab.core.money.parsePaise
import app.hisaab.data.TxnEntity
import app.hisaab.domain.CATEGORIES
import app.hisaab.feature.home.AppViewModel

@Composable
fun ActivityScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf<TxnEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    val filtered = ui.txns.filter { t ->
        (query.isBlank() || (t.payeeName ?: "").contains(query, true) || (t.note ?: "").contains(query, true) || (t.payeeVpa ?: "").contains(query, true)) &&
        (statusFilter == "All" || t.status == statusFilter)
    }
    val grouped = filtered.groupBy { ui.dateOf(it.timestamp) }
    LazyColumn(Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)) {
        item {
            Text("Activity", style = HType.Headline, color = HColors.TextPrimary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(query, { query = it }, label = { Text("Search payee or note") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "SUCCESS", "PENDING", "FAILED").forEach { s ->
                    Chip2(s, "", statusFilter == s, { statusFilter = s })
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Add manually") { adding = true }
            Spacer(Modifier.height(20.dp))
        }
        if (filtered.isEmpty()) item { Text("No payments match. Add one manually or pay with a QR.", style = HType.Body, color = HColors.TextSecondary) }
        grouped.forEach { (day, list) ->
            item { Text(fmtDate(day), style = HType.Title, color = HColors.TextPrimary); Spacer(Modifier.height(8.dp)) }
            items(list) { t ->
                val bName = ui.buckets.firstOrNull { it.id == t.bucketId }?.name
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selected = t },
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(t.payeeName ?: t.payeeVpa ?: "Payment", style = HType.Label, color = HColors.TextPrimary)
                        Text(listOfNotNull(bName, t.categoryKey, if (t.status == "PENDING") "Needs check" else null).joinToString(" · "),
                            style = HType.Caption, color = HColors.TextSecondary)
                    }
                    Text("−₹${formatPaise(t.amountPaise)}", style = HType.NumList, color = HColors.TextPrimary)
                }
            }
        }
    }
    selected?.let { t -> TxnDetailSheet(vm, t, { selected = null }) }
    if (adding) ManualAddSheet(vm, { adding = false })
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TxnDetailSheet(vm: AppViewModel, txn: TxnEntity, onDismiss: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf(txn.note ?: "") }
    var bucketId by remember { mutableStateOf(txn.bucketId) }
    var category by remember { mutableStateOf(txn.categoryKey ?: "Other") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HColors.SurfaceHigh) {
        Column(Modifier.padding(20.dp)) {
            Text(txn.payeeName ?: txn.payeeVpa ?: "Payment", style = HType.Headline, color = HColors.TextPrimary)
            Text("₹${formatPaise(txn.amountPaise, detail = true)} · ${txn.status.lowercase()} · ${txn.source.lowercase()}",
                style = HType.Caption, color = HColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("Bucket", style = HType.Label, color = HColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.buckets.forEach { b -> Chip2(b.name, "", bucketId == b.id, { bucketId = b.id }) }
            }
            Spacer(Modifier.height(12.dp))
            Text("Category", style = HType.Label, color = HColors.TextPrimary)
            // Simple category dropdown via chips (first row scroll omitted for brevity: show all wrapped in FlowRow)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { c -> Chip2(c, "", category == c, { category = c }) }
            }
            Spacer(Modifier.height(16.dp))
            if (txn.status == "PENDING") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { PrimaryButton("It went through") { vm.resolveTxn(txn.id, true); onDismiss() } }
                    Box(Modifier.weight(1f)) { SecondaryButton("It failed") { vm.resolveTxn(txn.id, false); onDismiss() } }
                }
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton("Save changes") {
                vm.updateTxn(txn.copy(note = note.ifBlank { null }, bucketId = bucketId, categoryKey = category)); onDismiss()
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Delete") { vm.deleteTxn(txn.id); onDismiss() }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var payee by remember { mutableStateOf("") }
    var amt by remember { mutableStateOf("") }
    var bucketId by remember { mutableStateOf<Long?>(ui.buckets.firstOrNull()?.id) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HColors.SurfaceHigh) {
        Column(Modifier.padding(20.dp)) {
            Text("Add manually", style = HType.Headline, color = HColors.TextPrimary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(payee, { payee = it }, label = { Text("Payee or note") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(amt, { amt = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("Bucket", style = HType.Label, color = HColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.buckets.forEach { b -> Chip2(b.name, "", bucketId == b.id, { bucketId = b.id }) }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Save payment") {
                val p = parsePaise(amt) ?: return@PrimaryButton
                if (p > 0) {
                    vm.addTxn(TxnEntity(amountPaise = p, payeeName = payee.ifBlank { null }, payeeVpa = null,
                        note = null, categoryKey = "Other", bucketId = bucketId, accountId = null,
                        timestamp = System.currentTimeMillis(), status = "SUCCESS", source = "MANUAL"))
                    onDismiss()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
