package app.hisaab.feature.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hisaab.core.design.*
import app.hisaab.core.money.formatPaise
import app.hisaab.core.money.parsePaise
import app.hisaab.data.*
import app.hisaab.feature.home.AppViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

const val DISCLAIMER = "Hisaab doesn't hold or move your money. Payments are completed in the UPI app you choose."

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = ui.settings
    var incomeText by remember(s.monthlyIncomePaise) { mutableStateOf(if (s.monthlyIncomePaise > 0) formatPaise(s.monthlyIncomePaise, detail = true) else "") }
    var savingsText by remember(s.savingsTargetPaise) { mutableStateOf(if (s.savingsTargetPaise > 0) formatPaise(s.savingsTargetPaise, detail = true) else "") }
    var showDelete by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun shareText(text: String, filename: String, mime: String) {
        val file = File(ctx.cacheDir, filename).apply { writeText(text) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        ctx.startActivity(Intent(Intent.ACTION_SEND).apply {
            type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Share export") })
    }
    fun exportCsv(): String {
        val sb = StringBuilder("date,payee,vpa,amount_rupees,category,bucket,status,source,note\n")
        ui.txns.forEach { t ->
            val b = ui.buckets.firstOrNull { it.id == t.bucketId }?.name ?: ""
            sb.append("${fmtDate(ui.dateOf(t.timestamp))},\"${t.payeeName ?: ""}\",\"${t.payeeVpa ?: ""}\",${formatPaise(t.amountPaise, detail = true)},${t.categoryKey ?: ""},$b,${t.status},${t.source},\"${t.note ?: ""}\"\n")
        }
        return sb.toString()
    }
    fun exportJson(): String {
        val arr = JSONArray()
        ui.txns.forEach { t ->
            arr.put(JSONObject().put("amountPaise", t.amountPaise).put("payeeName", t.payeeName ?: JSONObject.NULL)
                .put("payeeVpa", t.payeeVpa ?: JSONObject.NULL).put("note", t.note ?: JSONObject.NULL)
                .put("categoryKey", t.categoryKey ?: JSONObject.NULL).put("bucketId", t.bucketId ?: JSONObject.NULL)
                .put("timestamp", t.timestamp).put("status", t.status).put("source", t.source))
        }
        return JSONObject().put("transactions", arr).put("exportedFrom", "Hisaab").toString(2)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                val arr = JSONObject(text).getJSONArray("transactions")
                var count = 0
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    vm.addTxn(TxnEntity(amountPaise = o.getLong("amountPaise"),
                        payeeName = o.optString("payeeName").ifBlank { null },
                        payeeVpa = o.optString("payeeVpa").ifBlank { null },
                        note = o.optString("note").ifBlank { null },
                        categoryKey = o.optString("categoryKey").ifBlank { null },
                        bucketId = if (o.isNull("bucketId")) null else o.optLong("bucketId"),
                        accountId = null, timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        status = o.optString("status", "SUCCESS"), source = "MANUAL"))
                    count++
                }
                message = "Imported $count transactions."
            } catch (e: Exception) { message = "Import failed: that file doesn't look like a Hisaab backup." }
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)) {
        item {
            Text("Back", style = HType.Label, color = HColors.Accent, modifier = Modifier.clickableBack2 { onBack() })
            Spacer(Modifier.height(16.dp))
            Text("Settings", style = HType.Headline, color = HColors.TextPrimary)
            Spacer(Modifier.height(24.dp))
            Text("Budget style", style = HType.Title, color = HColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip2("Buckets", "Monthly buckets", s.mode == BudgetMode.BUCKETS, { vm.updateSettings { it.copy(mode = BudgetMode.BUCKETS) } })
                Chip2("Daily allowance", "Safe amount per day", s.mode == BudgetMode.DAILY, { vm.updateSettings { it.copy(mode = BudgetMode.DAILY) } })
            }
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(incomeText, { incomeText = it }, label = { Text("Monthly income (₹)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(s.paydayDay.toString(), { v -> v.toIntOrNull()?.let { d -> if (d in 1..31) vm.updateSettings { it.copy(paydayDay = d) } } },
                label = { Text("Payday day (1–31)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(savingsText, { savingsText = it }, label = { Text("Monthly savings target (₹)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip2("Sweep leftover", "Unspent goes to savings", s.leftoverRule == LeftoverRule.SWEEP, { vm.updateSettings { it.copy(leftoverRule = LeftoverRule.SWEEP) } })
                Chip2("Roll leftover", "Spread what's left evenly", s.leftoverRule == LeftoverRule.ROLL, { vm.updateSettings { it.copy(leftoverRule = LeftoverRule.ROLL) } })
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Save income and savings") {
                vm.updateSettings { it.copy(monthlyIncomePaise = parsePaise(incomeText) ?: it.monthlyIncomePaise,
                    savingsTargetPaise = parsePaise(savingsText) ?: 0L) }
                message = "Saved."
            }
            Spacer(Modifier.height(24.dp))
            ToggleRow("Strict mode", "Block payments past the remaining amount", s.strictMode) { v -> vm.updateSettings { it.copy(strictMode = v) } }
            ToggleRow("App lock", "Ask for biometrics or device credential on open", s.appLockEnabled) { v -> vm.updateSettings { it.copy(appLockEnabled = v) } }
            ToggleRow("Haptics", "Light vibration on selections and success", s.hapticsEnabled) { v -> vm.updateSettings { it.copy(hapticsEnabled = v) } }
            ToggleRow("Reduce motion", "Replace animations with instant changes", s.reduceMotion) { v -> vm.updateSettings { it.copy(reduceMotion = v) } }
            ToggleRow("Hide in recent apps", "Hide screenshots and recent-apps preview", s.hideInRecents) { v -> vm.updateSettings { it.copy(hideInRecents = v) } }
            Spacer(Modifier.height(24.dp))
            Text("Accounts", style = HType.Title, color = HColors.TextPrimary)
            ui.accounts.forEach { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(a.name + (a.last4?.let { " ••$it" } ?: ""), style = HType.Label, color = HColors.TextPrimary)
                    Text("Archive", style = HType.Label, color = HColors.TextSecondary, modifier = Modifier.clickableBack2 { vm.archiveAccount(a.id) })
                }
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Add account") { showAccount = true }
            Spacer(Modifier.height(24.dp))
            Text("Data", style = HType.Title, color = HColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Export CSV") { shareText(exportCsv(), "hisaab-export.csv", "text/csv") }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Export JSON backup") { shareText(exportJson(), "hisaab-backup.json", "application/json") }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Import JSON backup") { importLauncher.launch("application/json") }
            message?.let { Text(it, style = HType.Caption, color = HColors.TextSecondary) }
            Spacer(Modifier.height(24.dp))
            Text("About", style = HType.Title, color = HColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(DISCLAIMER, style = HType.Body, color = HColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("Hisaab 1.0.0 · All data stays on this device. No account, no login, no analytics, no ads. Camera is used only to scan QR codes; frames are never stored or sent.",
                style = HType.Body, color = HColors.TextSecondary)
            Spacer(Modifier.height(24.dp))
            SecondaryButton("Delete all data") { showDelete = true }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (showAccount) AccountSheet({ showAccount = false }) { name, last4 ->
        vm.addAccount(AccountEntity(name = name, last4 = last4.ifBlank { null }, sortOrder = ui.accounts.size)); showAccount = false
    }
    if (showDelete) DeleteConfirmSheet({ showDelete = false }) {
        vm.deleteAll(); showDelete = false; onBack()
    }
}

@Composable
fun ToggleRow(label: String, desc: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) { Text(label, style = HType.Label, color = HColors.TextPrimary); Text(desc, style = HType.Caption, color = HColors.TextSecondary) }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HColors.SurfaceHigh) {
        Column(Modifier.padding(20.dp)) {
            Text("Add account", style = HType.Headline, color = HColors.TextPrimary)
            Text("A label only. Hisaab never asks for account numbers, cards, or credentials.", style = HType.Body, color = HColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Bank label, for example HDFC Salary") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(last4, { last4 = it.take(4) }, label = { Text("Last 4 digits (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Add account") { if (name.isNotBlank()) onSave(name.trim(), last4) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmSheet(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HColors.SurfaceHigh) {
        Column(Modifier.padding(20.dp)) {
            Text("Delete all data", style = HType.Headline, color = HColors.TextPrimary)
            Text("This deletes every transaction and bucket on this device. Type DELETE to confirm.", style = HType.Body, color = HColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(typed, { typed = it }, label = { Text("Type DELETE") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Delete all data", enabled = typed == "DELETE") { onConfirm() }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun Modifier.clickableBack2(onClick: () -> Unit) = this.clickable { onClick() }
