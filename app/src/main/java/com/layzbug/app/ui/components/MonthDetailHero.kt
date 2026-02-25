package com.layzbug.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.layzbug.app.domain.StatsValue
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.PrimaryContainer

@Composable
fun MonthHeroPill(
    stats: StatsValue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
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
                text = stats.value.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = Primary
            )

            Spacer(modifier = Modifier.height(Dimens.spaceBase))

            Text(
                text = stats.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}