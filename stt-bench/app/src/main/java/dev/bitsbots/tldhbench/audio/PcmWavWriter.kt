package dev.bitsbots.tldhbench.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes the already prepared 16 kHz / mono / PCM16 stream into a minimal WAV container.
 * whisper.cpp based runners usually expect WAV/PCM input rather than raw .pcm bytes.
 */
internal object PcmWavWriter {
    fun writePcm16MonoWav(pcmFile: File, wavFile: File, sampleRate: Int = 16_000): File {
        require(pcmFile.isFile) { "PCM-Datei fehlt: ${pcmFile.absolutePath}" }
        val dataSize = pcmFile.length().coerceAtLeast(0L)
        require(dataSize > 0L) { "PCM-Datei ist leer." }
        wavFile.parentFile?.mkdirs()
        FileOutputStream(wavFile).use { output ->
            output.write(wavHeader(dataSize = dataSize, sampleRate = sampleRate, channelCount = 1, bitsPerSample = 16))
            FileInputStream(pcmFile).use { input -> input.copyTo(output) }
        }
        return wavFile
    }

    private fun wavHeader(
        dataSize: Long,
        sampleRate: Int,
        channelCount: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8
        val riffSize = (36L + dataSize).coerceAtMost(0xFFFF_FFFFL).toInt()
        val cappedDataSize = dataSize.coerceAtMost(0xFFFF_FFFFL).toInt()
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            putAscii("RIFF")
            putInt(riffSize)
            putAscii("WAVE")
            putAscii("fmt ")
            putInt(16) // PCM fmt chunk size
            putShort(1.toShort()) // PCM
            putShort(channelCount.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            putAscii("data")
            putInt(cappedDataSize)
        }.array()
    }

    private fun ByteBuffer.putAscii(value: String) {
        value.forEach { put(it.code.toByte()) }
    }
}
