package com.layzbug.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reusable Layzbug bottom sheet built on Material3's ModalBottomSheet.
 * Inherits native swipe-to-dismiss, scrim, and animation behavior.
 *
 * Usage:
 * if (showSheet) {
 *     LayzbugBottomSheet(
 *         onClose = { showSheet = false },
 *         lightBackground = true,
 *         showDragHandle = false  // optional, default true
 *     ) {
 *         // your content here
 *     }
 * }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayzbugBottomSheet(
    onClose: () -> Unit,
    lightBackground: Boolean = true,
    showDragHandle: Boolean = true,
    content: @Composable () -> Unit
) {
    val sheetBg = if (lightBackground) Color.White else Color(0xFF151619)
    val handleColor = if (lightBackground) Color(0xFFC9C4D0) else Color.White.copy(alpha = 0.2f)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = if (showDragHandle) {
            {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(handleColor, RoundedCornerShape(2.dp))
                )
            }
        } else {
            { Box(modifier = Modifier.height(0.dp)) }
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}