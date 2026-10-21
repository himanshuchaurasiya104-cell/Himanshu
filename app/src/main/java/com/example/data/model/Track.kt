package com.example.data.model

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String? = null,
    val genre: String = "Music",
    val year: Int = 2026,
    val isFavorite: Boolean = false,
    val lyrics: String = "",
    val colorHex: Long = 0xFFA855F7,
    val isBuiltIn: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }
}

enum class PlaybackMode {
    NORMAL,
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE
}

enum class SleepTimerOption(val minutes: Int, val label: String) {
    OFF(0, "Off"),
    MIN_15(15, "15 mins"),
    MIN_30(30, "30 mins"),
    MIN_45(45, "45 mins"),
    MIN_60(60, "60 mins"),
    END_OF_TRACK(-1, "End of Song")
}
