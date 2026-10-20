package com.edu.unal.tictactoe

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Move sounds, falling back to MediaPlayer while SoundPool is still loading. */
class GameSounds(private val context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val humanSoundId = soundPool.load(context, R.raw.human_sound, 1)
    private val computerSoundId = soundPool.load(context, R.raw.computer_sound, 1)

    fun playHuman() = play(humanSoundId, R.raw.human_sound)

    fun playComputer() = play(computerSoundId, R.raw.computer_sound)

    fun release() = soundPool.release()

    private fun play(soundId: Int, resId: Int) {
        val streamId = if (soundId != 0) soundPool.play(soundId, 1f, 1f, 1, 0, 1f) else 0
        if (streamId == 0) {
            MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        }
    }
}

@Composable
fun rememberGameSounds(): GameSounds {
    val context = LocalContext.current
    val sounds = remember(context) { GameSounds(context) }
    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }
    return sounds
}
