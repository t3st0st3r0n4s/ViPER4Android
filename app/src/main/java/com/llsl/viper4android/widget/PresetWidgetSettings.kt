package com.llsl.viper4android.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WidgetPreset(val id: Long, val label: String = "")

data class WidgetConfiguration(
    val presets: List<WidgetPreset>,
    val orientation: String = "horizontal",
    val gapDp: Int = 4,
)

object PresetWidgetSettings {
    const val CHANGE_REVISION = "widget_preset_revision"
    private const val STORAGE = "viper_preset_widgets"
    private const val ACTIVE_ID = "active_preset_id"

    private fun preferences(context: Context) =
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)

    fun load(context: Context, widgetId: Int): WidgetConfiguration? {
        val raw = preferences(context).getString("widget_$widgetId", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val list = json.optJSONArray("presets") ?: JSONArray()
            val presets =
                (0 until list.length()).mapNotNull { index ->
                    val value = list.optJSONObject(index) ?: return@mapNotNull null
                    val id = value.optLong("id", 0L)
                    if (id <= 0) null else WidgetPreset(id, value.optString("label", ""))
                }
            WidgetConfiguration(
                presets = presets,
                orientation = json.optString("orientation", "horizontal"),
                gapDp = json.optInt("gapDp", 4).coerceIn(0, 20),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun save(context: Context, widgetId: Int, config: WidgetConfiguration) {
        val list = JSONArray()
        config.presets.distinctBy { it.id }.forEach {
            list.put(JSONObject().put("id", it.id).put("label", it.label))
        }
        val json =
            JSONObject()
                .put("presets", list)
                .put("orientation", config.orientation)
                .put("gapDp", config.gapDp.coerceIn(0, 20))
        preferences(context).edit().putString("widget_$widgetId", json.toString()).apply()
    }

    fun delete(context: Context, widgetIds: IntArray) {
        preferences(context).edit().apply {
            widgetIds.forEach { remove("widget_$it") }
        }.apply()
    }

    fun activePresetId(context: Context): Long? {
        val id = preferences(context).getLong(ACTIVE_ID, -1L)
        return id.takeIf { it > 0 }
    }

    fun markActive(context: Context, presetId: Long?) {
        if (activePresetId(context) == presetId) return
        preferences(context).edit().putLong(ACTIVE_ID, presetId ?: -1L).apply()
        PresetWidgetProvider.refreshAll(context)
    }
}
