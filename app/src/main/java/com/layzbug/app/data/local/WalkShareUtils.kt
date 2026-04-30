package com.layzbug.app.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.io.File
import java.io.FileOutputStream
import com.layzbug.app.R

// ─── Data model ───────────────────────────────────────────────────────────────

data class ShareCardData(
    val chipLabel: String,
    val col1: ShareMetric?,
    val col2: ShareMetric,
    val col3: ShareMetric,
    val goalBadge: String? = null,
    val fileName: String,
    val shareText: String = "",
    val accentColor: Int = Color.parseColor("#FF00FF66") // Default to Green Accent
)

data class ShareMetric(
    val value: String,
    val label: String
)

// ─── Object ───────────────────────────────────────────────────────────────────

object WalkShareUtils {

    // ── CONFIGURABLE SIZES & SPACERS ──────────────────────────────────────────
    private const val SPACER_CHIP_TO_METRICS_DP = 28f  // Gap between Top Chip and Number metrics
    private const val SPACER_METRIC_TO_LABEL_DP = 4f   // Gap between Number and the first label line
    private const val SPACER_LABEL_LINE_DP      = 2f   // Gap between line 1 and line 2 of the label
    private const val SPACER_LABEL_TO_GOAL_DP   = 24f  // Gap between the bottom label line and Green Goal Pill

    private const val GOAL_PILL_HEIGHT_DP   = 28f
    private const val TOP_CHIP_TEXT_SIZE_DP = 12f  // Text size for the top date chip & branding chip
    private const val GOAL_PILL_TEXT_SIZE_DP= 12f  // Text size for the bottom green pill
    private const val METRIC_NUM_SIZE_DP    = 26f  // Size for the main numbers
    private const val METRIC_LABEL_SIZE_DP  = 12f  // Matched to Top Chip Size

    private const val CARD_W_DP = 360f
    private const val SCALE     = 3f
    private const val PAD_DP    = 16f // Standard padding
    private const val PAD_BOTTOM_NO_GOAL_DP = 32f // Extra bottom padding when no goal pill is present

    // Palette
    private val SURFACE    = Color.parseColor("#FF151619")
    private val BORDER     = Color.argb(13,  255, 255, 255)   // white 5%
    private val GRID_COLOR = Color.argb(8,   255, 255, 255)   // white 3%
    private val TEXT_MUTED = Color.argb(153, 255, 255, 255)   // white 60%
    private val DIVIDER    = Color.argb(13,  255, 255, 255)   // white 5%
    private val CHIP_BG    = Color.argb(8,   255, 255, 255)   // white 3%

