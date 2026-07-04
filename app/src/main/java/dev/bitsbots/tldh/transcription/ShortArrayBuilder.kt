package dev.bitsbots.tldh.transcription

internal class ShortArrayBuilder(initialCapacity: Int = 16_384) {
    private var data = ShortArray(initialCapacity)
    var size: Int = 0
        private set

    fun add(value: Short) {
        ensureCapacity(size + 1)
        data[size++] = value
    }

    fun toArray(): ShortArray = data.copyOf(size)

    private fun ensureCapacity(required: Int) {
        if (required <= data.size) return
        var next = data.size
        while (next < required) next *= 2
        data = data.copyOf(next)
    }
}
