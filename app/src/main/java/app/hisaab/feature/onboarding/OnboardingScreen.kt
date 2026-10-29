package app.hisaab.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hisaab.core.design.*
import app.hisaab.core.money.formatPaise
import app.hisaab.core.money.parsePaise
import app.hisaab.data.*
import app.hisaab.feature.home.AppViewModel
import app.hisaab.feature.settings.DISCLAIMER

@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf(0) }
    var income by remember { mutableStateOf("") }
    var payday by remember { mutableStateOf("1") }
    var mode by remember { mutableStateOf(BudgetMode.BUCKETS) }
    var accountName by remember { mutableStateOf("") }
    var savings by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf(true) }

    fun finish() {
        // Guard: if onboarding is ever shown again (e.g. app killed mid-finish),
        // only persist settings; never seed duplicate buckets/accounts.
        if (ui.buckets.isNotEmpty() || ui.accounts.isNotEmpty()) {
            vm.updateSettings {
                it.copy(mode = mode, monthlyIncomePaise = parsePaise(income) ?: 0L,
                    paydayDay = payday.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                    savingsTargetPaise = parsePaise(savings) ?: 0L, onboardingDone = true)
            }
            onDone(); return
        }
        val inc = parsePaise(income) ?: 0L
        vm.updateSettings {
            it.copy(mode = mode, monthlyIncomePaise = inc, paydayDay = payday.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                savingsTargetPaise = parsePaise(savings) ?: 0L, onboardingDone = true)
        }
        if (accountName.isNotBlank()) vm.addAccount(AccountEntity(name = accountName.trim(), sortOrder = 0))
        if (mode == BudgetMode.BUCKETS && preset && inc > 0) {
            // 50/30/20 of income; rounding keeps paise exact by giving remainder to Needs.
            val needs = inc * 50 / 100; val wants = inc * 30 / 100; val sav = inc - needs - wants
            vm.addBucket(BucketEntity(name = "Needs", monthlyBudgetPaise = needs, sortOrder = 0))
            vm.addBucket(BucketEntity(name = "Wants", monthlyBudgetPaise = wants, sortOrder = 1))
            vm.addBucket(BucketEntity(name = "Savings", monthlyBudgetPaise = sav, isSavings = true, sortOrder = 2))
        } else if (mode == BudgetMode.BUCKETS) {
            vm.addBucket(BucketEntity(name = "Needs", monthlyBudgetPaise = 0L, sortOrder = 0))
            vm.addBucket(BucketEntity(name = "Wants", monthlyBudgetPaise = 0L, sortOrder = 1))
        }
        onDone()
    }

    LazyColumn(Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)) {
        item {
            when (step) {
                0 -> {
                    Text("Know what's left before you spend it.", style = HType.Headline, color = HColors.TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Text("Hisaab is a budgeting layer for your UPI spending. It sits in front of the UPI app you already use and answers one question: which part of your money is this coming from, and what is left?", style = HType.Body, color = HColors.TextSecondary)
                    Spacer(Modifier.height(24.dp))
                    Text(DISCLAIMER, style = HType.Caption, color = HColors.TextSecondary)
                    Spacer(Modifier.height(32.dp))
                    PrimaryButton("Start") { step = 1 }
                }
                1 -> {
                    Text("Income and payday", style = HType.Headline, color = HColors.TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(income, { income = it }, label = { Text("Monthly income (₹)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(payday, { payday = it }, label = { Text("Payday day (1–31)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(32.dp))
                    PrimaryButton("Continue") { step = 2 }
                }
                2 -> {
                    Text("Pick a style", style = HType.Headline, color = HColors.TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    val inc = parsePaise(income) ?: 0L
                    GlowCard(mode == BudgetMode.BUCKETS, { mode = BudgetMode.BUCKETS }, Modifier.fillMaxWidth()) {
                        Text("Buckets", style = HType.Title, color = HColors.TextPrimary)
                        Text("Split income into buckets like Needs and Wants. Every payment comes from one bucket.", style = HType.Body, color = HColors.TextSecondary)
                        if (inc > 0) Text("Example: Needs ₹${formatPaise(inc * 50 / 100)} · Wants ₹${formatPaise(inc * 30 / 100)} · Savings ₹${formatPaise(inc * 20 / 100)}", style = HType.Caption, color = HColors.TextSecondary)
                    }
                    Spacer(Modifier.height(12.dp))
                    GlowCard(mode == BudgetMode.DAILY, { mode = BudgetMode.DAILY }, Modifier.fillMaxWidth()) {
                        Text("Daily allowance", style = HType.Title, color = HColors.TextPrimary)
                        Text("One safe amount to spend each day. Unspent money is swept into savings by default.", style = HType.Body, color = HColors.TextSecondary)
                        if (inc > 0) Text("Example: about ₹${formatPaise(inc / 30)} a day over a 30-day cycle.", style = HType.Caption, color = HColors.TextSecondary)
                    }
                    Spacer(Modifier.height(32.dp))
                    PrimaryButton("Continue") { step = 3 }
                }
                3 -> {
                    Text("Accounts", style = HType.Headline, color = HColors.TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Text("Add a label for the bank account your UPI payments come from. This is a label only — never an account number or credential.", style = HType.Body, color = HColors.TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(accountName, { accountName = it }, label = { Text("Bank label, for example HDFC Salary") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(32.dp))
                    PrimaryButton("Continue") { step = 4 }
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Add later") { step = 4 }
                }
                4 -> {
                    if (mode == BudgetMode.BUCKETS) {
                        Text("Buckets", style = HType.Headline, color = HColors.TextPrimary)
                        Spacer(Modifier.height(16.dp))
                        Text("Start with the 50 / 30 / 20 split, or set your own amounts later in Buckets.", style = HType.Body, color = HColors.TextSecondary)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip2("50 / 30 / 20", "Needs, Wants, Savings", preset, { preset = true })
                            Chip2("Custom later", "Empty buckets to fill in", !preset, { preset = false })
                        }
                    } else {
                        Text("Savings target", style = HType.Headline, color = HColors.TextPrimary)
                        Spacer(Modifier.height(16.dp))
                        Text("How much do you want to set aside each month? Leave it blank for none.", style = HType.Body, color = HColors.TextSecondary)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(savings, { savings = it }, label = { Text("Monthly savings target (₹)") }, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(32.dp))
                    PrimaryButton("You're set") { finish() }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Step ${step + 1} of 5", style = HType.Caption, color = HColors.TextTertiary)
        }
    }
}
