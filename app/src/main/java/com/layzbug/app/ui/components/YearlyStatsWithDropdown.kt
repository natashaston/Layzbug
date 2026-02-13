package com.layzbug.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.PrimaryContainer
import com.layzbug.app.ui.theme.OnPrimary

@Composable
fun YearlyStatsWithDropdown(
    totalWalks: Int,
    selectedYear: Int,
    availableYears: List<Int>,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.size80 + Dimens.size80),
        shape = RoundedCornerShape(Dimens.radius5xl),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = Dimens.spaceXl,
                    bottom = Dimens.spaceXl,
                    end = Dimens.spaceSm,
                    top = Dimens.spaceSm
                )
        ) {

            // ─────────────────────────────
            // Left Side — Stats
            // ─────────────────────────────
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {

                Text(
                    text = totalWalks.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Primary
                )

                Spacer(modifier = Modifier.height(Dimens.spaceXxs))

                Text(
                    text = "Walks in $selectedYear",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Primary,
                    textAlign = TextAlign.Start
                )
            }

            // ─────────────────────────────
            // Top Right — Year Dropdown Trigger
            // ─────────────────────────────
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {

                FilledTonalButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary
                    ),
                    contentPadding = PaddingValues(
                        horizontal = Dimens.spaceBase,
                        vertical = Dimens.spaceXs
                    )
                ) {

                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.width(Dimens.spaceXxs))

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Year",
                        modifier = Modifier.size(Dimens.sizeXl)
                    )
                }

                // ─────────────────────────────
                // Expressive Styled Dropdown Menu
                // (Stable Material3)
                // ─────────────────────────────
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(Dimens.radius2xl),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {

                    availableYears.forEach { year ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            interactionSource = interactionSource,
                            onClick = {
                                onYearSelected(year)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
