package au.edu.jcu.codequest.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * A few small icons drawn for this app, so the build does not need the very large
 * material-icons-extended library. All are 24 x 24 and tinted by [androidx.compose.material3.Icon].
 */
object CodeQuestIcons {

    /** XP. */
    val Bolt: ImageVector by lazy {
        icon("Bolt") { fill("M13.5 2L4.5 13.5h6.5L9.5 22l10-12h-6.6z") }
    }

    /** Lessons completed. */
    val Book: ImageVector by lazy {
        icon("Book") {
            fill("M4 5.5C4 4.7 4.7 4 5.5 4H11v16H5.5A1.5 1.5 0 0 1 4 18.5z")
            fill("M20 5.5C20 4.7 19.3 4 18.5 4H13v16h5.5c0.8 0 1.5-0.7 1.5-1.5z")
        }
    }

    /** Bonus round. */
    val Gift: ImageVector by lazy {
        icon("Gift") {
            fill("M3.5 10h7.5v10.5H6a2.5 2.5 0 0 1-2.5-2.5z")
            fill("M13 10h7.5v8a2.5 2.5 0 0 1-2.5 2.5H13z")
            fill("M2.5 6.5h8.5v2.5H2.5z")
            fill("M13 6.5h8.5v2.5H13z")
            stroke("M12 6.5C10 3 6.5 3.5 7.5 6.5M12 6.5C14 3 17.5 3.5 16.5 6.5", width = 2f)
        }
    }

    /** "Learn" tab: three stops on a winding path. */
    val Path: ImageVector by lazy {
        icon("Path") {
            fill("M7 3a3 3 0 1 1 0 6a3 3 0 1 1 0-6z")
            fill("M17 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z")
            fill("M7 15a3 3 0 1 1 0 6a3 3 0 1 1 0-6z")
            stroke("M9 7.5l5.5 3M14.5 13.5L9 16.5", width = 2f)
        }
    }

    /** "Progress" tab. */
    val Chart: ImageVector by lazy {
        icon("Chart") {
            fill("M5.5 12h1a1.5 1.5 0 0 1 1.5 1.5v5a1.5 1.5 0 0 1-1.5 1.5h-1A1.5 1.5 0 0 1 4 18.5v-5A1.5 1.5 0 0 1 5.5 12z")
            fill("M11.5 7h1A1.5 1.5 0 0 1 14 8.5v10a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-10A1.5 1.5 0 0 1 11.5 7z")
            fill("M17.5 3h1A1.5 1.5 0 0 1 20 4.5v14a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-14A1.5 1.5 0 0 1 17.5 3z")
        }
    }

    /** Missions and goals. */
    val Flag: ImageVector by lazy {
        icon("Flag") {
            stroke("M6 21V4M6 4h11l-2.5 4 2.5 4H6", width = 2.4f)
        }
    }

    /** Crystals collected in a grid mission. */
    val Gem: ImageVector by lazy {
        icon("Gem") { fill("M7 3h10l4.5 6L12 21.5 2.5 9z") }
    }

    /** Shields left in a boss fight. */
    val Shield: ImageVector by lazy {
        icon("Shield") { fill("M12 2.5l8 3v6c0 5-3.4 9.1-8 10.5C7.4 20.6 4 16.5 4 11.5v-6z") }
    }

    /** Rocket flights between planets. */
    val Rocket: ImageVector by lazy {
        icon("Rocket") {
            fill("M12 2c3 2.2 4.6 5.6 4.6 9.6v4.4H7.4v-4.4C7.4 7.6 9 4.2 12 2z")
            fill("M7.4 12.5L4 16.5v3.5l3.4-2z")
            fill("M16.6 12.5l3.4 4v3.5l-3.4-2z")
            fill("M9.5 17.5h5L12 22z")
        }
    }

    /** Boss fights: a target. */
    val Target: ImageVector by lazy {
        icon("Target") {
            stroke("M12 4a8 8 0 1 1 0 16a8 8 0 1 1 0-16z", width = 2.4f)
            fill("M12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z")
            stroke("M12 1.5v4M12 18.5v4M1.5 12h4M18.5 12h4", width = 2.4f)
        }
    }

    private class Builder(val builder: ImageVector.Builder) {
        fun fill(path: String) {
            builder.addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        }

        fun stroke(path: String, width: Float) {
            builder.addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

    private fun icon(name: String, block: Builder.() -> Unit): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        Builder(builder).block()
        return builder.build()
    }
}
