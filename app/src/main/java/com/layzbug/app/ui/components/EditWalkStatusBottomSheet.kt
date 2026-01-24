package com.layzbug.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWalkStatusBottomSheet(
    isVisible: Boolean,
    dateLabel: String,
    currentStatus: Boolean = false,
    onWalked: () -> Unit,
    onNotWalked: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember(currentStatus) { mutableStateOf(currentStatus) }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            shape = RoundedCornerShape(
                topStart = Dimens.radius2xl,
                topEnd = Dimens.radius2xl
            ),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = Dimens.spaceBase)
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = Color(0xFFC9C4D0),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.spaceLg,
                        end = Dimens.spaceLg,
                        bottom = Dimens.spaceXl3
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Date title
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF1C1B20),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = Dimens.spaceBase)
                )

                // Status text
                Text(
                    text = if (selectedStatus) "Currently marked as walked" else "Currently marked as not walked",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF48454E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = Dimens.spaceLg)
                )

                // Button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceBase)
                ) {
                    // Walked button
                    Button(
                        onClick = {
                            selectedStatus = true
                            onWalked()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedStatus) Color(0xFF65558F) else Color(0xFFE9DDFF),
                            contentColor = if (selectedStatus) Color.White else Color(0xFF65558F)
                        ),
                        shape = RoundedCornerShape(Dimens.radius7xl)
                    ) {
                        Text(
                            text = "Walked",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Not walked button
                    Button(
                        onClick = {
                            selectedStatus = false
                            onNotWalked()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!selectedStatus) Color(0xFF65558F) else Color(0xFFE9DDFF),
                            contentColor = if (!selectedStatus) Color.White else Color(0xFF65558F)
                        ),
                        shape = RoundedCornerShape(Dimens.radius7xl)
                    ) {
                        Text(
                            text = "Not walked",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
