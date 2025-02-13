package main.src.replayviewer.util

import android.util.Log
import java.io.File

fun loadFromDisk(filePath: String): ByteArray {
    val loadedData = runCatching {
        File(filePath).inputStream()
            .use { it.readBytes() }
    }.onFailure { error ->
        Log.e("LoadFromDisk", "Error loading file: $filePath", error)
        throw error
    }.getOrThrow()

    return loadedData
}