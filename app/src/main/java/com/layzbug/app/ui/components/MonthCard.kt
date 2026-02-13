package com.layzbug.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.OnSurfaceVariant
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.PrimaryContainer
import com.layzbug.app.ui.theme.SurfaceContainer

@Composable
fun MonthCard(
    monthName: String,
    walkCount: Int,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = if (isEnabled) onClick else {{}},
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(Dimens.radius2xl),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) PrimaryContainer else SurfaceContainer
        ),
        elevation = CardDefaults.cardElevation(Dimens.spaceZero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spaceBase),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = walkCount.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = if (isEnabled) Primary else Primary.copy(alpha = 0.38f), // Material Design disabled alpha
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spaceXs))

            Text(
                text = monthName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isEnabled) Primary else Primary.copy(alpha = 0.38f), // Material Design disabled alpha
                textAlign = TextAlign.Center
            )
        }
    }
}