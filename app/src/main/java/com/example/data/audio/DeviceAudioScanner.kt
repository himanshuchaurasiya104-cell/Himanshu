package com.example.data.audio

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DeviceAudioScanner {

    suspend fun scanDeviceMusic(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val yearCol = it.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                val colors = listOf(0xFFC084FC, 0xFF38BDF8, 0xFFF43F5E, 0xFFF59E0B, 0xFF10B981, 0xFFA855F7)

                var colorIdx = 0
                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown Title"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val year = if (yearCol >= 0) it.getInt(yearCol) else 2026
                    val albumId = if (albumIdCol >= 0) it.getLong(albumIdCol) else -1L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artUri = if (albumId >= 0) {
                        "content://media/external/audio/albumart/$albumId"
                    } else null

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = if (artist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else artist,
                            album = if (album.contains("<unknown>", ignoreCase = true)) "Unknown Album" else album,
                            durationMs = duration,
                            uri = contentUri.toString(),
                            albumArtUri = artUri,
                            genre = "Local Audio",
                            year = if (year > 0) year else 2026,
                            colorHex = colors[colorIdx % colors.size],
                            isBuiltIn = false
                        )
                    )
                    colorIdx++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    suspend fun getTrackFromUri(context: Context, uri: Uri): Track? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "Imported Track"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Local Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Local Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 180000L
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "Audio"
            retriever.release()

            val id = uri.hashCode().toLong().let { if (it <= 0) -it + 10000L else it }

            Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                uri = uri.toString(),
                albumArtUri = null,
                genre = genre,
                colorHex = 0xFF38BDF8,
                isBuiltIn = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
