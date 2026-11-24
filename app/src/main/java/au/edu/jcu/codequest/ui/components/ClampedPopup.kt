package au.edu.jcu.codequest.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Places a popup centred under its anchor, but never off screen: it is pushed sideways to stay
 * [edgeMargin] px from the window edges, and flips above the anchor when there is not enough
 * room below (keeping [bottomReserve] px free for the navigation bar).
 */
class ClampedPopupPositionProvider(
    private val gap: Int,
    private val edgeMargin: Int,
    private val bottomReserve: Int,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val centredX = anchorBounds.center.x - popupContentSize.width / 2
        val maxX = (windowSize.width - popupContentSize.width - edgeMargin).coerceAtLeast(edgeMargin)
        val x = centredX.coerceIn(edgeMargin, maxX)

        val below = anchorBounds.bottom + gap
        val fitsBelow = below + popupContentSize.height <= windowSize.height - bottomReserve
        val above = anchorBounds.top - gap - popupContentSize.height
        val y = when {
            fitsBelow -> below
            above >= edgeMargin -> above
            else -> (windowSize.height - bottomReserve - popupContentSize.height).coerceAtLeast(edgeMargin)
        }
        return IntOffset(x, y)
    }
}
