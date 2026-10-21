package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaybackMode
import com.example.data.model.Track
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.TrackItem
import com.example.ui.dialogs.AddToPlaylistSheet
import com.example.ui.dialogs.CreatePlaylistDialog
import com.example.ui.dialogs.PlaylistDetailSheet
import com.example.ui.dialogs.QueueSheet
import com.example.ui.dialogs.SleepTimerDialog
import com.example.ui.dialogs.TrackInfoSheet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHover
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.TrackFilter
import kotlinx.coroutines.launch

@Composable
fun MainMusicScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // States from ViewModel
    val selectedTab by viewModel.selectedTab.collectAsState()
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val trackFilter by viewModel.trackFilter.collectAsState()
    val playlists by viewModel.playlistsFlow.collectAsState()
    val favorites by viewModel.favoritesFlow.collectAsState()
    val artistsMap by viewModel.artistsMap.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // Player States
    val currentTrack by viewModel.playerManager.currentTrack.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val currentPositionMs by viewModel.playerManager.currentPositionMs.collectAsState()
    val playbackMode by viewModel.playerManager.playbackMode.collectAsState()
    val playbackSpeed by viewModel.playerManager.playbackSpeed.collectAsState()
    val sleepTimerRemainingSec by viewModel.playerManager.sleepTimerRemainingSeconds.collectAsState()
    val sleepTimerOption by viewModel.playerManager.sleepTimerOption.collectAsState()
    val equalizerState by viewModel.playerManager.equalizerState.collectAsState()
    val waveformBars by viewModel.playerManager.waveformBars.collectAsState()
    val currentQueue by viewModel.playerManager.queue.collectAsState()

    // Dialog & Sheet States
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val isQueueSheetVisible by viewModel.isQueueSheetVisible.collectAsState()
    val isSleepTimerDialogVisible by viewModel.isSleepTimerDialogVisible.collectAsState()
    val isCreatePlaylistDialogVisible by viewModel.isCreatePlaylistDialogVisible.collectAsState()
    val addToPlaylistTrack by viewModel.addToPlaylistTrack.collectAsState()
    val selectedPlaylistForDetail by viewModel.selectedPlaylistForDetail.collectAsState()
    val songInfoTrack by viewModel.songInfoTrack.collectAsState()

    // File Picker Launcher for SAF audio import
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importAudioUri(it)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Imported audio file successfully")
            }
        }
    }

    // Media Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.rescanDeviceMusic()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Device music library scanned")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Storage permission required to read device songs")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(DarkSurface)) {
                // Mini Player docked right above NavigationBar
                MiniPlayerBar(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    onTogglePlay = { viewModel.playerManager.togglePlayPause() },
                    onSkipNext = { viewModel.playerManager.skipNext() },
                    onExpandNowPlaying = { viewModel.setNowPlayingExpanded(true) }
                )

                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = PrimaryText,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.TRACKS,
                        onClick = { viewModel.setSelectedTab(MainTab.TRACKS) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == MainTab.TRACKS) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Tracks"
                            )
                        },
                        label = { Text("Tracks") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonPurple,
                            indicatorColor = NeonPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        ),
                        modifier = Modifier.testTag("nav_tab_tracks")
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.PLAYLISTS,
                        onClick = { viewModel.setSelectedTab(MainTab.PLAYLISTS) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == MainTab.PLAYLISTS) Icons.AutoMirrored.Filled.QueueMusic else Icons.Outlined.QueueMusic,
                                contentDescription = "Playlists"
                            )
                        },
                        label = { Text("Playlists") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonPurple,
                            indicatorColor = NeonPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        ),
                        modifier = Modifier.testTag("nav_tab_playlists")
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.ARTISTS,
                        onClick = { viewModel.setSelectedTab(MainTab.ARTISTS) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == MainTab.ARTISTS) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Artists"
                            )
                        },
                        label = { Text("Artists") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonPurple,
                            indicatorColor = NeonPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        ),
                        modifier = Modifier.testTag("nav_tab_artists")
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.EQUALIZER,
                        onClick = { viewModel.setSelectedTab(MainTab.EQUALIZER) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == MainTab.EQUALIZER) Icons.Filled.GraphicEq else Icons.Outlined.GraphicEq,
                                contentDescription = "Sound"
                            )
                        },
                        label = { Text("Sound") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonPurple,
                            indicatorColor = NeonPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        ),
                        modifier = Modifier.testTag("nav_tab_equalizer")
                    )
                }
            }
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.TRACKS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // App Branding Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(NeonPurple, NeonPink)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Resonance",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText
                                        )
                                    )
                                    Text(
                                        text = "Hi-Fi Offline Music Player",
                                        style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                                    )
                                }
                            }

                            if (isScanning) {
                                CircularProgressIndicator(
                                    color = NeonPurple,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search songs, artists, albums…", color = SecondaryText) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SecondaryText)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = SecondaryText)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_tracks_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Filter Pills
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(TrackFilter.values()) { filter ->
                                val isSelected = trackFilter == filter
                                val label = when (filter) {
                                    TrackFilter.ALL -> "All Songs (${viewModel.rawTracks.collectAsState().value.size})"
                                    TrackFilter.FAVORITES -> "Favorites (${favorites.size})"
                                    TrackFilter.DEVICE -> "Device"
                                    TrackFilter.BUILT_IN -> "Built-in (Hi-Fi)"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NeonPurple else DarkSurfaceVariant)
                                        .clickable { viewModel.setTrackFilter(filter) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else PrimaryText
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Bar: Shuffle All, Scan Device, Open Audio File
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.shuffleAll() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonPurple,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).testTag("action_shuffle_all")
                            ) {
                                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shuffle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                modifier = Modifier.weight(1f).testTag("action_scan_device")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Media", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText),
                                modifier = Modifier.weight(1f).testTag("action_open_file")
                            ) {
                                Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open File", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tracks LazyColumn
                        if (filteredTracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = SecondaryText,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No tracks found",
                                        style = MaterialTheme.typography.titleMedium.copy(color = PrimaryText)
                                    )
                                    Text(
                                        text = "Try switching filters or tapping 'Open File' / 'Scan Media'",
                                        style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredTracks, key = { it.id }) { track ->
                                    TrackItem(
                                        track = track,
                                        isCurrentTrack = currentTrack?.id == track.id,
                                        isPlaying = isPlaying,
                                        onClick = { viewModel.playTrack(track) },
                                        onToggleFavorite = { viewModel.toggleFavorite(track) },
                                        onAddToPlaylist = { viewModel.setAddToPlaylistTrack(track) },
                                        onSongInfo = { viewModel.setSongInfoTrack(track) }
                                    )
                                }
                            }
                        }
                    }
                }

                MainTab.PLAYLISTS -> {
                    val favIds = favorites.map { it.trackId }.toSet()
                    val favTracks = viewModel.rawTracks.collectAsState().value.filter { favIds.contains(it.id) }
                    PlaylistsScreen(
                        playlists = playlists,
                        favoriteTracks = favTracks,
                        onCreatePlaylist = { viewModel.setCreatePlaylistDialogVisible(true) },
                        onOpenFavorites = {
                            viewModel.setTrackFilter(TrackFilter.FAVORITES)
                            viewModel.setSelectedTab(MainTab.TRACKS)
                        },
                        onSelectPlaylist = { pl -> viewModel.setSelectedPlaylistForDetail(pl) },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) }
                    )
                }

                MainTab.ARTISTS -> {
                    ArtistsScreen(
                        artistsMap = artistsMap,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlayTrack = { viewModel.playTrack(it) },
                        onPlayArtist = { tracks ->
                            viewModel.playerManager.setQueue(tracks, startIndex = 0, autoPlay = true)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { viewModel.setAddToPlaylistTrack(it) },
                        onSongInfo = { viewModel.setSongInfoTrack(it) }
                    )
                }

                MainTab.EQUALIZER -> {
                    EqualizerScreen(
                        equalizerState = equalizerState,
                        waveformBars = waveformBars,
                        isPlaying = isPlaying,
                        onSelectPreset = { preset -> viewModel.playerManager.setEqualizerPreset(preset) },
                        onBandChange = { bandIndex, levelDb -> viewModel.playerManager.setBandLevel(bandIndex, levelDb) },
                        onBassBoostChange = { percent -> viewModel.playerManager.setBassBoost(percent) },
                        onVirtualizerChange = { percent -> viewModel.playerManager.setVirtualizer(percent) }
                    )
                }
            }

            // Full-screen Now Playing Overlay
            AnimatedVisibility(
                visible = isNowPlayingExpanded && currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingSheet(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    playbackMode = playbackMode,
                    playbackSpeed = playbackSpeed,
                    sleepTimerRemainingSec = sleepTimerRemainingSec,
                    waveformBars = waveformBars,
                    onTogglePlay = { viewModel.playerManager.togglePlayPause() },
                    onSkipNext = { viewModel.playerManager.skipNext() },
                    onSkipPrevious = { viewModel.playerManager.skipPrevious() },
                    onSeekTo = { viewModel.playerManager.seekTo(it) },
                    onCyclePlaybackMode = { viewModel.playerManager.cyclePlaybackMode() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onOpenQueue = { viewModel.setQueueSheetVisible(true) },
                    onOpenEqualizer = {
                        viewModel.setNowPlayingExpanded(false)
                        viewModel.setSelectedTab(MainTab.EQUALIZER)
                    },
                    onOpenSleepTimer = { viewModel.setSleepTimerDialogVisible(true) },
                    onOpenSongInfo = { viewModel.setSongInfoTrack(currentTrack) },
                    onCycleSpeed = {
                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                        viewModel.playerManager.setPlaybackSpeed(speeds[nextIdx])
                    },
                    onCollapse = { viewModel.setNowPlayingExpanded(false) }
                )
            }
        }
    }

    // Dialogs & Sheets
    if (isCreatePlaylistDialogVisible) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.setCreatePlaylistDialogVisible(false) },
            onConfirm = { name, desc -> viewModel.createPlaylist(name, desc) }
        )
    }

    addToPlaylistTrack?.let { track ->
        AddToPlaylistSheet(
            track = track,
            playlists = playlists,
            onSelectPlaylist = { plId -> viewModel.addTrackToPlaylist(plId, track) },
            onCreateNewPlaylist = {
                viewModel.setAddToPlaylistTrack(null)
                viewModel.setCreatePlaylistDialogVisible(true)
            },
            onDismiss = { viewModel.setAddToPlaylistTrack(null) }
        )
    }

    selectedPlaylistForDetail?.let { pl ->
        val playlistTracks by viewModel.getTracksForPlaylist(pl.id).collectAsState()
        PlaylistDetailSheet(
            playlist = pl,
            tracks = playlistTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            onPlayTrack = { viewModel.playTrack(it) },
            onPlayAll = {
                if (playlistTracks.isNotEmpty()) {
                    viewModel.playerManager.setQueue(playlistTracks, startIndex = 0, autoPlay = true)
                }
            },
            onShuffleAll = {
                if (playlistTracks.isNotEmpty()) {
                    viewModel.playerManager.setPlaybackMode(PlaybackMode.SHUFFLE)
                    viewModel.playerManager.setQueue(playlistTracks.shuffled(), startIndex = 0, autoPlay = true)
                }
            },
            onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(pl.id, trackId) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDismiss = { viewModel.setSelectedPlaylistForDetail(null) }
        )
    }

    if (isSleepTimerDialogVisible) {
        SleepTimerDialog(
            currentOption = sleepTimerOption,
            remainingSeconds = sleepTimerRemainingSec,
            onSelectOption = { opt -> viewModel.playerManager.setSleepTimer(opt) },
            onDismiss = { viewModel.setSleepTimerDialogVisible(false) }
        )
    }

    songInfoTrack?.let { track ->
        TrackInfoSheet(
            track = track,
            onDismiss = { viewModel.setSongInfoTrack(null) }
        )
    }

    if (isQueueSheetVisible) {
        QueueSheet(
            queue = currentQueue,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            onSelectTrack = { viewModel.playTrack(it) },
            onDismiss = { viewModel.setQueueSheetVisible(false) }
        )
    }
}
