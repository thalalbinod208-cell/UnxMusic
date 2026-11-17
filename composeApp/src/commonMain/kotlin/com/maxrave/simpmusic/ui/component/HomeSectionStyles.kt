package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel

/** Home artwork shelves use a compact, two-column grid. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContentCarousel(
    data: HomeItem,
    navController: NavController,
    homeViewModel: HomeViewModel,
) {
    // Keep Home lightweight and show two cards per shelf; the shelf's More action exposes the rest.
    val contents = remember(data.contents) { data.contents.filterNotNull().take(2) }
    if (contents.isEmpty()) return

    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    sheetTrack?.let { track ->
        NowPlayingBottomSheet(
            onDismiss = { sheetTrack = null },
            song = track.toSongEntity(),
            navController = navController,
        )
    }

    Column {
        HomeSectionHeader(data = data, navController = navController, onPlayAll = { homeViewModel.playAll(data) })
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = ((maxWidth - 8.dp) / 2).coerceAtLeast(120.dp)
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                contents.chunked(2).forEach { rowContents ->
                    val artworkHeight = rowContents.maxOf { cardWidth / it.homeCardAspectRatio() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowContents.forEach { content ->
                            Box(
                                Modifier.weight(1f).height(artworkHeight + 76.dp)
                                    .clip(RoundedCornerShape(if (data.title.equals("Covers and remixes", ignoreCase = true)) 28.dp else 20.dp)),
                            ) {
                                HomeContentCard(
                                    temp = content,
                                    navController = navController,
                                    viewModel = homeViewModel,
                                    fillMaxWidth = true,
                                    onLongClick = { sheetTrack = it.toTrack() },
                                )
                            }
                        }
                        if (rowContents.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Daily discover uses the same two-column grid; tapping a card starts its radio. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeDiscoverSection(
    data: HomeItem,
    navController: NavController,
    homeViewModel: HomeViewModel,
) {
    val songs = remember(data.contents) { data.contents.filterNotNull().take(2) }
    if (songs.isEmpty()) return
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    sheetTrack?.let { track ->
        NowPlayingBottomSheet(
            onDismiss = { sheetTrack = null },
            song = track.toSongEntity(),
            navController = navController,
        )
    }
    Column {
        HomeSectionHeader(data = data, navController = navController, onPlayAll = { homeViewModel.playAll(data) })
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = ((maxWidth - 8.dp) / 2).coerceAtLeast(120.dp)
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                songs.chunked(2).forEach { rowSongs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowSongs.forEach { song ->
                            val play: () -> Unit = { song.playRadio(homeViewModel) }
                            val (hoverModifier, hovered) = rememberCardHover(enabled = true)
                            Box(
                                Modifier.weight(1f).height(cardWidth).then(hoverModifier)
                                    .clip(RoundedCornerShape(24.dp))
                                    .combinedClickable(onClick = play, onLongClick = { sheetTrack = song.toTrack() }),
                            ) {
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(song.thumbnails.lastOrNull()?.url)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(song.thumbnails.lastOrNull()?.url)
                                            .crossfade(350)
                                            .build(),
                                    contentDescription = null,
                                    placeholder = rememberHolderPainter(),
                                    error = rememberHolderPainter(),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().hoverZoom(hovered),
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            smoothScrimBrush(
                                                from = MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                                                to = MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f),
                                                startFraction = 0.45f,
                                            ),
                                        ),
                                )
                                ForceDarkContent {
                                    Column(
                                        Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(12.dp),
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = typo().titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = song.artists.toListName().connectArtists(),
                                            style = typo().bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                HoverPlayButton(visible = hovered, onClick = play)
                            }
                        }
                        if (rowSongs.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

