package main.src.replayviewer.util

fun gcd(a: Int, b: Int): Int {
    return if (b == 0) a else gcd(b, a % b)
}