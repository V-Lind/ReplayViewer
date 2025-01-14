package main.src.replayviewer.util

import android.content.Context
import android.widget.Toast
import java.io.File

// Cleans up all images stored by this app
fun clearStorage(context: Context) {
    File(context.filesDir, "mediaPlayerFrames").deleteRecursively()
    File(context.filesDir, "streamFrames").deleteRecursively()


    Toast.makeText(context, "Storage cleared", Toast.LENGTH_SHORT).show()
}