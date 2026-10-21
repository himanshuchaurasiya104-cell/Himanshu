package com.example.data.audio

import android.content.Context
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioSynthesizer {

    suspend fun getOrCreateBuiltInTracks(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val musicDir = File(context.filesDir, "built_in_music").apply { mkdirs() }
        val tracks = mutableListOf<Track>()

        val trackSpecs = listOf(
            SynthSpec(
                id = -101L,
                fileName = "neon_horizon.wav",
                title = "Neon Horizon",
                artist = "Aura Wave",
                album = "Cyber Synth 2026",
                genre = "Synthwave",
                durationSeconds = 24,
                bpm = 120,
                colorHex = 0xFFC084FC,
                preset = SoundPreset.SYNTHWAVE,
                lyrics = "[Verse 1]\nNeon lines across the grid\nDrifting where the daylight slid\nElectric echoes in the night\nGuided by the purple light\n\n[Chorus]\nInfinite horizon glows\nEvery frequency that flows\nWe ride the neon tide tonight."
            ),
            SynthSpec(
                id = -102L,
                fileName = "midnight_rain.wav",
                title = "Midnight Rain",
                artist = "Lofi Reverie",
                album = "Coffee & Raindrops",
                genre = "Lo-Fi Beats",
                durationSeconds = 26,
                bpm = 84,
                colorHex = 0xFF38BDF8,
                preset = SoundPreset.LOFI,
                lyrics = "[Intro]\nGentle drops on glass window panes...\nCoffee steam rising softly...\n\n[Mellow Hook]\nMidnight raindrops fall in time\nSoft rhodes chords in rhythm chime\nPeaceful thoughts and quiet mind."
            ),
            SynthSpec(
                id = -103L,
                fileName = "celestial_drift.wav",
                title = "Celestial Drift",
                artist = "Starlight Ambient",
                album = "Cosmic Sanctuary",
                genre = "Ambient",
                durationSeconds = 28,
                bpm = 64,
                colorHex = 0xFFF43F5E,
                preset = SoundPreset.AMBIENT,
                lyrics = "[Atmosphere]\nWeightless float through stardust rings\nHarmonic frequencies of distant nebulae\n\n[Refrain]\nSuspended in the calm expanse\nA celestial resonance dance."
            ),
            SynthSpec(
                id = -104L,
                fileName = "solar_flare.wav",
                title = "Solar Flare",
                artist = "Pulse Vector",
                album = "Overdrive Odyssey",
                genre = "Cyber Electro",
                durationSeconds = 22,
                bpm = 128,
                colorHex = 0xFFF59E0B,
                preset = SoundPreset.ELECTRO,
                lyrics = "[Build Up]\nCharging core capacitor...\n3... 2... 1... ignition!\n\n[Drop]\nHigh voltage spark, kinetic sound\nShaking the pulse under the ground."
            ),
            SynthSpec(
                id = -105L,
                fileName = "echoes_of_dawn.wav",
                title = "Echoes of Dawn",
                artist = "Aria Sol",
                album = "Morning Reflections",
                genre = "Neo-Classical",
                durationSeconds = 25,
                bpm = 90,
                colorHex = 0xFF10B981,
                preset = SoundPreset.ACOUSTIC_PIANO,
                lyrics = "[Movement I]\nFirst light paints the quiet valley\nChords resonate through morning mist\n\n[Movement II]\nAwakening every slumbering note\nIn harmony with the breaking day."
            ),
            SynthSpec(
                id = -106L,
                fileName = "velvet_groove.wav",
                title = "Velvet Groove",
                artist = "Soul Transit",
                album = "Velvet Sessions",
                genre = "Funk & Chill",
                durationSeconds = 24,
                bpm = 105,
                colorHex = 0xFFA855F7,
                preset = SoundPreset.FUNK_GROOVE,
                lyrics = "[Groove Verse]\nWalking bassline tight and clean\nSmooth as silk you've ever seen\nSnare on two and four today\nLet the velvet rhythm play."
            )
        )

        for (spec in trackSpecs) {
            val audioFile = File(musicDir, spec.fileName)
            if (!audioFile.exists() || audioFile.length() < 1000) {
                generateWavFile(audioFile, spec)
            }
            tracks.add(
                Track(
                    id = spec.id,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    durationMs = spec.durationSeconds * 1000L,
                    uri = audioFile.absolutePath,
                    albumArtUri = null,
                    genre = spec.genre,
                    year = 2026,
                    isFavorite = false,
                    lyrics = spec.lyrics,
                    colorHex = spec.colorHex,
                    isBuiltIn = true
                )
            )
        }

        tracks
    }

    private enum class SoundPreset {
        SYNTHWAVE, LOFI, AMBIENT, ELECTRO, ACOUSTIC_PIANO, FUNK_GROOVE
    }

    private data class SynthSpec(
        val id: Long,
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val durationSeconds: Int,
        val bpm: Int,
        val colorHex: Long,
        val preset: SoundPreset,
        val lyrics: String
    )

    private fun generateWavFile(file: File, spec: SynthSpec) {
        val sampleRate = 22050
        val numChannels = 2
        val totalSamples = sampleRate * spec.durationSeconds
        val bytesPerSample = 2
        val dataSize = totalSamples * numChannels * bytesPerSample

        FileOutputStream(file).use { out ->
            // WAV Header
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size
            header.putShort(1) // PCM format
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
            header.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(dataSize)
            out.write(header.array())

            // Synthesize stereo samples in chunks
            val chunkSize = 4096
            val buffer = ByteBuffer.allocate(chunkSize * numChannels * bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)

            val beatInterval = sampleRate * 60.0 / spec.bpm
            val chordProgressions = when (spec.preset) {
                SoundPreset.SYNTHWAVE -> listOf(
                    listOf(220.0, 261.63, 329.63, 440.0), // Am
                    listOf(174.61, 220.0, 261.63, 349.23), // F
                    listOf(261.63, 329.63, 392.0, 523.25), // C
                    listOf(196.0, 246.94, 293.66, 392.0)   // G
                )
                SoundPreset.LOFI -> listOf(
                    listOf(196.0, 246.94, 293.66, 370.0), // Gmaj7
                    listOf(185.0, 233.08, 277.18, 329.63), // F#m7
                    listOf(164.81, 207.65, 246.94, 311.13), // Em7
                    listOf(220.0, 277.18, 329.63, 415.30)  // A7
                )
                SoundPreset.AMBIENT -> listOf(
                    listOf(146.83, 220.0, 293.66, 369.99), // Dmaj9
                    listOf(164.81, 246.94, 329.63, 392.0),  // Em9
                    listOf(196.0, 293.66, 392.0, 440.0),    // Gmaj9
                    listOf(220.0, 329.63, 440.0, 554.37)   // A add9
                )
                SoundPreset.ELECTRO -> listOf(
                    listOf(110.0, 164.81, 220.0, 329.63),
                    listOf(130.81, 196.0, 261.63, 392.0),
                    listOf(146.83, 220.0, 293.66, 440.0),
                    listOf(98.0, 146.83, 196.0, 293.66)
                )
                SoundPreset.ACOUSTIC_PIANO -> listOf(
                    listOf(261.63, 329.63, 392.0, 523.25),
                    listOf(220.0, 261.63, 329.63, 440.0),
                    listOf(174.61, 220.0, 261.63, 349.23),
                    listOf(196.0, 246.94, 293.66, 392.0)
                )
                SoundPreset.FUNK_GROOVE -> listOf(
                    listOf(146.83, 220.0, 261.63, 349.23),
                    listOf(196.0, 246.94, 293.66, 349.23),
                    listOf(220.0, 261.63, 329.63, 392.0),
                    listOf(164.81, 207.65, 246.94, 329.63)
                )
            }

            var sampleIndex = 0
            while (sampleIndex < totalSamples) {
                buffer.clear()
                val chunkEnd = minOf(totalSamples, sampleIndex + chunkSize)
                for (s in sampleIndex until chunkEnd) {
                    val t = s.toDouble() / sampleRate
                    val beat = s / beatInterval
                    val bar = (beat / 4.0).toInt() % chordProgressions.size
                    val chord = chordProgressions[bar]
                    val beatFraction = (beat % 1.0)

                    // Percussion / Beat pulse
                    val kickEnvelope = exp(-beatFraction * 8.0)
                    val kickFreq = 120.0 * exp(-beatFraction * 14.0) + 40.0
                    val kick = sin(2.0 * PI * kickFreq * (beatFraction * (60.0 / spec.bpm))) * kickEnvelope * 0.35

                    // Hi-hat pulse on 0.5 beat
                    val hiHatFraction = ((beat + 0.5) % 1.0)
                    val hiHatEnvelope = exp(-hiHatFraction * 20.0)
                    val hiHat = (sin(2.0 * PI * 6500.0 * t) + sin(2.0 * PI * 8200.0 * t)) * hiHatEnvelope * 0.12

                    // Melodic chords / Pad
                    var pad = 0.0
                    for (note in chord) {
                        pad += sin(2.0 * PI * note * t) * 0.08
                        // add a subtle octave / harmonic
                        pad += sin(2.0 * PI * (note * 2.01) * t) * 0.03
                    }

                    // Arpeggiated lead
                    val arpStep = ((beat * 4.0).toInt() % chord.size)
                    val arpNote = chord[arpStep] * 2.0
                    val arpEnvelope = exp(-(beat * 4.0 % 1.0) * 5.0)
                    val lead = sin(2.0 * PI * arpNote * t) * arpEnvelope * 0.15

                    // Bass note
                    val bassFreq = chord[0] / 2.0
                    val bass = sin(2.0 * PI * bassFreq * t) * 0.22

                    // Combine & stereo separation
                    var leftSignal = (pad + lead * 0.9 + bass + kick * 0.9 + hiHat * 0.7)
                    var rightSignal = (pad + lead * 1.1 + bass + kick * 0.9 + hiHat * 1.1)

                    // Gentle master limiter / soft clip
                    leftSignal = leftSignal.coerceIn(-0.95, 0.95)
                    rightSignal = rightSignal.coerceIn(-0.95, 0.95)

                    val leftShort = (leftSignal * 32767.0).toInt().toShort()
                    val rightShort = (rightSignal * 32767.0).toInt().toShort()

                    buffer.putShort(leftShort)
                    buffer.putShort(rightShort)
                }

                out.write(buffer.array(), 0, buffer.position())
                sampleIndex = chunkEnd
            }
        }
    }
}
