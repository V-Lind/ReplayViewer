package main.src.replayviewer.util

import android.content.Context
import android.util.Log
import java.io.File


fun makeDirectory(directoryName: String, context: Context): File {

    return File(context.filesDir, directoryName).apply {
        if (exists()) {
            Log.d("MakeDirectory", "Directory found; deleting : $absolutePath")
            while (!deleteRecursively()) {
                Log.d("MakeDirectory", "Failed to delete; retrying : $absolutePath")
            }
            mkdirs()
        } else {
            Log.d("MakeDirectory", "Directory does not exist. Creating: $absolutePath")
            mkdirs()
        }
    }
}