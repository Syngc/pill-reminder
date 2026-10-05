package com.pillreminder.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Outline icons from the redesign (Lucide shapes), tinted by `Icon`. */
object AppIcons {
    private fun icon(name: String, strokeWidth: Float = 2.2f, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private const val CIRCLE = "M22 12a10 10 0 1 1-20 0 10 10 0 1 1 20 0"

    val Capsule = icon("Capsule", 2f, "M10.5 20.5 3.5 13.5a4.95 4.95 0 0 1 7-7l7 7a4.95 4.95 0 0 1-7 7Z", "m8.5 8.5 7 7")
    val Tablet = icon("Tablet", 2f, "M21 12a9 9 0 1 1-18 0 9 9 0 1 1 18 0", "M3 12h18")
    val Bell = icon("Bell", 2.2f, "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val Check = icon("Check", 3f, "M20 6 9 17l-5-5")
    val Speaker = icon("Speaker", 2f, "M11 5 6 9H2v6h4l5 4V5z", "M15.54 8.46a5 5 0 0 1 0 7.07", "M19.07 4.93a10 10 0 0 1 0 14.14")
    val Clock = icon("Clock", 2.2f, CIRCLE, "M12 6v6l4 2")
    val Alert = icon("Alert", 2.4f, CIRCLE, "M12 8v4", "M12 16h.01")
    val Info = icon("Info", 2.2f, CIRCLE, "M12 16v-4", "M12 8h.01")
    val Calendar = icon(
        "Calendar", 2.2f,
        "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M16 2v4", "M8 2v4", "M3 10h18",
    )
    val Plus = icon("Plus", 2.4f, "M12 5v14", "M5 12h14")
    val List = icon("List", 2f, "M8 6h13", "M8 12h13", "M8 18h13", "M3 6h.01", "M3 12h.01", "M3 18h.01")
    val Back = icon("Back", 2.4f, "m15 18-6-6 6-6")
    val Forward = icon("Forward", 2.4f, "m9 18 6-6-6-6")
    val Globe = icon(
        "Globe", 2f, CIRCLE, "M2 12h20",
        "M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z",
    )
    val Camera = icon(
        "Camera", 2.2f,
        "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z",
        "M15 13a3 3 0 1 1-6 0 3 3 0 1 1 6 0",
    )
    val Gallery = icon(
        "Gallery", 2f,
        "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M11 9a2 2 0 1 1-4 0 2 2 0 1 1 4 0", "m21 15-3.1-3.1a2 2 0 0 0-2.8 0L6 21",
    )
    val Pencil = icon("Pencil", 2f, "M12 20h9", "M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z")
    val Search = icon("Search", 2.2f, "M19 11a8 8 0 1 1-16 0 8 8 0 1 1 16 0", "m21 21-4.3-4.3")
    val Close = icon("Close", 2.4f, "M18 6 6 18", "m6 6 12 12")
    val Trash = icon(
        "Trash", 2.2f,
        "M3 6h18", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
    )
}
