package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioSynthesizer
import com.example.data.audio.DeviceAudioScanner
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteTrackEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistTrackCrossRef
import com.example.data.db.RecentlyPlayedEntity
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerPresets
import com.example.data.model.PlaybackMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.Track
import com.example.data.player.EqualizerUiState
import com.example.data.player.MusicPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    TRACKS, PLAYLISTS, ARTISTS, EQUALIZER
}

enum class TrackFilter {
    ALL, FAVORITES, DEVICE, BUILT_IN
}

data class PlaylistWithTracks(
    val playlist: PlaylistEntity,
    val tracks: List<Track>
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.musicDao()
    val playerManager = MusicPlayerManager(application)

    // Base all tracks list
    private val _rawTracks = MutableStateFlow<List<Track>>(emptyList())
    val rawTracks: StateFlow<List<Track>> = _rawTracks.asStateFlow()

    // Navigation & Tab state
    private val _selectedTab = MutableStateFlow(MainTab.TRACKS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    // Search query & filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _trackFilter = MutableStateFlow(TrackFilter.ALL)
    val trackFilter: StateFlow<TrackFilter> = _trackFilter.asStateFlow()

    // UI Sheets & Dialogs
    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _isQueueSheetVisible = MutableStateFlow(false)
    val isQueueSheetVisible: StateFlow<Boolean> = _isQueueSheetVisible.asStateFlow()

    private val _isSleepTimerDialogVisible = MutableStateFlow(false)
    val isSleepTimerDialogVisible: StateFlow<Boolean> = _isSleepTimerDialogVisible.asStateFlow()

    private val _isCreatePlaylistDialogVisible = MutableStateFlow(false)
    val isCreatePlaylistDialogVisible: StateFlow<Boolean> = _isCreatePlaylistDialogVisible.asStateFlow()

    private val _addToPlaylistTrack = MutableStateFlow<Track?>(null)
    val addToPlaylistTrack: StateFlow<Track?> = _addToPlaylistTrack.asStateFlow()

    private val _selectedPlaylistForDetail = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylistForDetail: StateFlow<PlaylistEntity?> = _selectedPlaylistForDetail.asStateFlow()

    private val _songInfoTrack = MutableStateFlow<Track?>(null)
    val songInfoTrack: StateFlow<Track?> = _songInfoTrack.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Flow from Room for Favorites
    val favoritesFlow = dao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow from Room for Playlists
    val playlistsFlow = dao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined filtered tracks
    val filteredTracks: StateFlow<List<Track>> = combine(
        _rawTracks,
        _searchQuery,
        _trackFilter,
        favoritesFlow
    ) { tracks, query, filter, favs ->
        val favIds = favs.map { it.trackId }.toSet()
        val mappedTracks = tracks.map { it.copy(isFavorite = favIds.contains(it.id)) }

        val byFilter = when (filter) {
            TrackFilter.ALL -> mappedTracks
            TrackFilter.FAVORITES -> mappedTracks.filter { it.isFavorite }
            TrackFilter.DEVICE -> mappedTracks.filter { !it.isBuiltIn }
            TrackFilter.BUILT_IN -> mappedTracks.filter { it.isBuiltIn }
        }

        if (query.isBlank()) {
            byFilter
        } else {
            val q = query.trim().lowercase()
            byFilter.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.genre.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group tracks by artist
    val artistsMap: StateFlow<Map<String, List<Track>>> = filteredTracks.combine(_rawTracks) { filtered, _ ->
        filtered.groupBy { it.artist }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadInitialLibrary()
    }

    private fun loadInitialLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            // 1. Generate/load built-in synth tracks
            val builtIn = AudioSynthesizer.getOrCreateBuiltInTracks(getApplication())
            // 2. Scan device tracks
            val deviceTracks = DeviceAudioScanner.scanDeviceMusic(getApplication())
            val combined = (builtIn + deviceTracks).distinctBy { it.id }
            _rawTracks.value = combined

            // Setup default queue if empty
            if (playerManager.queue.value.isEmpty() && combined.isNotEmpty()) {
                playerManager.setQueue(combined, startIndex = 0, autoPlay = false)
            }
            _isScanning.value = false
        }
    }

    fun rescanDeviceMusic() {
        viewModelScope.launch {
            _isScanning.value = true
            val builtIn = AudioSynthesizer.getOrCreateBuiltInTracks(getApplication())
            val deviceTracks = DeviceAudioScanner.scanDeviceMusic(getApplication())
            val combined = (builtIn + deviceTracks).distinctBy { it.id }
            _rawTracks.value = combined
            _isScanning.value = false
        }
    }

    fun importAudioUri(uri: Uri) {
        viewModelScope.launch {
            val imported = DeviceAudioScanner.getTrackFromUri(getApplication(), uri)
            if (imported != null) {
                val current = _rawTracks.value.toMutableList()
                current.add(0, imported)
                _rawTracks.value = current.distinctBy { it.id }
                playTrack(imported)
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            dao.addRecentTrack(RecentlyPlayedEntity(trackId = track.id))
            val currentList = filteredTracks.value.ifEmpty { _rawTracks.value }
            val index = currentList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            playerManager.setQueue(currentList, startIndex = index, autoPlay = true)
        }
    }

    fun shuffleAll() {
        val tracks = filteredTracks.value.ifEmpty { _rawTracks.value }
        if (tracks.isNotEmpty()) {
            playerManager.setPlaybackMode(PlaybackMode.SHUFFLE)
            val shuffled = tracks.shuffled()
            playerManager.setQueue(shuffled, startIndex = 0, autoPlay = true)
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val isFav = dao.isFavorite(track.id).first()
            if (isFav) {
                dao.deleteFavorite(track.id)
            } else {
                dao.insertFavorite(FavoriteTrackEntity(trackId = track.id))
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val colors = listOf(0xFFC084FC, 0xFF38BDF8, 0xFFF43F5E, 0xFF10B981, 0xFFF59E0B)
            val randomColor = colors.random()
            dao.insertPlaylist(
                PlaylistEntity(
                    name = name.trim(),
                    description = description.trim(),
                    colorHex = randomColor
                )
            )
            _isCreatePlaylistDialogVisible.value = false
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            dao.deletePlaylist(playlistId)
            dao.deletePlaylistTracks(playlistId)
            if (_selectedPlaylistForDetail.value?.id == playlistId) {
                _selectedPlaylistForDetail.value = null
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = track.id))
            _addToPlaylistTrack.value = null
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            dao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun getTracksForPlaylist(playlistId: Long): StateFlow<List<Track>> {
        return dao.getTrackIdsForPlaylist(playlistId).combine(_rawTracks) { trackIds, allTracks ->
            val trackMap = allTracks.associateBy { it.id }
            trackIds.mapNotNull { trackMap[it] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Setters for UI state
    fun setSelectedTab(tab: MainTab) { _selectedTab.value = tab }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setTrackFilter(filter: TrackFilter) { _trackFilter.value = filter }
    fun setNowPlayingExpanded(expanded: Boolean) { _isNowPlayingExpanded.value = expanded }
    fun setQueueSheetVisible(visible: Boolean) { _isQueueSheetVisible.value = visible }
    fun setSleepTimerDialogVisible(visible: Boolean) { _isSleepTimerDialogVisible.value = visible }
    fun setCreatePlaylistDialogVisible(visible: Boolean) { _isCreatePlaylistDialogVisible.value = visible }
    fun setAddToPlaylistTrack(track: Track?) { _addToPlaylistTrack.value = track }
    fun setSelectedPlaylistForDetail(playlist: PlaylistEntity?) { _selectedPlaylistForDetail.value = playlist }
    fun setSongInfoTrack(track: Track?) { _songInfoTrack.value = track }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
