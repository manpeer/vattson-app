package ee.vattson.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import kotlin.math.cos
import kotlin.math.sin

object DonutChartGenerator {

    fun generateDonutChart(
        prices: FloatArray, 
        sizePx: Int, 
        backgroundColor: Int, 
        textColor: Int,
        cheapestStart: Int = -1,
        cheapestDuration: Int = 0,
        config: Bitmap.Config = Bitmap.Config.RGB_565
    ): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx, config)
        val canvas = Canvas(bitmap)
        val center = sizePx / 2f
        val outerRadius = sizePx / 2f
        val innerRadius = outerRadius * 0.75f

        // 1. Draw opaque background
        canvas.drawColor(backgroundColor)

        if (prices.size != 24) {
            // Draw a simple gray donut if data is missing
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = outerRadius - innerRadius
            }
            canvas.drawCircle(center, center, (outerRadius + innerRadius) / 2f, paint)
            return bitmap
        }

        val minPrice = prices.minOrNull() ?: 0f
        val maxPrice = prices.maxOrNull() ?: 1f
        val avgPrice = prices.average().toFloat()
        val range = if (maxPrice > minPrice) maxPrice - minPrice else 1f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            isFilterBitmap = true
            isDither = true
        }

        val sweepAngle = 360f / 24f
        val rectF = RectF()

        for (i in 0 until 24) {
            val price = prices[i]
            
            // Scaling logic: lowest price at inner ring, highest at outer
            val scaledRadius = innerRadius + (outerRadius - innerRadius) * (price - minPrice) / range
            
            // Color logic
            val isCheapestWindow = cheapestStart != -1 && i >= cheapestStart && i < (cheapestStart + cheapestDuration)
            
            paint.color = when {
                isCheapestWindow -> 0xFF43A047.toInt() // Green (cheapestWindowGreen) for the specific period
                price > avgPrice * 1.2f -> 0xFF0D47A1.toInt() // Dark Blue for peak
                else -> 0xFF2196F3.toInt() // Blue for others (defaultBarBlue)
            }

            // Midnight (Hour 0) is at the top (-90 degrees)
            val startAngle = -90f + (i * sweepAngle)
            
            rectF.set(
                center - scaledRadius,
                center - scaledRadius,
                center + scaledRadius,
                center + scaledRadius
            )
            
            // Use a tiny overlap (0.4 degrees) to eliminate sub-pixel gaps between sectors
            canvas.drawArc(rectF, startAngle, sweepAngle + 0.4f, true, paint)
        }

        // 2. Draw the "donut hole" with background color
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
        }
        canvas.drawCircle(center, center, innerRadius, holePaint)

        // 3. Draw helper rings and ticks
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            style = Paint.Style.STROKE
            strokeWidth = (sizePx * 0.004f).coerceAtLeast(2f) // Scale stroke with resolution
            alpha = 100
            isFilterBitmap = true
            isDither = true
        }
        
        canvas.drawCircle(center, center, innerRadius, linePaint)
        
        for (i in 0 until 24) {
            val angleRad = Math.toRadians((i * 15.0 - 90.0))
            val startX = center + (innerRadius * cos(angleRad)).toFloat()
            val startY = center + (innerRadius * sin(angleRad)).toFloat()
            // Ticks point inwards slightly to separate from sectors
            val tickLen = if (i % 6 == 0) sizePx * 0.03f else sizePx * 0.015f
            val endX = center + ((innerRadius - tickLen) * cos(angleRad)).toFloat()
            val endY = center + ((innerRadius - tickLen) * sin(angleRad)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, linePaint)
        }

        // 4. Draw hour labels inside the hollow part
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            isSubpixelText = true // Improve text rendering quality
            hinting = Paint.HINTING_ON
        }

        for (h in 0 until 24) {
            val isMajor = h % 3 == 0
            textPaint.textSize = if (isMajor) sizePx * 0.058f else sizePx * 0.04f
            textPaint.alpha = if (isMajor) 255 else 180
            textPaint.typeface = if (isMajor) android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD) 
                                 else android.graphics.Typeface.DEFAULT
            
            val angleRad = Math.toRadians((h * 15.0 - 90.0))
            // Place major labels at 0.78, minor labels slightly inward to avoid crowding the ring
            val labelRadius = if (isMajor) innerRadius * 0.78f else innerRadius * 0.82f
            val x = center + (labelRadius * cos(angleRad)).toFloat()
            val y = center + (labelRadius * sin(angleRad)).toFloat()

            val fontMetrics = textPaint.fontMetrics
            val yOffset = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
            
            canvas.drawText(h.toString(), x, y + yOffset, textPaint)
        }

        return bitmap
    }
}
