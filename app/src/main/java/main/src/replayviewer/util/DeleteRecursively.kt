package main.src.replayviewer.util

import java.io.File

fun deleteContentsRecursively(fileOrDirectory: File) {
    if (fileOrDirectory.isDirectory) {
        fileOrDirectory.listFiles()?.forEach { it.deleteRecursively() }
    }
}