package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CenterLoadingBox(modifier: Modifier) {
    Box(modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp).align(Alignment.Center),
            strokeWidth = 3.dp,
        )
    }
}
