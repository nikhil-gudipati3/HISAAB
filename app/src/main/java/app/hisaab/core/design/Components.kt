package app.hisaab.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hisaab.core.money.digitsPaise
import app.hisaab.core.money.formatPaise

/**
 * Draw an Outline as stroked geometry. Compose's DrawScope has no drawOutline
 * in 1.7.x, so Rounded/Rectangle/Generic outlines are drawn explicitly.
 * This keeps the spec's stacked-stroke glow look exactly.
 */
private fun DrawScope.drawGlowOutline(outline: Outline, color: Color, stroke: Stroke) {
    when (outline) {
        is Outline.Rectangle -> drawRect(color, outline.rect.topLeft, outline.rect.size, style = stroke)
        is Outline.Rounded -> {
            val rr = outline.roundRect
            drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(rr.left, rr.top),
                size = androidx.compose.ui.geometry.Size(rr.width, rr.height),
                cornerRadius = rr.topLeftCornerRadius, style = stroke)
        }
        is Outline.Generic -> drawPath(outline.path, color, style = stroke)
    }
}

/** Glow drawn as stacked strokes, never a shadow. Apply BEFORE .background(). */
fun Modifier.glow(
    shape: Shape,
    color: Color = HColors.Accent,
    stroke: Dp = 1.dp,
    halos: List<Pair<Dp, Float>> = listOf(4.dp to 0.18f, 10.dp to 0.08f),
    dashed: Boolean = false
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    halos.forEach { (extra, a) ->
        drawGlowOutline(outline, color.copy(alpha = a),
            Stroke(width = (stroke + extra * 2).toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null))
    }
    drawGlowOutline(outline, color,
        Stroke(width = stroke.toPx(),
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null))
}

fun Modifier.glow0(shape: Shape): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawGlowOutline(outline, HColors.OutlineDim, Stroke(width = 1.dp.toPx()))
}

/** Rupee sign always in Geist (Sans), smaller, dimmer, raised; digits in the given style. */
@Composable
fun Amount(paise: Long, style: TextStyle, modifier: Modifier = Modifier, detail: Boolean = false, dim: Boolean = true) {
    val digits = digitsPaise(paise, detail)
    val rupeeSize = style.fontSize * 0.42f
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = Sans, fontSize = rupeeSize,
            baselineShift = BaselineShift(0.35f),
            color = if (dim) HColors.AccentDim else HColors.TextSecondary)) { append("₹") }
        append(digits)
    }
    Text(text = text, style = style, color = HColors.TextPrimary, maxLines = 1, modifier = modifier)
}

/** Simple amount text for lists: rupee in Sans + tabular digits. */
@Composable
fun AmountText(paise: Long, modifier: Modifier = Modifier, negative: Boolean = false) {
    val prefix = if (negative) "−₹" else "₹"
    Text(prefix + formatPaise(paise), style = HType.NumList, color = HColors.TextPrimary, modifier = modifier)
}

@Composable
fun TickMeter(
    remaining: Long, total: Long, modifier: Modifier = Modifier,
    ticks: Int = 28, overBudget: Boolean = false
) {
    val fraction = if (total <= 0) 0f else (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val filled = (fraction * ticks).toInt()
    val desc = if (overBudget) "Over budget" else "₹${formatPaise(remaining)} left of ₹${formatPaise(total)}"
    Row(modifier = modifier.semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(ticks) { i ->
            val on = i < filled && !overBudget
            Box(Modifier.width(2.dp).height(18.dp)
                .background(if (on) HColors.Accent else HColors.OutlineDim, RoundedCornerShape(1.dp)))
        }
    }
    if (overBudget) {
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HColors.AccentDim))
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier
        .height(56.dp).fillMaxWidth()
        .glow(shape, stroke = 1.5.dp, halos = listOf(4.dp to 0.25f, 10.dp to 0.12f, 20.dp to 0.06f))
        .background(if (enabled) HColors.Accent else HColors.OutlineDim, shape)
        .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center) {
        Text(text, style = HType.Button, color = HColors.OnAccent)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier
        .height(56.dp).fillMaxWidth()
        .glow(shape, halos = listOf(4.dp to 0.18f, 10.dp to 0.08f))
        .background(HColors.Surface, shape)
        .clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Text(text, style = HType.Button, color = HColors.TextPrimary)
    }
}

@Composable
fun GlowCard(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val base = if (selected) modifier.glow(shape) else modifier.glow0(shape)
    Column(base.background(HColors.Surface, shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(16.dp), content = content)
}
