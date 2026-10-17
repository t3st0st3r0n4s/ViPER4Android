package com.llsl.viper4android.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
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
                    Text("Select the presets to show. Their saved names appear on the buttons.")
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
                    if (chosen.isNotEmpty()) {
                        val vertical = orientation == "vertical"
                        Text(
                            if (vertical) "Button order: top to bottom" else "Button order: left to right",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        chosen.toList().forEachIndexed { index, presetId ->
                            val name = presets.firstOrNull { it.id == presetId }?.name ?: "Missing preset"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${index + 1}. $name", modifier = Modifier.weight(1f), maxLines = 1)
                                TextButton(
                                    onClick = {
                                        chosen.removeAt(index)
                                        chosen.add(index - 1, presetId)
                                    },
                                    enabled = index > 0,
                                ) { Text(if (vertical) "↑ Up" else "← Left") }
                                TextButton(
                                    onClick = {
                                        chosen.removeAt(index)
                                        chosen.add(index + 1, presetId)
                                    },
                                    enabled = index < chosen.lastIndex,
                                ) { Text(if (vertical) "↓ Down" else "Right →") }
                            }
                        }
                        Text("Preview", style = MaterialTheme.typography.titleMedium)
                        if (orientation == "auto") {
                            Text("Auto displays horizontally in a wide widget and vertically in a tall widget.")
                        }
                        if (vertical) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
                                chosen.forEach { id ->
                                    val label = presets.firstOrNull { it.id == id }?.name ?: "Missing preset"
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.shapes.medium,
                                                ).padding(12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                            ) {
                                chosen.forEach { id ->
                                    val label = presets.firstOrNull { it.id == id }?.name ?: "Missing preset"
                                    Box(
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.shapes.medium,
                                                ).padding(vertical = 12.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
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
                                    presets = chosen.map { WidgetPreset(it) },
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
