package com.llsl.viper4android.widget

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

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
        TextButton(onClick = onClick) {
            Text("Pick")
        }
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
    var selected by remember(initialColor) { mutableIntStateOf(initialColor) }
    var hex by remember(initialColor) { mutableStateOf(colorHex(initialColor)) }
    fun setColor(value: Int) {
        selected = value
        hex = colorHex(value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title colour") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color(selected), MaterialTheme.shapes.medium)
                            .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(colorHex(selected), color = Color.White) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0xFFFFFFFF.toInt(), 0xFF151515.toInt(), 0xFF30343D.toInt(),
                        0xFF4068B7.toInt(), 0xFF2D9F72.toInt(), 0xFFFFA630.toInt(),
                    ).forEach { preset ->
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .background(Color(preset), RoundedCornerShape(8.dp))
                                    .clickable { setColor(preset) },
                        )
                    }
                }
                ColorChannel("Red", AndroidColor.red(selected)) { next ->
                    setColor(AndroidColor.argb(AndroidColor.alpha(selected), next, AndroidColor.green(selected), AndroidColor.blue(selected)))
                }
                ColorChannel("Green", AndroidColor.green(selected)) { next ->
                    setColor(AndroidColor.argb(AndroidColor.alpha(selected), AndroidColor.red(selected), next, AndroidColor.blue(selected)))
                }
                ColorChannel("Blue", AndroidColor.blue(selected)) { next ->
                    setColor(AndroidColor.argb(AndroidColor.alpha(selected), AndroidColor.red(selected), AndroidColor.green(selected), next))
                }
                ColorChannel("Opacity", AndroidColor.alpha(selected)) { next ->
                    setColor(AndroidColor.argb(next, AndroidColor.red(selected), AndroidColor.green(selected), AndroidColor.blue(selected)))
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { raw ->
                        hex = raw.take(9)
                        parseHex(hex)?.let { selected = it }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Hex #RRGGBB or #AARRGGBB") },
                    isError = parseHex(hex) == null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }, enabled = parseHex(hex) != null) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ColorChannel(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Text("$label: $value")
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt().coerceIn(0, 255)) },
        valueRange = 0f..255f,
    )
}
