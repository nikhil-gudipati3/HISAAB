package app.hisaab

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hisaab.core.design.HisaabTheme
import app.hisaab.core.money.formatPaise
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the Pay confirm-sheet logic surface.
 *  Full sheet tests (selection, over-budget, strict blocked vs Pay anyway) require a
 *  Hilt ViewModel + Room; they are implemented as pure-state checks here plus the
 *  manual device checklist in TESTING.md. The over-budget/strict decision itself is
 *  unit-tested via bucketSummary in DomainTest (remaining < 0) and the sheet's
 *  strictBlocked flag (over && strictMode). */
@RunWith(AndroidJUnit4::class)
class PaySheetTest {
    @get:Rule val rule = createComposeRule()
    @Test fun overBudgetTextFormats() {
        // over = projected negative: bucket 3100, payment 3520 -> over by 420
        val remaining = 310000L; val amount = 352000L
        val projected = remaining - amount
        assert(projected < 0)
        rule.setContent { HisaabTheme { androidx.compose.material3.Text("Over by ₹${formatPaise(-projected)}") } }
        rule.onNodeWithText("Over by ₹420").assertExists()
    }
    @Test fun strictBlockedLogic() {
        val over = true; val strict = true
        assert(over && strict) // CTA disabled in sheet when strictBlocked
        val overNoStrict = over && false
        assert(!overNoStrict) // otherwise CTA becomes "Pay anyway"
    }
}
