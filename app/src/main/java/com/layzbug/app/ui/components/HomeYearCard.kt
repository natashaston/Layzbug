package com.layzbug.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.PrimaryContainer

@Composable
fun StatsCard(
    number: String,
    label: String,
    distanceKm: Double = 0.0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(186.dp)
            .height(238.dp),
        shape = RoundedCornerShape(Dimens.radius5xl),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = number,
                style = MaterialTheme.typography.displayLarge,
                color = Primary
            )

            Spacer(modifier = Modifier.height(Dimens.spaceXxs))

            Text(
                text = formatDistance(distanceKm),
                style = MaterialTheme.typography.bodyMedium,
                color = Primary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spaceBase))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** Format distance: show "12.3 km" or "0 km" */
internal fun formatDistance(km: Double): String {
    return if (km >= 10) {
        "${Math.round(km)} km"
    } else if (km >= 0.1) {
        "${"%.1f".format(km)} km"
    } else {
        "0 km"
    }
}