/*
 * IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.
 * Copyright (C) 2026 onlymash
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fiepi.media.app.ui.screen.browser.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.components.BrowserItemPlaceholder
import com.fiepi.media.app.ui.components.rememberShimmerBrush

/**
 * A shimmer-animated skeleton list used as a placeholder during the initial loading of media files.
 */
@Composable
fun MediaFileSkeletonList(
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    val shimmerBrush = rememberShimmerBrush()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + bottomContentPadding)
    ) {
        // Display 10 placeholder skeleton items matching BrowserItem layout
        items(10) {
            BrowserItemPlaceholder(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                brush = shimmerBrush
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaFileSkeletonListPreview() {
    MaterialTheme {
        MediaFileSkeletonList()
    }
}
