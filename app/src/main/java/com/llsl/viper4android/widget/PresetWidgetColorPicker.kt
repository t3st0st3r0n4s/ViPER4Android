package com.llsl.viper4android.widget

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun WidgetColorSetting(label: String, color: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        CheckerboardSwatch(color, Modifier.size(32.dp).clickable(onClick = onClick))
        TextButton(onClick = onClick) { Text("Pick") }
    }
}

private fun colorHex(color: Int): String = String.format(Locale.US, "#%08X", color)

private fun parseHex(value: String): Int? {
    val digits = value.trim().removePrefix("#")
    if (!digits.matches(Regex("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))) return null
    return try { AndroidColor.parseColor("#$digits") } catch (_: IllegalArgumentException) { null }
}

@Composable
private fun CheckerboardSwatch(color: Int, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp))) {
        Canvas(Modifier.matchParentSize()) {
            val step = 10.dp.toPx()
            repeat(ceil(size.height / step).toInt()) { y ->
                repeat(ceil(size.width / step).toInt()) { x ->
                    drawRect(
                        if ((x + y) % 2 == 0) Color.White else Color(0xFFC4C4C4),
                        topLeft = Offset(x * step, y * step),
                        size = Size(step, step),
                    )
                }
            }
        }
        Box(Modifier.matchParentSize().background(Color(color)))
    }
}

@Composable
fun WidgetPickerLivePreview(
    presetLabels: List<String>,
    identity: String,
    inactiveText: Int,
    activeText: Int,
    inactiveBackground: Int,
    activeBackground: Int,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        presetLabels.take(2).ifEmpty { listOf("ON", "BASS") }.forEachIndexed { index, name ->
            val fg = if (index == 0) activeText else inactiveText
            Box(Modifier.weight(1f).height(52.dp)) {
                CheckerboardSwatch(if (index == 0) activeBackground else inactiveBackground, Modifier.matchParentSize())
                Text(
                    name,
                    modifier = Modifier.align(Alignment.Center).padding(top = 9.dp, start = 4.dp, end = 4.dp),
                    color = Color(fg), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (identity.isNotBlank()) {
                    Text(
                        identity,
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 2.dp),
                        fontSize = 9.sp, color = Color(fg), maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun PresetWidgetColorPicker(
    title: String,
    initialColor: Int,
    onPreview: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    preview: @Composable (Int) -> Unit,
) {
    val originalHsv = remember(initialColor) { FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) } }
    var hue by remember(initialColor) { mutableFloatStateOf(originalHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(originalHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(originalHsv[2]) }
    var alpha by remember(initialColor) { mutableIntStateOf(AndroidColor.alpha(initialColor)) }
    var selected by remember(initialColor) { mutableIntStateOf(initialColor) }
    var hex by remember(initialColor) { mutableStateOf(colorHex(initialColor)) }
    var squareSize by remember { mutableStateOf(IntSize.Zero) }
    var hueWidth by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current

    fun setFromHsv() {
        selected = AndroidColor.HSVToColor(alpha, floatArrayOf(hue, saturation, value))
        hex = colorHex(selected)
        onPreview(selected)
    }

    fun setFromHex(input: String) {
        hex = input.take(9)
        parseHex(hex)?.let { colour ->
            selected = colour
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(colour, hsv)
            hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
            alpha = AndroidColor.alpha(colour)
            onPreview(colour)
        }
    }

    val touchSquare by rememberUpdatedState(newValue = { point: Offset ->
        if (squareSize.width > 0 && squareSize.height > 0) {
            saturation = (point.x / squareSize.width).coerceIn(0f, 1f)
            value = (1f - point.y / squareSize.height).coerceIn(0f, 1f)
            setFromHsv()
        }
    })
    val touchHue by rememberUpdatedState(newValue = { point: Offset ->
        if (hueWidth > 0) {
            hue = (point.x / hueWidth).coerceIn(0f, 0.99999f) * 360f
            setFromHsv()
        }
    })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title colour") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("Live widget preview", style = MaterialTheme.typography.labelMedium)
                preview(selected)
                Text("Saturation / brightness", style = MaterialTheme.typography.labelMedium)
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(156.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .onSizeChanged { squareSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                touchSquare(down.position)
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.firstOrNull { it.id == down.id && it.pressed }?.let {
                                        touchSquare(it.position)
                                        it.consume()
                                    }
                                } while (event.changes.any { it.id == down.id && it.pressed })
                            }
                        },
                ) {
                    val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                    drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val dot = Offset(saturation * size.width, (1f - value) * size.height)
                    drawCircle(Color.Black, radius = 9.dp.toPx(), center = dot, style = Stroke(3.dp.toPx()))
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = dot, style = Stroke(2.dp.toPx()))
                }
                Text("Hue: ${hue.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .onSizeChanged { hueWidth = it.width }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                touchHue(down.position)
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.firstOrNull { it.id == down.id && it.pressed }?.let {
                                        touchHue(it.position)
                                        it.consume()
                                    }
                                } while (event.changes.any { it.id == down.id && it.pressed })
                            }
                        },
                ) {
                    drawRect(brush = Brush.horizontalGradient(
                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                    ))
                    val x = hue / 360f * size.width
                    drawLine(Color.Black, Offset(x, 0f), Offset(x, size.height), 5.dp.toPx())
                    drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                }
                Text("Opacity: ${(alpha * 100f / 255).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = alpha.toFloat(),
                    onValueChange = {
                        alpha = it.roundToInt().coerceIn(0, 255)
                        setFromHsv()
                    },
                    valueRange = 0f..255f,
                )
                OutlinedTextField(
                    value = hex, onValueChange = ::setFromHex,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("HEX (#RRGGBB or #AARRGGBB)") },
                    isError = parseHex(hex) == null,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(hex)) }, enabled = parseHex(hex) != null) {
                        Text("Copy")
                    }
                    TextButton(onClick = { clipboard.getText()?.text?.trim()?.let(::setFromHex) }) {
                        Text("Paste")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }, enabled = parseHex(hex) != null) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
