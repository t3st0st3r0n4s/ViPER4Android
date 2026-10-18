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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
                var presetTextSize by remember(widgetId) {
                    mutableFloatStateOf((previous?.presetTextSizeSp ?: 13).toFloat())
                }
                var presetTextWeight by remember(widgetId) {
                    mutableStateOf(previous?.presetTextWeight ?: "normal")
                }
                var activeTextColor by remember(widgetId) {
                    mutableIntStateOf(previous?.activeTextColor ?: DEFAULT_TEXT_COLOR)
                }
                var identityMode by remember(widgetId) {
                    mutableStateOf(previous?.identityMode ?: "viper")
                }
                var identityText by remember(widgetId) {
                    mutableStateOf(previous?.identityText ?: "")
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
                                            presetTextSizeSp = presetTextSize.roundToInt(),
                                            presetTextWeight = presetTextWeight,
                                            activeTextColor = activeTextColor,
                                            identityMode = identityMode,
                                            identityText = identityText.trim().take(18),
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

                        Text("Widget identity (inside buttons)", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "viper" to "ViPER",
                                "custom" to "Custom",
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = identityMode == mode,
                                    onClick = { identityMode = mode },
                                    label = { Text(label) },
                                )
                            }
                        }
                        if (identityMode == "custom") {
                            OutlinedTextField(
                                value = identityText,
                                onValueChange = { identityText = it.replace("\n", " ").take(18) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Button identity") },
                                placeholder = { Text("e.g. Headphones") },
                                singleLine = true,
                                supportingText = { Text("Leave empty to hide the identity label") },
                            )
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
                                textSizeSp = presetTextSize.roundToInt(),
                                textWeight = presetTextWeight,
                                activeTextColor = activeTextColor,
                                widgetColor = widgetColor,
                                identityMode = identityMode,
                                identityText = identityText,
                                activeColor = activeColor,
                            )
                        }

                        Text("Between buttons: ${spacing.roundToInt()} dp")
                        Slider(value = spacing, onValueChange = { spacing = it }, valueRange = 0f..96f)
                        Text("Outside inset: ${outerInset.roundToInt()} dp")
                        Slider(value = outerInset, onValueChange = { outerInset = it }, valueRange = 0f..64f)

                        Text("Preset label", style = MaterialTheme.typography.titleMedium)
                        Text("Size: ${presetTextSize.roundToInt()} sp")
                        Slider(
                            value = presetTextSize,
                            onValueChange = { presetTextSize = it },
                            valueRange = 10f..30f,
                            steps = 19,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("normal" to "Regular", "medium" to "Medium", "bold" to "Bold")
                                .forEach { (weight, label) ->
                                    FilterChip(
                                        selected = presetTextWeight == weight,
                                        onClick = { presetTextWeight = weight },
                                        label = { Text(label) },
                                    )
                                }
                        }

                        Text("Colours", style = MaterialTheme.typography.titleMedium)
                        WidgetColorSetting("Text (inactive)", textColor) { editingColor = "Text" }
                        WidgetColorSetting("Text (active)", activeTextColor) { editingColor = "Active text" }
                        WidgetColorSetting("Widget (inactive)", widgetColor) { editingColor = "Widget" }
                        WidgetColorSetting("Active preset", activeColor) { editingColor = "Active" }
                    }
                }

                val title = editingColor
                if (title != null) {
                    key(title) {
                        // Freeze the original colour while preview updates the underlying state.
                        val initial = remember {
                            when (title) {
                                "Text" -> textColor
                                "Active text" -> activeTextColor
                                "Widget" -> widgetColor
                                else -> activeColor
                            }
                        }
                        val updateColour: (Int) -> Unit = { colour ->
                            when (title) {
                                "Text" -> textColor = colour
                                "Active text" -> activeTextColor = colour
                                "Widget" -> widgetColor = colour
                                else -> activeColor = colour
                            }
                        }
                        PresetWidgetColorPicker(
                            title = title,
                            initialColor = initial,
                            onPreview = updateColour,
                            onDismiss = {
                                updateColour(initial)
                                editingColor = null
                            },
                            onSelect = { colour ->
                                updateColour(colour)
                                editingColor = null
                            },
                            preview = { picked ->
                                WidgetPickerLivePreview(
                                    presetLabels = chosen.take(2).map { id ->
                                        presets.firstOrNull { it.id == id }?.name ?: "Missing preset"
                                    },
                                    identity = if (identityMode == "custom") identityText.trim() else "ViPER",
                                    inactiveText = if (title == "Text") picked else textColor,
                                    activeText = if (title == "Active text") picked else activeTextColor,
                                    inactiveBackground = if (title == "Widget") picked else widgetColor,
                                    activeBackground = if (title == "Active") picked else activeColor,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
