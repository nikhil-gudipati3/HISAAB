package app.hisaab.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.hisaab.core.design.*
import app.hisaab.data.BudgetMode

@Composable
fun HomeScreen(vm: AppViewModel, onSettings: () -> Unit, onBucket: (Long) -> Unit, onPay: () -> Unit) {
    val ui = vm.ui.collectAsStateWithLifecycle()
    val s = ui.value
    val cycleLabel = "${fmtDate(s.cycle.start)} – ${fmtDate(s.cycle.endExclusive.minusDays(1))} · day ${s.cycle.dayIndex(s.today) + 1} of ${s.cycle.daysInCycle}"
    LazyColumn(modifier = Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 110.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cycleLabel, style = HType.Caption, color = HColors.TextSecondary)
                Text("Settings", style = HType.Label, color = HColors.Accent, modifier = Modifier.clickable0 { onSettings() })
            }
            Spacer(Modifier.height(32.dp))
        }
        if (s.settings.mode == BudgetMode.DAILY) {
            val d = s.daily()
            item {
                Text("Safe to spend today", style = HType.Headline, color = HColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Amount(d.todayAllowance, HType.HeroAmount)
                Spacer(Modifier.height(12.dp))
                TickMeter(d.remainingToday, d.todayAllowance.coerceAtLeast(1))
                Spacer(Modifier.height(8.dp))
                Text("₹${app.hisaab.core.money.formatPaise(d.remainingToday)} left of ₹${app.hisaab.core.money.formatPaise(d.todayAllowance)} · resets at midnight",
                    style = HType.Caption, color = HColors.TextSecondary)
                Spacer(Modifier.height(32.dp))
                Text("Savings", style = HType.Title, color = HColors.TextPrimary)
                GlowCard(false, null) {
                    Text("Savings pool", style = HType.Label, color = HColors.TextSecondary)
                    Amount(d.savingsPool, HType.DisplayAmount)
                    Text("${d.daysLeft} days left in this cycle", style = HType.Caption, color = HColors.TextSecondary)
                }
                Spacer(Modifier.height(32.dp))
            }
        } else {
            val buckets = s.bucketUis()
            val total = s.totalBudget; val rem = s.totalRemaining
            item {
                Text("Left this cycle", style = HType.Headline, color = HColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Amount(rem, HType.HeroAmount)
                Spacer(Modifier.height(12.dp))
                TickMeter(rem.coerceAtLeast(0), total.coerceAtLeast(1), overBudget = rem < 0)
                Spacer(Modifier.height(8.dp))
                Text("₹${app.hisaab.core.money.formatPaise(rem.coerceAtLeast(0))} left of ₹${app.hisaab.core.money.formatPaise(total)}",
                    style = HType.Caption, color = HColors.TextSecondary)
                if (s.unallocated != 0L) {
                    Spacer(Modifier.height(8.dp))
                    Text(if (s.unallocated > 0) "₹${app.hisaab.core.money.formatPaise(s.unallocated)} not assigned to a bucket"
                        else "Buckets add up to ₹${app.hisaab.core.money.formatPaise(-s.unallocated)} more than your income.",
                        style = HType.Caption, color = HColors.TextSecondary)
                }
                Spacer(Modifier.height(32.dp))
                Text("Buckets", style = HType.Title, color = HColors.TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    buckets.forEach { b ->
                        GlowCard(false, { onBucket(b.bucket.id) }, Modifier.width(180.dp)) {
                            Text(b.bucket.name, style = HType.Label, color = HColors.TextPrimary)
                            Amount(b.remaining, HType.DisplayAmount)
                            TickMeter(b.remaining.coerceAtLeast(0), (b.bucket.monthlyBudgetPaise + b.rolloverIn).coerceAtLeast(1), ticks = 20, overBudget = b.remaining < 0)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
        item { Text("Recent", style = HType.Title, color = HColors.TextPrimary); Spacer(Modifier.height(12.dp)) }
        if (s.txns.isEmpty()) {
            item { Text("No payments yet. Scan a QR to log your first one.", style = HType.Body, color = HColors.TextSecondary) }
        } else {
            items(s.txns.take(5)) { t ->
                val bName = s.buckets.firstOrNull { it.id == t.bucketId }?.name
                val aName = s.accounts.firstOrNull { it.id == t.accountId }?.name
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(t.payeeName ?: t.payeeVpa ?: "Payment", style = HType.Label, color = HColors.TextPrimary)
                        Text(listOfNotNull(bName, aName).joinToString(" · ").ifBlank { fmtTime(t.timestamp) },
                            style = HType.Caption, color = HColors.TextSecondary)
                    }
                    Text("−₹${app.hisaab.core.money.formatPaise(t.amountPaise)}", style = HType.NumList, color = HColors.TextPrimary)
                }
            }
        }
    }
}

private fun Modifier.clickable0(onClick: () -> Unit): Modifier =
    this.clickable { onClick() }
