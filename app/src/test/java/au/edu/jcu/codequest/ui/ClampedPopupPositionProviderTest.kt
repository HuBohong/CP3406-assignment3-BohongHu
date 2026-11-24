package au.edu.jcu.codequest.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import au.edu.jcu.codequest.ui.components.ClampedPopupPositionProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class ClampedPopupPositionProviderTest {

    private val provider = ClampedPopupPositionProvider(gap = 10, edgeMargin = 40, bottomReserve = 200)
    private val window = IntSize(1000, 2000)
    private val popup = IntSize(600, 400)

    private fun place(anchor: IntRect) = provider.calculatePosition(anchor, window, LayoutDirection.Ltr, popup)

    @Test
    fun centredUnderAnAnchorInTheMiddle() {
        assertEquals(IntOffset(200, 610), place(IntRect(450, 500, 550, 600)))
    }

    @Test
    fun anchorNearTheLeftEdge_keepsThePopupOnScreen() {
        assertEquals(40, place(IntRect(0, 500, 100, 600)).x)
    }

    @Test
    fun anchorNearTheRightEdge_keepsThePopupOnScreen() {
        assertEquals(1000 - 600 - 40, place(IntRect(900, 500, 1000, 600)).x)
    }

    @Test
    fun noRoomBelowAboveTheBottomBar_flipsAboveTheAnchor() {
        // Below would end at 1500 + 10 + 400 = 1910, past 2000 - 200.
        assertEquals(1400 - 10 - 400, place(IntRect(450, 1400, 550, 1500)).y)
    }
}
