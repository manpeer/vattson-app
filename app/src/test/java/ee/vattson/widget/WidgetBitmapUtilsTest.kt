package ee.vattson.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetBitmapUtilsTest {
    @Test
    fun calculateWidgetMemoryCapBytes() {
        assertEquals(6L * 100 * 200, calculateWidgetMemoryCapBytes(100, 200))
    }
}