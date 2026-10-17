package com.llsl.viper4android.widget

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WidgetColorSetting(
    label: String,
    color: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(Color(color), RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick),
        )
        TextButton(onClick = onClick) { Text("Pick") }
    }
}

private fun colorHex(color: Int): String = String.format(Locale.US, "#%08X", color)

private fun parseHex(value: String): Int? {
    val digits = value.removePrefix("#")
    if (!digits.matches(Regex("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))) return null
    return try {
        AndroidColor.parseColor("#$digits")
    } catch (_: IllegalArgumentException) {
        null
    }
}

@Composable
fun PresetWidgetColorPicker(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    var opacity by remember(initialColor) { mutableIntStateOf(AndroidColor.alpha(initialColor)) }
    var selected by remember(initialColor) { mutableIntStateOf(initialColor) }
    var hex by remember(initialColor) { mutableStateOf(colorHex(initialColor)) }
    var spectrumSize by remember { mutableStateOf(IntSize.Zero) }

    fun updateFromHsv() {
        val color = AndroidColor.HSVToColor(opacity, floatArrayOf(hue, saturation, brightness))
        selected = color
        hex = colorHex(color)
    }

    fun selectFromSpectrum(position: Offset) {
        if (spectrumSize.width < 1 || spectrumSize.height < 1) return
        hue = (position.x / spectrumSize.width).coerceIn(0f, 0.9999f) * 360f
        saturation = (position.y / spectrumSize.height).coerceIn(0f, 1f)
        updateFromHsv()
    }

    val onSpectrumTouch by rememberUpdatedState(newValue = { position: Offset ->
        selectFromSpectrum(position)
    })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title colour") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                            .padding(4.dp)
                            .background(Color(selected), MaterialTheme.shapes.medium)
                            .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(colorHex(selected), color = if (brightness > 0.55f) Color.Black else Color.White)
                }

                Text("Colour spectrum")
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .onSizeChanged { spectrumSize = it }
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onSpectrumTouch(it) })
                            }.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { onSpectrumTouch(it) },
                                    onDrag = { change, _ ->
                                        onSpectrumTouch(change.position)
                                        change.consume()
                                    },
                                )
                            },
                ) {
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red,
                                ),
                            ),
                    )
                    drawRect(
                        brush = Brush.verticalGradient(listOf(Color.White, Color.Transparent)),
                    )
                    val cursor = Offset(hue / 360f * size.width, saturation * size.height)
                    drawCircle(Color.Black, radius = 10.dp.toPx(), center = cursor, style = Stroke(3.dp.toPx()))
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = cursor, style = Stroke(2.dp.toPx()))
                }
                Text("Brightness: ${(brightness * 100).roundToInt()}%")
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        updateFromHsv()
                    },
                    valueRange = 0f..1f,
                )
                Text("Opacity: ${(opacity * 100f / 255).roundToInt()}%")
                Slider(
                    value = opacity.toFloat(),
                    onValueChange = {
                        opacity = it.roundToInt().coerceIn(0, 255)
                        updateFromHsv()
                    },
                    valueRange = 0f..255f,
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { raw ->
                        hex = raw.take(9)
                        parseHex(hex)?.let { color ->
                            selected = color
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(color, hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                            opacity = AndroidColor.alpha(color)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Hex: #RRGGBB or #AARRGGBB") },
                    isError = parseHex(hex) == null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelect(selected) },
                enabled = parseHex(hex) != null,
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
