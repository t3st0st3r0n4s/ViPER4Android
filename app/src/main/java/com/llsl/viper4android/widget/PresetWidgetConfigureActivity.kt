package com.llsl.viper4android.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.data.repository.ViperRepository
import com.llsl.viper4android.ui.theme.ViperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt
import javax.inject.Inject

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
                var outerInset by remember(widgetId) {
                    mutableFloatStateOf((previous?.outerInsetDp ?: 0).toFloat())
                }
                var textColor by remember(widgetId) {
                    mutableIntStateOf(previous?.textColor ?: DEFAULT_TEXT_COLOR)
                }
                var widgetColor by remember(widgetId) {
                    mutableIntStateOf(previous?.widgetColor ?: DEFAULT_WIDGET_COLOR)
                }
                var activeColor by remember(widgetId) {
                    mutableIntStateOf(previous?.activeColor ?: DEFAULT_ACTIVE_COLOR)
                }
                var editingColor by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    bottomBar = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .navigationBarsPadding()
                                    .imePadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = chosen.isNotEmpty(),
                                onClick = {
                                    PresetWidgetSettings.save(
                                        this@PresetWidgetConfigureActivity,
                                        widgetId,
                                        WidgetConfiguration(
                                            presets = chosen.map { WidgetPreset(it) },
                                            orientation = orientation,
                                            gapDp = spacing.roundToInt(),
                                            outerInsetDp = outerInset.roundToInt(),
                                            textColor = textColor,
                                            widgetColor = widgetColor,
                                            activeColor = activeColor,
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
                    },
                ) { contentPadding ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Preset widget", style = MaterialTheme.typography.titleLarge)
                        Text("Select which saved presets appear on this widget.")
                        presets.forEach { preset ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = preset.id in chosen,
                                    onCheckedChange = { selected ->
                                        if (selected && preset.id !in chosen) chosen.add(preset.id)
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
                            Text(
                                "Arrange buttons — long-press and drag in the preview",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (orientation == "auto") {
                                Text("Auto uses a horizontal layout when wide, vertical when tall.")
                            }
                            PresetWidgetPreview(
                                chosen = chosen,
                                names = presets.associate { it.id to it.name },
                                vertical = orientation == "vertical",
                                gapDp = spacing.roundToInt(),
                                outerInsetDp = outerInset.roundToInt(),
                                textColor = textColor,
                                widgetColor = widgetColor,
                                activeColor = activeColor,
                            )
                        }

                        Text("Between buttons: ${spacing.roundToInt()} dp")
                        Slider(value = spacing, onValueChange = { spacing = it }, valueRange = 0f..96f)
                        Text("Outside inset: ${outerInset.roundToInt()} dp")
                        Slider(value = outerInset, onValueChange = { outerInset = it }, valueRange = 0f..64f)

                        Text("Colours", style = MaterialTheme.typography.titleMedium)
                        WidgetColorSetting("Text", textColor) { editingColor = "Text" }
                        WidgetColorSetting("Widget (inactive)", widgetColor) { editingColor = "Widget" }
                        WidgetColorSetting("Active preset", activeColor) { editingColor = "Active" }
                    }
                }

                val title = editingColor
                if (title != null) {
                    key(title) {
                        PresetWidgetColorPicker(
                            title = title,
                            initialColor =
                                when (title) {
                                    "Text" -> textColor
                                    "Widget" -> widgetColor
                                    else -> activeColor
                                },
                            onDismiss = { editingColor = null },
                            onSelect = { selected ->
                                when (title) {
                                    "Text" -> textColor = selected
                                    "Widget" -> widgetColor = selected
                                    else -> activeColor = selected
                                }
                                editingColor = null
                            },
                        )
                    }
                }
            }
        }
    }
}
