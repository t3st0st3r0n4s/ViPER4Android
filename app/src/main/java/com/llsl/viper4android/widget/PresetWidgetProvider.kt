package com.llsl.viper4android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.res.ColorStateList
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.llsl.viper4android.R
import com.llsl.viper4android.data.model.Preset
import com.llsl.viper4android.data.repository.ViperRepository
import com.llsl.viper4android.service.ViperService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PresetWidgetProvider : AppWidgetProvider() {
    @Inject
    lateinit var repository: ViperRepository

    companion object {
        private const val ACTION_SELECT = "com.llsl.viper4android.widget.SELECT"
        private const val EXTRA_PRESET_ID = "preset_id"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PresetWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, PresetWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SELECT) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val presetId = intent.getLongExtra(EXTRA_PRESET_ID, -1L)
            val configured = PresetWidgetSettings.load(context, widgetId)
            if (presetId > 0 && configured?.presets?.any { it.id == presetId } == true) {
                ViperService.applyWidgetPreset(context, presetId)
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val all = repository.getAllPresets().first().associateBy { it.id }
                widgetIds.forEach { id ->
                    render(context, manager, id, all)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, manager, intArrayOf(widgetId))
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        PresetWidgetSettings.delete(context, widgetIds)
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        presets: Map<Long, Preset>,
    ) {
        val config = PresetWidgetSettings.load(context, widgetId)
        val options = manager.getAppWidgetOptions(widgetId)
        val vertical =
            when (config?.orientation) {
                "vertical" -> true
                "auto" -> {
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) <
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                }
                else -> false
            }
        val layout = if (vertical) R.layout.widget_presets_vertical else R.layout.widget_presets_horizontal
        val views = RemoteViews(context.packageName, layout)
        views.removeAllViews(R.id.widget_preset_container)
        val outerInset =
            ((config?.outerInsetDp ?: 0) * context.resources.displayMetrics.density).toInt()
        views.setViewPadding(
            R.id.widget_preset_container,
            outerInset,
            outerInset,
            outerInset,
            outerInset,
        )
        val choices = config?.presets.orEmpty()
        if (choices.isEmpty()) {
            val empty = RemoteViews(context.packageName, R.layout.widget_presets_empty)
            val configure =
                Intent(context, PresetWidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            empty.setOnClickPendingIntent(
                R.id.widget_preset_item,
                PendingIntent.getActivity(
                    context,
                    widgetId,
                    configure,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            views.addView(R.id.widget_preset_container, empty)
        } else {
            val active = PresetWidgetSettings.activePresetId(context)
            val gapDp = config?.gapDp ?: 4
            val gapPx = (context.resources.displayMetrics.density * gapDp).toInt()
            choices.forEachIndexed { index, choice ->
                val preset = presets[choice.id]
                val itemLayout =
                    when {
                        vertical && active == choice.id -> R.layout.widget_preset_item_vertical_active
                        vertical -> R.layout.widget_preset_item_vertical_inactive
                        active == choice.id -> R.layout.widget_preset_item_horizontal_active
                        else -> R.layout.widget_preset_item_horizontal_inactive
                    }
                val item = RemoteViews(context.packageName, itemLayout)
                item.setTextViewText(
                    R.id.widget_preset_label,
                    preset?.name ?: "Missing preset",
                )
                val labelColor =
                    if (active == choice.id) config?.activeTextColor ?: DEFAULT_TEXT_COLOR
                    else config?.textColor ?: DEFAULT_TEXT_COLOR
                item.setTextColor(R.id.widget_preset_label, labelColor)
                val identity =
                    when (config?.identityMode) {
                        "none" -> ""
                        "custom" -> config.identityText.ifBlank { "ViPER" }
                        else -> "ViPER"
                    }
                item.setTextViewText(R.id.widget_identity, identity)
                item.setTextColor(R.id.widget_identity, labelColor)
                item.setViewVisibility(
                    R.id.widget_identity,
                    if (identity.isBlank()) View.GONE else View.VISIBLE,
                )
                val backgroundColor =
                    if (active == choice.id) config?.activeColor ?: DEFAULT_ACTIVE_COLOR
                    else config?.widgetColor ?: DEFAULT_WIDGET_COLOR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item.setColorStateList(
                        R.id.widget_preset_label,
                        "setBackgroundTintList",
                        ColorStateList.valueOf(backgroundColor),
                    )
                } else {
                    // Android < 12 has no dynamic RemoteViews ColorStateList setter.
                    item.setInt(R.id.widget_preset_label, "setBackgroundColor", backgroundColor)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Margins participate in LinearLayout weight distribution, so every
                    // button (including the first) shrinks equally as spacing increases.
                    if (index > 0) {
                        item.setViewLayoutMargin(
                            R.id.widget_preset_item,
                            if (vertical) RemoteViews.MARGIN_TOP else RemoteViews.MARGIN_LEFT,
                            gapDp.toFloat(),
                            TypedValue.COMPLEX_UNIT_DIP,
                        )
                    }
                } else {
                    // RemoteViews layout-margin actions were added in Android 12.
                    // Keep a usable symmetric visual gap for supported older devices.
                    item.setViewPadding(
                        R.id.widget_preset_item,
                        if (vertical || index == 0) 0 else gapPx / 2,
                        if (!vertical || index == 0) 0 else gapPx / 2,
                        if (vertical || index == choices.lastIndex) 0 else gapPx / 2,
                        if (!vertical || index == choices.lastIndex) 0 else gapPx / 2,
                    )
                }
                if (preset != null) {
                    val click =
                        Intent(context, PresetWidgetProvider::class.java).apply {
                            action = ACTION_SELECT
                            data = Uri.parse("viper-widget://$widgetId/${choice.id}")
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            putExtra(EXTRA_PRESET_ID, choice.id)
                        }
                    item.setOnClickPendingIntent(
                        R.id.widget_preset_item,
                        PendingIntent.getBroadcast(
                            context,
                            widgetId,
                            click,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
                views.addView(R.id.widget_preset_container, item)
            }
        }
        manager.updateAppWidget(widgetId, views)
    }
}
