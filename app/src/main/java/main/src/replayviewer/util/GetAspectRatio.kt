package main.src.replayviewer.util

import android.util.Size

fun getAspectRatio(size: Size): String {
    val gcd = gcd(size.width, size.height)
    val aspectRatio = "${size.width / gcd}:${size.height / gcd}"
    return when (aspectRatio) {
        "16:9" -> "16:9"
        "4:3" -> "4:3"
        else -> "Other"
    }
}

fun getAspectRatio(resolution: String): String {
    val (width, height) = resolution.split("x").map { it.toInt() }
    val gcd = gcd(width, height)
    val aspectRatio = "${width / gcd}:${height / gcd}"
    return when (aspectRatio) {
        "16:9" -> "16:9"
        "4:3" -> "4:3"
        else -> "Other"
    }
}