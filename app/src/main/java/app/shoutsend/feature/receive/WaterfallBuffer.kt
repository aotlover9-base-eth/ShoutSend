package app.shoutsend.feature.receive

import android.graphics.Bitmap
import app.shoutsend.core.designsystem.colormap.ColormapFactory

/**
 * Preallocated ring buffer and backing Bitmap for high-performance,
 * zero-allocation streaming acoustic spectrogram waterfall rendering.
 *
 * Conforms to Rule 22: Allocates bitmap and pixel buffers once at construction.
 * Ingests 256-bin spectrum rows at ~30-60 fps via [pushRow] with zero heap allocations.
 *
 * @param width Frequency resolution (number of spectral display bins).
 * @param height Time resolution (number of historical temporal rows, e.g. 120 for 4s @ 30fps).
 */
class WaterfallBuffer(
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT
) {
    val pixelBuffer: IntArray = IntArray(width * height)
    val latestRow: FloatArray = FloatArray(width)

    /**
     * Lazy backing bitmap. Null in headless JVM unit tests.
     */
    val bitmap: Bitmap? by lazy {
        try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            null
        }
    }

    @Volatile
    private var colormapLut: IntArray = IntArray(ColormapFactory.COLORMAP_SIZE)

    /**
     * Updates the active 256-entry ARGB color lookup table.
     */
    fun updateColormap(lut: IntArray) {
        if (lut.size == ColormapFactory.COLORMAP_SIZE) {
            colormapLut = lut
        }
    }

    /**
     * Ingests a normalized spectral power row ([0.0f, 1.0f]).
     * Shifts historical rows downward and maps the newest row into row 0.
     * Thread-safe and zero heap allocations.
     */
    @Synchronized
    fun pushRow(row: FloatArray) {
        if (row.isEmpty()) return
        val count = minOf(width, row.size)

        // Save latest row values for accessible bar-chart fallback
        System.arraycopy(row, 0, latestRow, 0, count)

        // Shift rows down by 1 row (temporal progression from top to bottom)
        System.arraycopy(pixelBuffer, 0, pixelBuffer, width, (height - 1) * width)

        // Map row 0 to colormap
        val lut = colormapLut
        if (lut.isNotEmpty()) {
            for (i in 0 until count) {
                val level = (row[i] * 255.0f + 0.5f).toInt().coerceIn(0, 255)
                pixelBuffer[i] = lut[level]
            }
        }

        // Flush updated pixels into backing bitmap
        bitmap?.setPixels(pixelBuffer, 0, width, 0, 0, width, height)
    }

    /**
     * Resets the entire buffer to baseline silence.
     */
    @Synchronized
    fun clear() {
        val floorColor = if (colormapLut.isNotEmpty()) colormapLut[0] else 0
        pixelBuffer.fill(floorColor)
        latestRow.fill(0.0f)
        bitmap?.setPixels(pixelBuffer, 0, width, 0, 0, width, height)
    }

    companion object {
        const val DEFAULT_WIDTH: Int = 256
        const val DEFAULT_HEIGHT: Int = 120
    }
}
