package com.khadka.limitr.ui.home.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.khadka.limitr.R
import com.khadka.limitr.databinding.ActivityVideoBinding
import com.khadka.limitr.utils.Insets

class VideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoBinding
    private var player: ExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Insets.apply(binding.root)
    }
    override fun onStart() {
        super.onStart()
        player = ExoPlayer.Builder(this).build().also {
            binding.playerView.player = it
            val video = if (intent.getIntExtra("video", 2) == 1) R.raw.instruction_one else R.raw.instructions_two
            it.setMediaItem(MediaItem.fromUri("android.resource://$packageName/$video"))
            it.prepare()
        }
    }
    override fun onStop() { binding.playerView.player = null; player?.release(); player = null; super.onStop() }
}
