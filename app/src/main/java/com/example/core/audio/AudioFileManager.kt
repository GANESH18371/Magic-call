package com.example.core.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AudioFileManager"

data class SavedAudioItem(
    val id: String,
    val title: String,
    val effectType: AudioEffectType,
    val filePath: String,
    val durationMs: Long,
    val timestamp: Long,
    val fileSizeFormatted: String
)

class AudioFileManager(private val context: Context) {
    private val storageDir = File(context.filesDir, "saved_recordings").apply {
        if (!exists()) mkdirs()
    }
    private val metaFile = File(storageDir, "meta_recordings.json")

    private val _savedRecordings = MutableStateFlow<List<SavedAudioItem>>(emptyList())
    val savedRecordings: StateFlow<List<SavedAudioItem>> = _savedRecordings.asStateFlow()

    init {
        loadRecordings()
    }

    fun loadRecordings() {
        val items = mutableListOf<SavedAudioItem>()
        if (metaFile.exists()) {
            try {
                val jsonString = metaFile.readText()
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val path = obj.getString("filePath")
                    val file = File(path)
                    if (file.exists()) {
                        items.add(
                            SavedAudioItem(
                                id = obj.getString("id"),
                                title = obj.getString("title"),
                                effectType = AudioEffectType.fromId(obj.getString("effectType")),
                                filePath = path,
                                durationMs = obj.optLong("durationMs", 0L),
                                timestamp = obj.optLong("timestamp", file.lastModified()),
                                fileSizeFormatted = formatFileSize(file.length())
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading metadata", e)
            }
        }
        // Also check any orphan audio files in directory
        storageDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".m4a") && items.none { it.filePath == file.absolutePath }) {
                items.add(
                    SavedAudioItem(
                        id = file.nameWithoutExtension,
                        title = "Recording ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}",
                        effectType = AudioEffectType.ORIGINAL,
                        filePath = file.absolutePath,
                        durationMs = 0L,
                        timestamp = file.lastModified(),
                        fileSizeFormatted = formatFileSize(file.length())
                    )
                )
            }
        }

        _savedRecordings.value = items.sortedByDescending { it.timestamp }
    }

    suspend fun saveRecording(
        sourceFile: File,
        customTitle: String,
        effectType: AudioEffectType,
        durationMs: Long
    ): SavedAudioItem? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null

        val id = "magic_${System.currentTimeMillis()}"
        val destFile = File(storageDir, "$id.m4a")
        try {
            sourceFile.copyTo(destFile, overwrite = true)
            val title = customTitle.ifBlank {
                "${effectType.title} - ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}"
            }
            val newItem = SavedAudioItem(
                id = id,
                title = title,
                effectType = effectType,
                filePath = destFile.absolutePath,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis(),
                fileSizeFormatted = formatFileSize(destFile.length())
            )

            val updatedList = listOf(newItem) + _savedRecordings.value
            _savedRecordings.value = updatedList
            persistMetadata(updatedList)
            return@withContext newItem
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recording", e)
            return@withContext null
        }
    }

    suspend fun importAudio(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}.m4a")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                return@withContext destFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import audio", e)
        }
        return@withContext null
    }

    suspend fun deleteRecording(item: SavedAudioItem) = withContext(Dispatchers.IO) {
        val file = File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
        val updated = _savedRecordings.value.filter { it.id != item.id }
        _savedRecordings.value = updated
        persistMetadata(updated)
    }

    fun shareRecording(item: SavedAudioItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) return

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Listen to my voice transformed with Magic Voice AI (${item.effectType.title})!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Magic Voice Audio").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share recording", e)
        }
    }

    private fun persistMetadata(items: List<SavedAudioItem>) {
        try {
            val array = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("effectType", item.effectType.id)
                    put("filePath", item.filePath)
                    put("durationMs", item.durationMs)
                    put("timestamp", item.timestamp)
                }
                array.put(obj)
            }
            metaFile.writeText(array.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist meta", e)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(Locale.US, "%.1f MB", bytes.toFloat() / (1024 * 1024))
        }
    }
}
