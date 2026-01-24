package com.layzbug.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.layzbug.app.R
import com.layzbug.app.ui.navigation.Routes
import com.layzbug.app.ui.theme.Dimens

@Composable
fun PermissionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6E1E9))
            .padding(Dimens.spaceLg),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(Dimens.spaceXl5))

        // Sloth emoji/image placeholder
        Image(
            painter = painterResource(id = R.drawable.sloth_mascot_icn),
            contentDescription = "Sloth mascot",
            modifier = Modifier
                .size(102.dp)
                .padding(bottom = Dimens.spaceBase)
        )

        // App name
        Text(
            text = "Layzbug",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF1C1B20)
        )

        Spacer(modifier = Modifier.height(Dimens.spaceXl3))

        // Feature cards
        FeatureCard(
            icon = "🚶",
            title = "Log your walks",
            description = "Automatically log your 30 minute walk as exercise sessions",
            backgroundColor = Color(0xFFE9DDFF)
        )

        Spacer(modifier = Modifier.height(Dimens.spaceBase))

        FeatureCard(
            icon = "💜",
            title = "Sync with Google Fit",
            description = "No buttons to press or no manual entries to log your walks",
            backgroundColor = Color(0xFFE9DDFF)
        )

        Spacer(modifier = Modifier.height(Dimens.spaceBase))

        FeatureCard(
            icon = "📅",
            title = "View History",
            description = "See how many days you have walked over a period of time",
            backgroundColor = Color(0xFFE9DDFF)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Next button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Permission.route) { inclusive = true }
                    }
                },
                containerColor = Color(0xFF65558F),
                contentColor = Color.White,
                modifier = Modifier
                    .size(width = 220.dp, height = 80.dp)
                    .padding(bottom = Dimens.spaceLg),
                shape = RoundedCornerShape(Dimens.radius7xl)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spaceXl))
    }
}

@Composable
fun FeatureCard(
    icon: String,
    title: String,
    description: String,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(Dimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spaceBase),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF1C1B20)
                )
                Spacer(modifier = Modifier.height(Dimens.spaceXs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF48454E)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spaceBase))

            // Icon container
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(Dimens.radiusLg)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp)
                )
            }
        }
    }
}
