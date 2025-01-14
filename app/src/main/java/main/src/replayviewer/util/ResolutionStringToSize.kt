package main.src.replayviewer.util

import android.util.Size

fun resolutionStringToSize(resolution: String): Size {
    val (width, height) = resolution.split("x").map { it.toInt() }
    return Size(width, height)
}