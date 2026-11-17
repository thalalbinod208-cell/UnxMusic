package com.maxrave.simpmusic.ui.component


import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.SimpIcons
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalAppBar(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    leftIcon: @Composable (() -> Unit)? = null,
    rightIcon: @Composable (RowScope.() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = leftIcon ?: {},
        actions = {
            if (rightIcon != null) {
                rightIcon.invoke(this)
            } else if (leftIcon != null) {
                CenterAppBarBalance()
            }
        },
        // Every other screen in the app builds its TopAppBar with a transparent container and
        // onBackground content. Leaving `colors` unset here fell back to the Material default
        // (surface container + onSurface content), which is why MoodScreen — the only caller —
        // showed a light bar with black text on top of the app's dark background.
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
    )
}

/** Balances the back-button slot so an app-bar title stays centered in the full screen width. */
@Composable
fun CenterAppBarBalance() {
    Spacer(Modifier.width(42.dp))
}

@Preview
@Composable
fun NormalAppBarPreview() {
    NormalAppBar(
        title = {
            Text(
                textAlign = TextAlign.Center,text = "Title")
        },
        leftIcon = {
            IconButton(onClick = { }) {
                Icon(
                    SimpIcons.ArrowBackIosNew,
                    contentDescription = "Back",
                )
            }
        },
        rightIcon = {
            IconButton(onClick = { }) {
                Icon(
                    SimpIcons.MoreVert,
                    contentDescription = "Back",
                )
            }
        },
    )
}
