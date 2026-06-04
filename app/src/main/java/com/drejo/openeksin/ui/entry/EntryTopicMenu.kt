package com.drejo.openeksin.ui.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Bottom sheet mirroring the original entry-list topic menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryTopicMenuSheet(
    title: String,
    isLoggedIn: Boolean,
    isTracked: Boolean,
    hasDraft: Boolean,
    showAllVisible: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onWatch: () -> Unit,
    onWrite: () -> Unit,
    onFindInTopic: () -> Unit,
    onShowAll: () -> Unit,
    onSukelaAll: () -> Unit,
    onSukelaToday: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            MenuRow(Icons.Default.Share, "paylaş", onClick = onShare)
            if (isLoggedIn) {
                MenuRow(
                    icon = if (isTracked) Icons.Default.Star else Icons.Default.StarBorder,
                    label = if (isTracked) "takibi bırak" else "takip et",
                    iconTint = if (isTracked) Color(0xFF38B0DE) else Color.Unspecified,
                    onClick = onWatch,
                )
                MenuRow(
                    icon = Icons.Default.Edit,
                    label = "yaz",
                    iconTint = if (hasDraft) Color(0xFF38B0DE) else Color.Unspecified,
                    onClick = onWrite,
                )
                MenuRow(Icons.Default.Search, "başlıkta ara", onClick = onFindInTopic)
            }
            if (showAllVisible) {
                MenuRow(Icons.Default.MoreHoriz, "tümünü göster", onClick = onShowAll)
            }
            MenuRow(Icons.Default.AttachMoney, "şükela: tümü", onClick = onSukelaAll)
            MenuRow(Icons.Default.MonetizationOn, "şükela: bugün", onClick = onSukelaToday)
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
            tint = iconTint,
        )
        Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider()
}
