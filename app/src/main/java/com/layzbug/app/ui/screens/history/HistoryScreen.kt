package com.layzbug.app.ui.screens.history

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.layzbug.app.R
import com.layzbug.app.ui.components.MonthCard
import com.layzbug.app.ui.components.SharePreviewBottomSheet
import com.layzbug.app.ui.components.WalkShareUtils
import com.layzbug.app.ui.components.YearlyStatsWithDropdown
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import kotlinx.coroutines.launch

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onNavigateToMonth: (year: Int, month: Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
    gridHorizontalSpacing: Dp = Dimens.spaceXs,
    gridVerticalSpacing: Dp = Dimens.spaceXs
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val monthsStats by viewModel.monthsStats.collectAsState()
    val yearTotal by viewModel.yearTotal.collectAsState()
    val yearDistanceKm by viewModel.yearDistanceKm.collectAsState()
    val yearMinutes by viewModel.yearMinutes.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Share Sheet State
    var showSharePreview by remember { mutableStateOf(false) }
    var shareBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Share Preview Bottom Sheet
    val bmp = shareBitmap
    if (showSharePreview && bmp != null) {
        SharePreviewBottomSheet(
            bitmap = bmp,
            cardData = WalkShareUtils.yearlyCardData(
                year = selectedYear,
                daysWalked = yearTotal,
                distanceKm = yearDistanceKm,
                totalMins = yearMinutes
            ),
            onDismiss = {
                showSharePreview = false
                shareBitmap = null
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceColor)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(gridHorizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Yearly Stats
        item {
            YearlyStatsWithDropdown(
                totalWalks = yearTotal,
                totalDistanceKm = yearDistanceKm,
                totalMinutes = yearMinutes,
                selectedYear = selectedYear,
                availableYears = availableYears,
                onYearSelected = { year -> viewModel.setSelectedYear(year) },
                onShareClick = {
                    scope.launch {
                        val cardData = WalkShareUtils.yearlyCardData(
                            year = selectedYear,
                            daysWalked = yearTotal,
                            distanceKm = yearDistanceKm,
                            totalMins = yearMinutes
                        )
                        shareBitmap = WalkShareUtils.renderCardBitmap(context, cardData)
                        showSharePreview = true
                    }
                },
                modifier = Modifier
            )
        }

        // 2. Spacer
        item {
            Spacer(modifier = Modifier.height(Dimens.spaceXs))
        }

        // 3. Section Divider
        item {
            SectionDivider(title = "Monthly Breakdown")
        }

        // 4. Spacer
        item {
            Spacer(modifier = Modifier.height(0.dp))
        }

        // 5. Month cards
        items(monthsStats) { monthStat ->
            MonthCard(
                monthName = getFullMonthName(monthStat.monthName),
                walkCount = monthStat.walkCount,
                distanceKm = monthStat.distanceKm,
                totalMinutes = monthStat.totalMinutes,
                isEnabled = monthStat.isEnabled,
                isCurrentMonth = monthStat.year == java.time.LocalDate.now().year && monthStat.month == java.time.LocalDate.now().monthValue,
                onClick = {
                    if (monthStat.isEnabled) {
                        onNavigateToMonth(monthStat.year, monthStat.month)
                    }
                }
            )
        }

        // 6. Bottom padding
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun getFullMonthName(shortName: String): String {
    return when (shortName.uppercase()) {
        "JAN" -> "January"
        "FEB" -> "February"
        "MAR" -> "March"
        "APR" -> "April"
        "MAY" -> "May"
        "JUN" -> "June"
        "JUL" -> "July"
        "AUG" -> "August"
        "SEP" -> "September"
        "OCT" -> "October"
        "NOV" -> "November"
        "DEC" -> "December"
        else -> shortName
    }
}

@Composable
fun SectionDivider(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp)
            .alpha(0.3f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            color = Color.Black
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
    }
}