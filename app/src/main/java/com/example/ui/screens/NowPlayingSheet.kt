package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaybackMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.Track
import com.example.ui.components.VinylDisc
import com.example.ui.components.VisualizerWave
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    playbackMode: PlaybackMode,
    playbackSpeed: Float,
    sleepTimerRemainingSec: Int?,
    waveformBars: List<Float>,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSongInfo: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = track != null) {
        onCollapse()
    }

    if (track == null) return

    val trackColor = Color(track.colorHex)
    val durationMs = track.durationMs.coerceAtLeast(1L)

    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var draggingSliderPos by remember { mutableFloatStateOf(0f) }

    val currentSliderVal = if (isUserDraggingSlider) {
        draggingSliderPos
    } else {
        (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    val displayPosMs = if (isUserDraggingSlider) {
        (draggingSliderPos * durationMs).toLong()
    } else {
        currentPositionMs
    }

    val currentMin = displayPosMs / 1000 / 60
    val currentSec = (displayPosMs / 1000) % 60
    val durMin = durationMs / 1000 / 60
    val durSec = (durationMs / 1000) % 60

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("now_playing_screen"),
        color = DarkBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            trackColor.copy(alpha = 0.28f),
                            DarkBackground,
                            DarkBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.testTag("now_playing_collapse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse player",
                            tint = PrimaryText,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SecondaryText,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.testTag("now_playing_queue_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Current queue",
                            tint = PrimaryText,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Vinyl Disc with halo
                Box(
                    modifier = Modifier
                        .size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow halo behind vinyl
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        trackColor.copy(alpha = if (isPlaying) 0.45f else 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    VinylDisc(
                        isPlaying = isPlaying,
                        labelColor = trackColor,
                        size = 240.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Real-time Audio Waveform Visualizer
                VisualizerWave(
                    bars = waveformBars,
                    isPlaying = isPlaying,
                    accentColor = trackColor,
                    height = 36.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Track Title, Artist & Favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${track.artist} • ${track.genre}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SecondaryText
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_fav_btn")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (track.isFavorite) NeonPink else SecondaryText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrubber Slider
                Slider(
                    value = currentSliderVal,
                    onValueChange = {
                        isUserDraggingSlider = true
                        draggingSliderPos = it
                    },
                    onValueChangeFinished = {
                        val seekTarget = (draggingSliderPos * durationMs).toLong()
                        onSeekTo(seekTarget)
                        isUserDraggingSlider = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = NeonPurple,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("now_playing_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "%d:%02d".format(currentMin, currentSec),
                        style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                    )
                    Text(
                        text = "%d:%02d".format(durMin, durSec),
                        style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Shuffle Mode
                    IconButton(
                        onClick = onCyclePlaybackMode,
                        modifier = Modifier.size(44.dp).testTag("now_playing_shuffle_btn")
                    ) {
                        val isShuffle = playbackMode == PlaybackMode.SHUFFLE
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) CyberCyan else SecondaryText
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(48.dp).testTag("now_playing_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = PrimaryText,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = { onSeekTo((displayPosMs - 10000L).coerceAtLeast(0L)) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = SecondaryText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Large Play/Pause FAB
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NeonPurple, NeonPink)
                                )
                            )
                            .clickable(onClick = onTogglePlay)
                            .testTag("now_playing_play_pause_fab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Fast Forward 10s
                    IconButton(
                        onClick = { onSeekTo((displayPosMs + 10000L).coerceAtMost(durationMs)) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = SecondaryText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Skip Next
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(48.dp).testTag("now_playing_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = PrimaryText,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(
                        onClick = onCyclePlaybackMode,
                        modifier = Modifier.size(44.dp).testTag("now_playing_repeat_btn")
                    ) {
                        val icon = if (playbackMode == PlaybackMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                        val tint = if (playbackMode != PlaybackMode.NORMAL && playbackMode != PlaybackMode.SHUFFLE) CyberCyan else SecondaryText
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Tool Pills: Speed, EQ, Sleep Timer, Lyrics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed selector
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceVariant)
                            .clickable(onClick = onCycleSpeed)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            )
                        }
                    }

                    // Equalizer
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = NeonPurple
                        )
                    }

                    // Sleep Timer
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sleepTimerRemainingSec != null) NeonPurple.copy(alpha = 0.25f) else DarkSurfaceVariant)
                            .clickable(onClick = onOpenSleepTimer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerRemainingSec != null) NeonPurple else SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val timerText = if (sleepTimerRemainingSec != null) {
                                if (sleepTimerRemainingSec < 0) "End" else "${sleepTimerRemainingSec / 60}m"
                            } else {
                                "Timer"
                            }
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (sleepTimerRemainingSec != null) NeonPurple else SecondaryText
                                )
                            )
                        }
                    }

                    // Track Info / Lyrics
                    IconButton(
                        onClick = onOpenSongInfo,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Track Info & Lyrics",
                            tint = SecondaryText
                        )
                    }
                }
            }
        }
    }
}
