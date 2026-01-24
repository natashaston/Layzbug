package com.layzbug.app.ui.components

import android.graphics.Matrix
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// Cleaned up shape imports for Kotlin 2.2 compatibility
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.toPath
import com.layzbug.app.domain.StatsValue
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.OnPrimary
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.PrimaryContainer

@Composable
fun StatsCardPill(
    stats: StatsValue,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Define the organic shape
    val expressivePill = remember {
        RoundedPolygon.pill(
            width = 6f,
            height = 5.2f,
            smoothing = 0.98f
        )
    }

    Card(
        onClick = onClick,
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP SPACING
            Spacer(modifier = Modifier.height(20.dp))

            // 2. SHAPE BOX
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawWithCache {
                        val androidPath = expressivePill.toPath()
                        val matrix = Matrix()

                        val targetSize = size.width * 0.82f
                        val scaleFactor = targetSize / 5.2f

                        matrix.setScale(scaleFactor, scaleFactor)
                        matrix.postRotate(135f)
                        matrix.postTranslate(size.width / 2f, size.height / 2f)

                        androidPath.transform(matrix)
                        val composePath = androidPath.asComposePath()

                        onDrawBehind {
                            drawPath(
                                path = composePath,
                                color = Primary
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stats.value.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = OnPrimary
                )
            }

            // 3. MIDDLE SPACING
            Spacer(modifier = Modifier.height(Dimens.spaceBase))

            // 4. TEXT LABEL
            Text(
                text = stats.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.spaceBase)
            )
        }
    }
}