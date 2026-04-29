package com.layzbug.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R
import kotlinx.coroutines.launch

private val VictorMonoPreview = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

/**
 * SharePreviewBottomSheet
 *
 * Accepts a pre-rendered [bitmap] so the sheet opens instantly with no stutter.
 * The bitmap is rendered by the call site before showing the sheet.
 *
 * Usage in EditWalkStatusContent:
 *   IconButton(onClick = {
 *       scope.launch {
 *           val bmp = WalkShareUtils.renderCardBitmap(context, cardData)
 *           shareBitmap = bmp
 *           showSharePreview = true
 *       }
 *   })
 *
 *   if (showSharePreview && shareBitmap != null) {
 *       SharePreviewBottomSheet(
 *           bitmap    = shareBitmap!!,
 *           cardData  = cardData,
 *           onDismiss = { showSharePreview = false }
 *       )
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewBottomSheet(
    bitmap: Bitmap,
    cardData: ShareCardData,
    onDismiss: () -> Unit
) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sharing    by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFC9C4D0), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Header chip
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .background(Color(0xFF151619), CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF66))
                    )
                    Text(
                        text          = "SHARE PREVIEW",
                        color         = Color.White.copy(alpha = 0.6f),
                        fontSize      = 11.sp,
                        fontFamily    = VictorMonoPreview,
                        letterSpacing = 1.1.sp
                    )
                }
            }

            // Card preview — bitmap is already ready, renders instantly
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "Walk share card preview",
                contentScale       = ContentScale.FillWidth,
                modifier           = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            )

            // Share button
            Button(
                onClick = {
                    sharing = true
                    scope.launch {
                        WalkShareUtils.shareFromBitmap(context, bitmap, cardData)
                        onDismiss()
                    }
                },
                enabled  = !sharing,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF151619),
                    disabledContainerColor = Color(0xFF151619).copy(alpha = 0.5f)
                )
            ) {
                if (sharing) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Outlined.Share,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text          = "Share",
                        color         = Color.White,
                        fontSize      = 15.sp,
                        fontFamily    = VictorMonoPreview,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}