package main.src.replayviewer.model

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


class FrameStorageBuffer(
    size: Int,
) {
    private var bufferSize = size
    private val buffer: ArrayDeque<String> = ArrayDeque(bufferSize)
    private val sortingBuffer: ConcurrentHashMap<Int, String> = ConcurrentHashMap()
    private var expectedFrameNumber = AtomicInteger(0)

    fun getFrameCount(): Int {
        return buffer.size
    }

    fun removeFirst(): String {
        synchronized(buffer) {
            return buffer.removeFirst()
        }
    }

    fun isBufferFull(): Boolean {
        return buffer.size >= bufferSize
    }

    fun addLast(filePath: String) {
        buffer.add(filePath)
    }

    fun addLastOrdered(frameNumber: Int, filePath: String) {
        sortingBuffer[frameNumber] = filePath
        checkSortingBuffer()
    }

    private fun checkSortingBuffer() {

        while (sortingBuffer.containsKey(expectedFrameNumber.get())) {
            // Remove frame from sortingBuffer and move it to buffer
            val content = sortingBuffer.remove(expectedFrameNumber.get())

            content?.let {
                expectedFrameNumber.incrementAndGet()
                synchronized(buffer) { buffer.add(content) }
                Log.d("FrameStorageBuffer", "Added frame to buffer: $content")
            }
        }


        // Remove too old frames or frames that are leftover from failed reset
        // Keeps sortingBuffer clean of messed up frames
        val currentExpectedNumber = expectedFrameNumber.get()
        sortingBuffer.keys.forEach {
            if (it < currentExpectedNumber || it > currentExpectedNumber + 10) {
                Log.d("FrameStorageBuffer", "Removing frame from sortingBuffer: $it | Expected $currentExpectedNumber")
                sortingBuffer.remove(it)
            }
        }

        // Skip expected frame if it doesn't seem to come
        if (sortingBuffer.size > 2) {
            Log.d(
                "FrameStorageBuffer",
                "Sorter: $sortingBuffer \n| Skipping frame: $currentExpectedNumber"
            )
            expectedFrameNumber.incrementAndGet()
        }
    }

    fun toList(): List<String> {
        val bufferList: List<String>
        synchronized(buffer) {
            bufferList = buffer.toList()
        }
        return bufferList.sortedBy { it.split("/").last().toInt() }
    }

    fun reset() {
        sortingBuffer.clear()
        buffer.clear()
        expectedFrameNumber.set(0)
    }

    fun reconfigure(bufferSize: Int) {
        this.bufferSize = bufferSize
    }

}