    // Goal pill — green tinted
    private val GOAL_PILL_BG     = Color.argb(35,  0, 255, 102)   // green ~14%
    private val GOAL_PILL_BORDER = Color.argb(80,  0, 255, 102)   // green ~31%
    private val GOAL_PILL_TEXT   = Color.argb(230, 0, 255, 102)   // green ~90%

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun renderCardBitmap(context: Context, data: ShareCardData): Bitmap {
        val victorMono: Typeface = withContext(Dispatchers.Main) {
            ResourcesCompat.getFont(context, R.font.victor_mono_regular)
        } ?: Typeface.MONOSPACE
        val jetbrainsMono: Typeface = withContext(Dispatchers.Main) {
            ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular)
        } ?: Typeface.MONOSPACE
        return withContext(Dispatchers.Default) { renderCard(context, data, victorMono, jetbrainsMono) }
    }

    suspend fun shareFromBitmap(context: Context, bitmap: Bitmap, data: ShareCardData) {
        val uri = withContext(Dispatchers.IO) {
            val dir  = File(context.cacheDir, "share_images").also { it.mkdirs() }
            val file = File(dir, "${data.fileName}.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, data.shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share walk").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ── Formatting ───────────────────────────────────────────────────────────

    private fun formatDistance(km: Double): String {
        return if (km >= 1000) {
            "%,.0f".format(km)
        } else if (km >= 10) {
            "${Math.round(km)}"
        } else if (km > 0.0) {
            "%.1f".format(km)
        } else {
            "0"
        }
    }

    // ── Builders ─────────────────────────────────────────────────────────────

    fun dailyCardData(date: LocalDate, durationMins: Long, distanceKm: Double): ShareCardData {
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.uppercase()
        return ShareCardData(
            chipLabel = "$month ${date.dayOfMonth}, ${date.year}",
            col1      = null,
            col2      = ShareMetric(if (distanceKm > 0.0) formatDistance(distanceKm) else "-", "KILOMETRES COVERED"),
            col3      = ShareMetric(if (durationMins > 0) "$durationMins" else "-", "MINUTES WALKED"),
            goalBadge  = if (durationMins >= 30) "30 MINUTE WALKING GOAL HIT" else null,
            fileName   = "layzbug_${date.year}${date.monthNumber.toString().padStart(2,'0')}${date.dayOfMonth.toString().padStart(2,'0')}",
            shareText  = "My walks on ${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year} — Tracked with Layzbug App"
        )
    }

    fun monthlyCardData(year: Int, monthNumber: Int, daysWalked: Int, distanceKm: Double, totalMins: Long): ShareCardData {
        val monthName = java.time.Month.of(monthNumber).name.lowercase().replaceFirstChar { it.uppercase() }.uppercase()
        return ShareCardData(
            chipLabel = "$monthName $year",
            col1      = ShareMetric(if (daysWalked > 0) "$daysWalked" else "-", "DAYS WALKED"),
            col2      = ShareMetric(if (distanceKm > 0.0) formatDistance(distanceKm) else "-", "KILOMETRES COVERED"),
            col3      = ShareMetric(if (totalMins > 0) "$totalMins" else "-", "MINUTES WALKED"),
            goalBadge  = null,
            fileName   = "layzbug_${monthName.lowercase()}_$year",
            shareText  = "My walks in ${monthName.lowercase().replaceFirstChar { it.uppercase() }} $year — Tracked with Layzbug App"
        )
    }

    fun yearlyCardData(year: Int, daysWalked: Int, distanceKm: Double, totalMins: Long): ShareCardData {
        return ShareCardData(
            chipLabel = "$year",
            col1      = ShareMetric(if (daysWalked > 0) "$daysWalked" else "-", "DAYS WALKED"),
            col2      = ShareMetric(if (distanceKm > 0.0) formatDistance(distanceKm) else "-", "KILOMETRES COVERED"),
            col3      = ShareMetric(if (totalMins > 0) "$totalMins" else "-", "MINUTES WALKED"),
            goalBadge  = null,
            fileName   = "layzbug_$year",
            shareText  = "My walks in $year — Tracked with Layzbug App",
            accentColor = Color.parseColor("#FFFF4400") // Orange Accent for Yearly Stats
        )
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun renderCard(context: Context, data: ShareCardData, victorMono: Typeface, jetbrainsMono: Typeface): Bitmap {
        val density = context.resources.displayMetrics.density

        // Enforce minimum effective scale so output is always ≥ 1080px wide
        val effectiveScale = maxOf(SCALE, 1080f / (CARD_W_DP * density))
        val dp = density * effectiveScale
        val pad = PAD_DP * dp

        // Standardised heights from the configurable options above
        val chipH     = 28f * dp
        val goalH     = GOAL_PILL_HEIGHT_DP * dp
        val numSize   = METRIC_NUM_SIZE_DP * dp
        val labelSize = METRIC_LABEL_SIZE_DP * dp

        // Dynamic Height Calculation
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = numSize; typeface = jetbrainsMono }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = labelSize
            typeface = victorMono
            letterSpacing = 0.1f
            color = TEXT_MUTED
        }

        val numH = numPaint.fontMetrics.descent - numPaint.fontMetrics.ascent
        val lblH = labelPaint.fontMetrics.descent - labelPaint.fontMetrics.ascent

        // Two lines for labels
        val labelLineGap = SPACER_LABEL_LINE_DP * dp
        val totalLabelH = (lblH * 2) + labelLineGap

        val metricsBlockH = numH + (SPACER_METRIC_TO_LABEL_DP * dp) + totalLabelH

        val hasGoal = data.goalBadge != null

        // Pad bottom dynamically based on whether there's a goal pill or not
        val bottomPad = if (hasGoal) pad else (PAD_BOTTOM_NO_GOAL_DP * dp)

        val totalCardHeight = pad + chipH + (SPACER_CHIP_TO_METRICS_DP * dp) + metricsBlockH + (if (hasGoal) (SPACER_LABEL_TO_GOAL_DP * dp) + goalH else 0f) + bottomPad

        val W = (CARD_W_DP * dp).toInt()
        val H = totalCardHeight.toInt()

        val bmp    = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        val cardR  = 24f * dp // Matches MonthHero RoundedCornerShape(24.dp)

        // ── 1. Background & Grid ─────────────────────────────────────────
        paint.color = SURFACE
        canvas.drawRoundRect(0f, 0f, W.toFloat(), H.toFloat(), cardR, cardR, paint)

        paint.color       = GRID_COLOR
        paint.strokeWidth = 1f * dp
        val gs = 4f * dp
        var gx = 0f; while (gx <= W) { canvas.drawLine(gx, 0f, gx, H.toFloat(), paint); gx += gs }
        var gy = 0f; while (gy <= H) { canvas.drawLine(0f, gy, W.toFloat(), gy, paint); gy += gs }

        // ── 2. Border ──────────────────────────────────────────────────
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f * dp; paint.color = BORDER
        val bo = paint.strokeWidth / 2f
        canvas.drawRoundRect(bo, bo, W - bo, H - bo, cardR, cardR, paint)
        paint.style = Paint.Style.FILL

        // ── 3. Top row ─────────────────────────────────────────────────
        val chipTopY = pad
        val chipBotY = chipTopY + chipH
        val chipY    = chipTopY + chipH / 2f
        val chipR    = chipH / 2f

        val chipTextSize = TOP_CHIP_TEXT_SIZE_DP * dp
        paint.textSize    = chipTextSize
        paint.typeface    = victorMono
        paint.letterSpacing = 0.1f
        val chipText  = data.chipLabel.uppercase()
        val chipTextW = paint.measureText(chipText)

        val iconSz   = 12f * dp
        val iconGap  = 8f * dp
        val chipPadH = 12f * dp
        val chipW    = chipPadH + iconSz + iconGap + chipTextW + chipPadH
        val chipL    = pad
        val chipRt   = chipL + chipW

        // Date Chip fill + border
        paint.color = CHIP_BG; paint.style = Paint.Style.FILL
        canvas.drawRoundRect(chipL, chipTopY, chipRt, chipBotY, chipR, chipR, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f * dp; paint.color = BORDER
        canvas.drawRoundRect(chipL, chipTopY, chipRt, chipBotY, chipR, chipR, paint)
        paint.style = Paint.Style.FILL

        // Date Calendar icon (Dynamically colored based on accent)
        drawCalendarIcon(canvas, dp, chipL + chipPadH, chipY, iconSz, data.accentColor)

        // Date label
        paint.color = TEXT_MUTED
        val lmDate = paint.fontMetrics
        canvas.drawText(chipText, chipL + chipPadH + iconSz + iconGap, chipY - (lmDate.ascent + lmDate.descent) / 2f, paint)

        // LAYZBUG APP Branding Chip
        paint.typeface = Typeface.create(victorMono, Typeface.BOLD)
        val appText = "LAYZBUG APP"
        val appTextW = paint.measureText(appText)
        val appChipW = chipPadH + appTextW + chipPadH
        val appChipRt = W - pad
        val appChipL = appChipRt - appChipW

        // App Chip fill + border
        paint.color = CHIP_BG; paint.style = Paint.Style.FILL
        canvas.drawRoundRect(appChipL, chipTopY, appChipRt, chipBotY, chipR, chipR, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f * dp; paint.color = BORDER
        canvas.drawRoundRect(appChipL, chipTopY, appChipRt, chipBotY, chipR, chipR, paint)
        paint.style = Paint.Style.FILL

        // App text
        paint.color = Color.WHITE
        val lmApp = paint.fontMetrics
        canvas.drawText(appText, appChipL + chipPadH, chipY - (lmApp.ascent + lmApp.descent) / 2f, paint)
        paint.letterSpacing = 0f

        // ── 4. Metrics Stack ───────────────────────────────────────────
        val metricsTopY = chipBotY + (SPACER_CHIP_TO_METRICS_DP * dp)

        val columns  = listOfNotNull(data.col1, data.col2, data.col3)
        val colCount = columns.size
        val colW     = (W - pad * 2f) / colCount

        columns.forEachIndexed { i, metric ->
            val colCentreX = pad + i * colW + colW / 2f

            val numBaselineY = metricsTopY - numPaint.fontMetrics.ascent
            val numBottomY = numBaselineY + numPaint.fontMetrics.descent
            val firstLabelBaselineY = numBottomY + (SPACER_METRIC_TO_LABEL_DP * dp) - labelPaint.fontMetrics.ascent

            // Draw Number with dynamic accent color
            drawMetricNumber(canvas, dp, numSize, metric.value, colCentreX, numBaselineY, jetbrainsMono, data.accentColor)

            // Draw 2-Line Label
            val words = metric.label.split(" ")
            var currentLabelY = firstLabelBaselineY

            words.forEach { word ->
                val lblW = labelPaint.measureText(word)
                val lblX = colCentreX - lblW / 2f
                canvas.drawText(word, lblX, currentLabelY, labelPaint)
                currentLabelY += lblH + labelLineGap
            }

            // Vertical divider
            if (i < colCount - 1) {
                val divX = pad + (i + 1) * colW
                val divH = 40f * dp
                paint.color = DIVIDER; paint.strokeWidth = 1f * dp
                canvas.drawLine(divX, metricsTopY + (metricsBlockH / 2f) - (divH / 2f), divX, metricsTopY + (metricsBlockH / 2f) + (divH / 2f), paint)
            }
        }

        // ── 5. Full-Width Goal Pill ────────────────────────────────────
        if (hasGoal) {
            val pillTop = metricsTopY + metricsBlockH + (SPACER_LABEL_TO_GOAL_DP * dp)
            drawGoalPill(canvas, dp, pad, pillTop, W - pad, pillTop + goalH, data.goalBadge!!, victorMono)
        }

        return bmp
    }

    // ── Calendar icon ─────────────────────────────────────────────────────────

    private fun drawCalendarIcon(canvas: Canvas, dp: Float, iconLeft: Float, centreY: Float, size: Float, accentColor: Int) {
        val iconTop = centreY - size / 2f
        val iconR   = 1.5f * dp

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor; style = Paint.Style.STROKE
            strokeWidth = 1.2f * dp; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawRoundRect(iconLeft, iconTop, iconLeft + size, iconTop + size, iconR, iconR, stroke)

        val headerH = size * 0.32f
        stroke.strokeWidth = 1f * dp
        canvas.drawLine(iconLeft, iconTop + headerH, iconLeft + size, iconTop + headerH, stroke)

        stroke.strokeWidth = 1.5f * dp
        listOf(0.3f, 0.7f).forEach { fx ->
            val x = iconLeft + size * fx
            canvas.drawLine(x, iconTop - 1.5f * dp, x, iconTop + 2f * dp, stroke)
        }
    }

    // ── Metric number ────────────────────────────────────────────────────────

    private fun drawMetricNumber(
        canvas: Canvas, dp: Float, numSize: Float,
        value: String, centreX: Float, baselineY: Float,
        jetbrainsMono: Typeface, accentColor: Int
    ) {
        // Dynamically compute glow colour (80% opacity, ~204 out of 255)
        val r = Color.red(accentColor)
        val g = Color.green(accentColor)
        val b = Color.blue(accentColor)
        val glowColor = Color.argb(204, r, g, b)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = numSize; typeface = jetbrainsMono; letterSpacing = -0.06f
            color = glowColor
            isFakeBoldText = false
            maskFilter = BlurMaskFilter(8f * dp, BlurMaskFilter.Blur.NORMAL)
        }
        val crispPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = numSize; typeface = jetbrainsMono; letterSpacing = -0.06f
            color = accentColor
            isFakeBoldText = false
        }

        val numW = crispPaint.measureText(value)
        val numX = centreX - numW / 2f

        canvas.drawText(value, numX, baselineY, glowPaint)
        canvas.drawText(value, numX, baselineY, crispPaint)
    }

    // ── Full-Width Goal Pill ─────────────────────────────────────────────────

    private fun drawGoalPill(
        canvas: Canvas, dp: Float,
        left: Float, top: Float, right: Float, bottom: Float,
        label: String,
        victorMono: Typeface
    ) {
        val pillR = (bottom - top) / 2f

        // Fill
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOAL_PILL_BG; style = Paint.Style.FILL }
        canvas.drawRoundRect(left, top, right, bottom, pillR, pillR, p)
        // Border
        p.style = Paint.Style.STROKE; p.strokeWidth = 1f * dp; p.color = GOAL_PILL_BORDER
        canvas.drawRoundRect(left, top, right, bottom, pillR, pillR, p)

        // Text
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize      = GOAL_PILL_TEXT_SIZE_DP * dp
            typeface      = victorMono
            letterSpacing = 0.1f
            color         = GOAL_PILL_TEXT
            textAlign     = Paint.Align.CENTER
        }

        val labelText = label.uppercase()
        val lm = txtPaint.fontMetrics

        val pillCentreX = left + (right - left) / 2f
        val pillCentreY = top + (bottom - top) / 2f
        val textY = pillCentreY - ((lm.ascent + lm.descent) / 2f)

        canvas.drawText(labelText, pillCentreX, textY, txtPaint)
    }
}