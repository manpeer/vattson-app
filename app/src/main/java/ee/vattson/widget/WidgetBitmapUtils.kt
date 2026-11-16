package ee.vattson.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import java.io.File
import kotlin.math.max

fun calculateWidgetMemoryCapBytes(width: Int, height: Int): Long {
    return 6L * width.toLong() * height.toLong()
}

fun calculateWidgetMemoryCapBytes(context: Context): Long {
    val bounds = context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds
    if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
        return calculateWidgetMemoryCapBytes(bounds.width(), bounds.height())
    }
    val dm = context.resources.displayMetrics
    return calculateWidgetMemoryCapBytes(dm.widthPixels, dm.heightPixels)
}

fun decodeDownsampledWidgetBitmap(
    context: Context,
    sourceFile: File,
    distinctImageCount: Int = 1,
    maxWidthDp: Int = 240,
    maxHeightDp: Int = 240,
    config: Bitmap.Config = Bitmap.Config.RGB_565,
): Bitmap? {
    if (!sourceFile.exists()) return null
    val density = context.resources.displayMetrics.density
    val targetWidthPx = max(1, (maxWidthDp * density).toInt())
    val targetHeightPx = max(1, (maxHeightDp * density).toInt())
    val requestedBytesPerPixel = when (config) {
        Bitmap.Config.ALPHA_8 -> 1L
        Bitmap.Config.RGB_565 -> 2L
        else -> 4L
    }
    val perImageCapBytes = calculateWidgetMemoryCapBytes(context) / max(1, distinctImageCount)
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inPreferredConfig = config
    }
    BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
    val bytesPerPixel = if (bounds.outConfig != null && bounds.outConfig != config) {
        4L
    } else {
        requestedBytesPerPixel
    }
    var sampleSize = 1
    while (
        (bounds.outWidth / (sampleSize * 2) >= targetWidthPx &&
            bounds.outHeight / (sampleSize * 2) >= targetHeightPx) ||
            (bounds.outWidth / sampleSize > targetWidthPx * 2) ||
            (bounds.outHeight / sampleSize > targetHeightPx * 2)
    ) {
        sampleSize *= 2
    }
    while ((max(1, bounds.outWidth / sampleSize).toLong() * max(1, bounds.outHeight / sampleSize).toLong() * bytesPerPixel) > perImageCapBytes) {
        sampleSize *= 2
    }
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = config
    }
    return BitmapFactory.decodeFile(sourceFile.absolutePath, opts)
}

