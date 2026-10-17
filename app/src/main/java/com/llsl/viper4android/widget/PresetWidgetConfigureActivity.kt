package com.llsl.viper4android.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.data.repository.ViperRepository
import com.llsl.viper4android.ui.theme.ViperTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class PresetWidgetConfigureActivity : ComponentActivity() {
    @Inject
    lateinit var repository: ViperRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (widgetId == -1) {
            finish()
            return
        }
        val previous = PresetWidgetSettings.load(this, widgetId)
        setContent {
            ViperTheme {
                val source = remember { repository.getAllPresets() }
                val presets by source.collectAsState(initial = emptyList())
                val chosen =
                    remember(widgetId) {
                        mutableStateListOf<Long>().apply {
                            addAll(previous?.presets?.map { it.id }.orEmpty())
                        }
                    }
                val labels =
                    remember(widgetId) {
                        mutableStateMapOf<Long, String>().apply {
                            previous?.presets?.forEach { put(it.id, it.label) }
                        }
                    }
                var orientation by remember(widgetId) {
                    mutableStateOf(previous?.orientation ?: "horizontal")
                }
                var spacing by remember(widgetId) {
                    mutableFloatStateOf((previous?.gapDp ?: 4).toFloat())
                }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Preset widget", style = MaterialTheme.typography.titleLarge)
                    Text("Choose any presets. Each widget has its own layout and labels.")
                    presets.forEach { preset ->
                        val checked = preset.id in chosen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { selected ->
                                    if (selected && !checked) chosen.add(preset.id)
                                    if (!selected) chosen.remove(preset.id)
                                },
                            )
                            Text(preset.name)
                        }
                        if (checked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = labels[preset.id].orEmpty(),
                                    onValueChange = { labels[preset.id] = it },
                                    label = { Text("Label (optional)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                TextButton(
                                    onClick = {
                                        val i = chosen.indexOf(preset.id)
                                        if (i > 0) {
                                            chosen.removeAt(i)
                                            chosen.add(i - 1, preset.id)
                                        }
                                    },
                                    enabled = chosen.indexOf(preset.id) > 0,
                                ) { Text("↑") }
                                TextButton(
                                    onClick = {
                                        val i = chosen.indexOf(preset.id)
                                        if (i >= 0 && i < chosen.lastIndex) {
                                            chosen.removeAt(i)
                                            chosen.add(i + 1, preset.id)
                                        }
                                    },
                                    enabled = chosen.indexOf(preset.id) < chosen.lastIndex,
                                ) { Text("↓") }
                            }
                        }
                    }
                    Text("Layout", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("horizontal", "vertical", "auto").forEach { option ->
                            FilterChip(
                                selected = orientation == option,
                                onClick = { orientation = option },
                                label = { Text(option.replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    Text("Button gap: ${spacing.roundToInt()} dp")
                    Slider(
                        value = spacing,
                        onValueChange = { spacing = it },
                        valueRange = 0f..20f,
                        steps = 19,
                    )
                    Button(
                        enabled = chosen.isNotEmpty(),
                        onClick = {
                            PresetWidgetSettings.save(
                                this@PresetWidgetConfigureActivity,
                                widgetId,
                                WidgetConfiguration(
                                    presets = chosen.map { WidgetPreset(it, labels[it].orEmpty()) },
                                    orientation = orientation,
                                    gapDp = spacing.roundToInt(),
                                ),
                            )
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                            )
                            PresetWidgetProvider.refreshAll(this@PresetWidgetConfigureActivity)
                            finish()
                        },
                    ) { Text("Save widget") }
                }
            }
        }
    }
}
