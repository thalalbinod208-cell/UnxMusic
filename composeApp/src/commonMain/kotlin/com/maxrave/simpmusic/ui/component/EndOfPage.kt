package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Small end inset so the final item clears the persistent player and navigation controls. */
@Composable
fun EndOfPage(withoutCredit: Boolean = false, height: Dp = 24.dp) {
    Box(
        modifier = Modifier.fillMaxWidth().height(height),
        contentAlignment = Alignment.TopCenter,
    ) {}
}
