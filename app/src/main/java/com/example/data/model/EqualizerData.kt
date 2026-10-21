package com.example.data.model

data class EqualizerBand(
    val index: Int,
    val frequencyLabel: String,
    val levelMb: Int // millibels: -1000 to +1000 (i.e. -10dB to +10dB)
)

data class EqualizerPreset(
    val name: String,
    val levels: List<Int>, // 5 band levels in dB: e.g. [-10 to 10]
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0
)

object EqualizerPresets {
    val presets = listOf(
        EqualizerPreset("Normal", listOf(0, 0, 0, 0, 0), bassBoostPercent = 10, virtualizerPercent = 10),
        EqualizerPreset("Bass Boost", listOf(7, 5, 0, -1, -2), bassBoostPercent = 85, virtualizerPercent = 25),
        EqualizerPreset("Acoustic", listOf(3, 2, 1, 3, 4), bassBoostPercent = 20, virtualizerPercent = 40),
        EqualizerPreset("Electronic", listOf(6, 4, -1, 3, 6), bassBoostPercent = 65, virtualizerPercent = 50),
        EqualizerPreset("Rock & Metal", listOf(5, 3, -2, 4, 6), bassBoostPercent = 55, virtualizerPercent = 35),
        EqualizerPreset("Vocal Boost", listOf(-2, 1, 5, 4, 1), bassBoostPercent = 10, virtualizerPercent = 30)
    )
}
