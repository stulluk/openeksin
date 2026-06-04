package com.drejo.openeksin.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drejo.openeksin.data.Feed
import com.drejo.openeksin.ui.theme.EksiPalette

/**
 * Compact feed tabs matching Ekşin's [androidx.viewpager.widget.PagerTabStrip]:
 * 4dp vertical padding, ~14sp labels, 3dp bottom indicator.
 */
@Composable
fun FeedTabRow(
    feeds: List<Feed>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(EksiPalette.TabBar)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.Bottom,
    ) {
        feeds.forEachIndexed { index, feed ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 12.dp)
                    .height(32.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = feed.title,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) EksiPalette.TabSelected else EksiPalette.TabUnselected,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, bottom = if (selected) 2.dp else 5.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            if (selected) EksiPalette.TabSelected
                            else EksiPalette.TabBar,
                        ),
                )
            }
        }
    }
}
