package com.myra.assistant.ai

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

class AudioEngine(private val context: Context) {

    companion object {
        const val MIC_SAMPLE_RATE = 16000
        const val SPEAKER_SAMPLE_RATE = 24000
        const val CHUNK_SIZE = 1024
    }

    var onAudioChunkRecorded: ((ByteArray) -> Unit)? = null
    var onAmplitudeChanged: ((Float) -> Unit)? = null
    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingStopped: (() -> Unit)? = null

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var isSpeaking: Boolean = false
        private set

    @Volatile
    private var isRecording: Boolean = false

    @Volatile
    private var isPlaying: Boolean = false

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordJob: Job? = null
    private var playJob: Job? = null

    // IRIS-MX AEC3 acoustic echo suppression engine
    private val aecEngine = IrisAcousticEchoCancellationEngine(MIC_SAMPLE_RATE, CHUNK_SIZE / 2)
    private var lastRenderFrame: ShortArray? = null

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            MIC_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MIC_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordJob = scope.launch {
                val buffer = ByteArray(CHUNK_SIZE)
                val captureShorts = ShortArray(CHUNK_SIZE / 2)
                val processedShorts = ShortArray(CHUNK_SIZE / 2)

                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Convert byte buffer to 16-bit PCM shorts for AEC
                        val samples = read / 2
                        for (i in 0 until samples) {
                            val low = buffer[i * 2].toInt() and 0xFF
                            val high = buffer[i * 2 + 1].toInt()
                            captureShorts[i] = ((high shl 8) or low).toShort()
                        }

                        // Run IRIS-MX AEC frame processor
                        val doubleTalk = aecEngine.processAudioFrames(
                            captureShorts,
                            lastRenderFrame,
                            processedShorts,
                            isSpeaking
                        )

                        // If user starts speaking over the assistant (double-talk), trigger interruption
                        if (doubleTalk && isSpeaking) {
                            interruptAndClear()
                        }

                        // Calculate RMS Amplitude
                        val rms = calculateRms(buffer, read)
                        onAmplitudeChanged?.invoke(rms)

                        // Do NOT send mic audio while MYRA is speaking (unless double-talk) or when muted
                        if ((!isSpeaking || doubleTalk) && !isMuted) {
                            // Re-encode processed shorts back to byte array
                            val outBytes = ByteArray(samples * 2)
                            for (i in 0 until samples) {
                                val s = processedShorts[i].toInt()
                                outBytes[i * 2] = (s and 0xFF).toByte()
                                outBytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                            }
                            onAudioChunkRecorded?.invoke(outBytes)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        isRecording = false
        recordJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }

    fun startPlayback() {
        if (isPlaying) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            SPEAKER_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SPEAKER_SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(maxOf(minBufferSize, CHUNK_SIZE * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true

            playJob = scope.launch {
                while (isActive && isPlaying) {
                    val chunk = audioQueue.poll()
                    if (chunk != null) {
                        if (!isSpeaking) {
                            isSpeaking = true
                            onSpeakingStarted?.invoke()
                        }
                        audioTrack?.write(chunk, 0, chunk.size)
                    } else {
                        if (isSpeaking) {
                            isSpeaking = false
                            onSpeakingStopped?.invoke()
                        }
                        kotlinx.coroutines.delay(10)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun queueAudio(pcmBytes: ByteArray) {
        audioQueue.add(pcmBytes)
    }

    fun interruptAndClear() {
        audioQueue.clear()
        if (isSpeaking) {
            isSpeaking = false
            onSpeakingStopped?.invoke()
        }
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateRms(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        val shortCount = length / 2
        if (shortCount == 0) return 0f

        for (i in 0 until length - 1 step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val shortVal = sample.toShort()
            sum += shortVal * shortVal
        }
        val rms = sqrt(sum / shortCount)
        return (rms / 32768.0).toFloat().coerceIn(0f, 1f)
    }

    fun release() {
        stopRecording()
        isPlaying = false
        playJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
        audioQueue.clear()
    }
}
