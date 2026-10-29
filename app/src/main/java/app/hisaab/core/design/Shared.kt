package app.hisaab.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun fmtDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM"))
fun fmtTime(ts: Long): String {
    val t = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return t.format(DateTimeFormatter.ofPattern("h:mm a"))
}

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp)
        .padding(bottom = 110.dp), content = content)
}

@Composable
fun Chip2(name: String, sub: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val base = if (selected) modifier.glow(shape) else modifier.glow0(shape)
    Column(base.background(HColors.Surface, shape).clip(shape).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(name, style = HType.Label, color = HColors.TextPrimary)
        Text(sub, style = HType.Caption, color = HColors.TextSecondary)
    }
}

@Composable
fun Keypad(onDigit: (String) -> Unit, onBack: () -> Unit, onDot: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { k ->
                    Box(Modifier.weight(1f).height(64.dp).clickable {
                        when (k) { "⌫" -> onBack(); "." -> onDot(); else -> onDigit(k) }
                    }, contentAlignment = Alignment.Center) {
                        Text(k, style = HType.Title.copy(fontSize = 28.sp))
                    }
                }
            }
        }
    }
}
