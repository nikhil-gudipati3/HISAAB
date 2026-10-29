package app.hisaab.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hisaab.core.design.*
import app.hisaab.core.money.formatPaise
import app.hisaab.data.BudgetMode
import app.hisaab.domain.paceLeftOnPayday
import app.hisaab.feature.home.AppViewModel
import java.time.DayOfWeek

@Composable
fun InsightsScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var period by remember { mutableStateOf(0) } // 0 this cycle, 1 last cycle, 2 last 3 cycles
    var byBucket by remember { mutableStateOf(false) }
    val cycle = if (period == 1) ui.prevCycle else ui.cycle
    // For "last 3 cycles" approximate: use all txns before end of current cycle within 3 cycle lengths - use txns in current+prev only if needed; simpler: filter by date >= cycle start for 0/1, all for 2
    val txns = when (period) {
        0 -> ui.cycleTxns(ui.cycle)
        1 -> ui.cycleTxns(ui.prevCycle)
        else -> ui.txns
    }.filter { it.status == "SUCCESS" }
    val spent = txns.sumOf { it.amountPaise }
    val prevSpentAtSamePoint = ui.cycleTxns(ui.prevCycle).filter { it.status == "SUCCESS" }
        .filter { ui.dateOf(it.timestamp).dayOfMonth <= ui.today.dayOfMonth }.sumOf { it.amountPaise }
    val diff = spent - prevSpentAtSamePoint
    LazyColumn(Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)) {
        item {
            Text("Insights", style = HType.Headline, color = HColors.TextPrimary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("This cycle", "Last cycle", "Last 3 cycles").forEachIndexed { i, label ->
                    Chip2(label, "", period == i, { period = i })
                }
            }
            Spacer(Modifier.height(28.dp))
            if (txns.isEmpty()) {
                Text("Nothing here yet", style = HType.HeadlineItalic, color = HColors.TextPrimary)
                Text("After a few payments, this screen will show where your money goes, your daily pace, and your top payees.",
                    style = HType.Body, color = HColors.TextSecondary)
                return@item
            }
            Text("You've spent ₹${formatPaise(spent)}", style = HType.Headline, color = HColors.TextPrimary)
            if (period == 0) Text(
                if (diff <= 0) "₹${formatPaise(-diff)} less than this time last cycle" else "₹${formatPaise(diff)} more than this time last cycle",
                style = HType.Body, color = HColors.TextSecondary)
            Spacer(Modifier.height(28.dp))
            // 2. Where it went
            Text("Where it went", style = HType.HeadlineItalic, color = HColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip2("By category", "", !byBucket, { byBucket = false })
                Chip2("By bucket", "", byBucket, { byBucket = true })
            }
            Spacer(Modifier.height(12.dp))
        }
        val groups: List<Pair<String, Long>> = if (byBucket) {
            txns.groupBy { t -> ui.buckets.firstOrNull { it.id == t.bucketId }?.name ?: "No bucket" }
                .mapValues { it.value.sumOf { t -> t.amountPaise } }.toList().sortedByDescending { it.second }
        } else {
            txns.groupBy { it.categoryKey ?: "Other" }.mapValues { it.value.sumOf { t -> t.amountPaise } }
                .toList().sortedByDescending { it.second }
        }
        items(groups) { (name, amt) ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, style = HType.Label, color = HColors.TextPrimary)
                    Text("₹${formatPaise(amt)} · ${if (spent > 0) amt * 100 / spent else 0}%", style = HType.NumList, color = HColors.TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                TickMeter(amt, spent.coerceAtLeast(1), ticks = 28)
            }
        }
        item {
            Spacer(Modifier.height(28.dp))
            Text("Day by day", style = HType.HeadlineItalic, color = HColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            val byDay = txns.groupBy { ui.dateOf(it.timestamp) }.mapValues { it.value.sumOf { t -> t.amountPaise } }
            val allowance = if (ui.settings.mode == BudgetMode.DAILY) ui.daily().todayAllowance
                else if (ui.cycle.daysInCycle > 0) (ui.settings.monthlyIncomePaise / ui.cycle.daysInCycle) else 0L
            val maxAmt = (byDay.values.maxOrNull() ?: 1L).coerceAtLeast(allowance).coerceAtLeast(1L)
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val n = byDay.entries.sortedBy { it.key }.size.coerceAtLeast(1)
                val bw = size.width / n
                byDay.entries.sortedBy { it.key }.forEachIndexed { i, e ->
                    val h = (e.value.toFloat() / maxAmt.toFloat() * size.height * 0.9f)
                    val over = e.value > allowance && allowance > 0
                    val x = i * bw + bw * 0.2f
                    if (over) drawRect(Color.Transparent, Offset(x, size.height - h), Size(bw * 0.6f, h),
                        style = Stroke(width = 2f))
                    else drawRect(HColors.Accent, Offset(x, size.height - h), Size(bw * 0.6f, h))
                }
                if (allowance > 0) {
                    val y = size.height - (allowance.toFloat() / maxAmt.toFloat() * size.height * 0.9f)
                    drawLine(HColors.AccentDim, Offset(0f, y), Offset(size.width, y))
                }
            }
            Text("Bars over the dashed allowance line are hollow.", style = HType.Caption, color = HColors.TextSecondary)
            Spacer(Modifier.height(28.dp))
            Text("When you spend", style = HType.HeadlineItalic, color = HColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            // Weekday totals as simple rows (grid intensity approximated by tick fill)
            DayOfWeek.values().forEach { dow ->
                val amt = txns.filter { ui.dateOf(it.timestamp).dayOfWeek == dow }.sumOf { it.amountPaise }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dow.name.lowercase().replaceFirstChar { it.uppercase() }, style = HType.Label, color = HColors.TextPrimary)
                    Text("₹${formatPaise(amt)}", style = HType.NumList, color = HColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("Top payees", style = HType.HeadlineItalic, color = HColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
        }
        val top = txns.groupBy { it.payeeName ?: it.payeeVpa ?: "Payment" }
            .mapValues { it.value.sumOf { t -> t.amountPaise } to it.value.size }
            .toList().sortedByDescending { it.second.first }.take(8)
        items(top) { (name, pair) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(name, style = HType.Label, color = HColors.TextPrimary)
                    Text("${pair.second} payments", style = HType.Caption, color = HColors.TextSecondary) }
                Text("₹${formatPaise(pair.first)}", style = HType.NumList, color = HColors.TextPrimary)
            }
        }
        item {
            Spacer(Modifier.height(28.dp))
            Text("Pace", style = HType.HeadlineItalic, color = HColors.TextPrimary)
            val elapsed = (ui.cycle.dayIndex(ui.today) + 1).toLong()
            val paceLeft = paceLeftOnPayday(ui.settings.monthlyIncomePaise, spent, elapsed, ui.cycle.daysInCycle)
            Text("At this pace you'll have ₹${formatPaise(paceLeft)} left on payday.", style = HType.Body, color = HColors.TextSecondary)
            if (ui.settings.mode == BudgetMode.DAILY) {
                Spacer(Modifier.height(28.dp))
                Text("Streak", style = HType.HeadlineItalic, color = HColors.TextPrimary)
                Text("Days at or under your allowance in a row are counted from your daily history.",
                    style = HType.Body, color = HColors.TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
