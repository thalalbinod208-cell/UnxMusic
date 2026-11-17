package com.maxrave.simpmusic.ui.screen.other

import com.maxrave.simpmusic.ui.component.CenterAppBarBalance
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.domain.extension.now
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val hazeState = rememberHazeState()
    val scrollState = rememberScrollState()
    val legalDisclaimer by produceState<String?>(null) {
        value = Res.readBytes("files/legal_disclaimer.md").decodeToString()
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(paddingValues)
            .padding(top = 64.dp)
            .verticalScroll(scrollState)
            .hazeSource(state = hazeState)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)),
                )
                Text(stringResource(Res.string.app_name), style = typo().headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Text(stringResource(Res.string.version_format, VersionManager.getVersionName()), style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Developed & Maintained by Binod Thalal", style = typo().bodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("About Unx Music", style = typo().titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Unx Music is a modern music browsing and playback application designed to provide users with a convenient and enjoyable music experience.\n\nThe application is maintained and developed by Binod Thalal under the Unx Music brand.\n\nOur goal is to deliver a clean, user-friendly and reliable music experience with a focus on simplicity, performance and accessibility.",
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Company and support", style = typo().titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Unx Music · Binod Thalal", style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Company: Unx Music", style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Developer: Binod Thalal", style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Support email  ·  hii.binodthalal@gmail.com", style = typo().bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().clickable { openUrl("mailto:hii.binodthalal@gmail.com") })
                Text("Phone  ·  +977 9768914027", style = typo().bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().clickable { openUrl("tel:+9779768914027") })
                Text("WhatsApp  ·  +977 9768914027", style = typo().bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().clickable { openUrl("https://wa.me/9779768914027") })
                Text("Deelasaini 6, Baitadi, Nepal", style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        legalDisclaimer?.let { disclaimer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("Legal and open-source notices", style = typo().titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Markdown(
                        disclaimer,
                        typography = markdownTypography(
                            h2 = typo().titleMedium,
                            h3 = typo().labelSmall,
                            text = typo().bodyMedium,
                            paragraph = typo().bodyMedium,
                            bullet = typo().bodyMedium,
                            textLink = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Text(
            text = "Unx Music · modified open-source derivative · GPL-3.0",
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
        )
    }

    VerticalScrollBar(
        scrollState = scrollState,
        modifier = Modifier.padding(top = paddingValues.calculateTopPadding() + 64.dp, bottom = DesktopMiniPlayerClearance),
    )
    CenterAlignedTopAppBar(
        modifier = Modifier.hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                style = typo().titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(SimpIcons.ArrowBackIosNew, Modifier.size(32.dp), true, tint = MaterialTheme.colorScheme.onSurface) {
                    navController.navigateUp()
                }
            }
        },
actions = { CenterAppBarBalance() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            scrolledContainerColor = androidx.compose.ui.graphics.Color.Unspecified,
            navigationIconContentColor = androidx.compose.ui.graphics.Color.Unspecified,
            titleContentColor = androidx.compose.ui.graphics.Color.Unspecified,
            actionIconContentColor = androidx.compose.ui.graphics.Color.Unspecified,
        ),
    )
}
