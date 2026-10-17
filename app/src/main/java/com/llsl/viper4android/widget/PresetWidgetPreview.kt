package com.llsl.viper4android.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs

@Composable
fun PresetWidgetPreview(
    chosen: SnapshotStateList<Long>,
    names: Map<Long, String>,
    vertical: Boolean,
    gapDp: Int,
    outerInsetDp: Int,
    textColor: Int,
    widgetColor: Int,
    activeColor: Int,
) {
    // One preview is also the ordering control: no separate duplicate preset list.
    val entries = chosen.toList()
    if (vertical) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(outerInsetDp.dp),
            verticalArrangement = Arrangement.spacedBy(gapDp.dp),
        ) {
            entries.forEachIndexed { index, id ->
                key(id) {
                    PreviewPresetButton(
                        id = id,
                        label = names[id] ?: "Missing preset",
                        chosen = chosen,
                        vertical = true,
                        gapDp = gapDp,
                        textColor = textColor,
                        backgroundColor = if (index == 0) activeColor else widgetColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(outerInsetDp.dp),
            horizontalArrangement = Arrangement.spacedBy(gapDp.dp),
        ) {
            entries.forEachIndexed { index, id ->
                key(id) {
                    PreviewPresetButton(
                        id = id,
                        label = names[id] ?: "Missing preset",
                        chosen = chosen,
                        vertical = false,
                        gapDp = gapDp,
                        textColor = textColor,
                        backgroundColor = if (index == 0) activeColor else widgetColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPresetButton(
    id: Long,
    label: String,
    chosen: SnapshotStateList<Long>,
    vertical: Boolean,
    gapDp: Int,
    textColor: Int,
    backgroundColor: Int,
    modifier: Modifier,
) {
    var dimensionPx by remember { mutableIntStateOf(1) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val spacingPx = with(LocalDensity.current) { gapDp.dp.toPx() }

    Box(
        modifier =
            modifier
                .heightIn(min = 56.dp)
                .onSizeChanged {
                    dimensionPx = if (vertical) it.height else it.width
                }.zIndex(if (dragging) 1f else 0f)
                .graphicsLayer {
                    if (dragging) {
                        if (vertical) translationY = dragPx else translationX = dragPx
                    }
                }.pointerInput(id, vertical, gapDp) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragging = true
                            dragPx = 0f
                        },
                        onDragEnd = {
                            dragging = false
                            dragPx = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            dragPx = 0f
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            dragPx += if (vertical) delta.y else delta.x
                            val extent = dimensionPx.coerceAtLeast(1) + spacingPx
                            if (abs(dragPx) >= extent * 0.55f) {
                                val from = chosen.indexOf(id)
                                val step = if (dragPx > 0) 1 else -1
                                val to = from + step
                                if (from >= 0 && to in chosen.indices) {
                                    chosen.removeAt(from)
                                    chosen.add(to, id)
                                    dragPx -= step * extent
                                } else {
                                    dragPx = 0f
                                }
                            }
                        },
                    )
                }.background(
                    Color(backgroundColor),
                    MaterialTheme.shapes.medium,
                ).padding(vertical = 14.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color(textColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
