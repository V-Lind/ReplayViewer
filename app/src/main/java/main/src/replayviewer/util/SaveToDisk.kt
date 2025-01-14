package main.src.replayviewer.util

import android.util.Log
import java.io.File

fun saveToDisk(
    data: ByteArray,
    directory: File,
    fileName: String
): String? {
    return runCatching {
        File(directory, fileName).apply {
            outputStream().use { stream -> stream.write(data) }
        }.absolutePath
    }.onFailure {
        Log.e("saveToDisk", "Error saving file: $fileName", it)
    }.getOrNull()
}