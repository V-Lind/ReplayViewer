package main.src.replayviewer.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun deleteFile(filePath: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else {
            Log.e("deleteFile", "File not found for deletion: $filePath")
        }
    }
}