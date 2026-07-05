package dev.bitsbots.tldhbench

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import dev.bitsbots.tldhbench.share.ShareIntentReader
import dev.bitsbots.tldhbench.share.SharedAudio
import dev.bitsbots.tldhbench.ui.BenchApp

class MainActivity : ComponentActivity() {
    private val sharedAudioState = mutableStateOf<SharedAudio?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedAudioState.value = ShareIntentReader.read(intent)
        setContent { BenchApp(sharedAudioState = sharedAudioState) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedAudioState.value = ShareIntentReader.read(intent)
    }
}
