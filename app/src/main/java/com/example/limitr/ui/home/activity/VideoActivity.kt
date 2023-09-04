package com.example.limitr.ui.home.activity

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.limitr.R
import com.example.limitr.databinding.ActivityVideoBinding
import com.example.limitr.utils.Status

class VideoActivity : AppCompatActivity() {

    private lateinit var activityVideoBinding: ActivityVideoBinding
    private var resourceId: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityVideoBinding = DataBindingUtil.setContentView(this, R.layout.activity_video)
        setContentView(activityVideoBinding.root)

        val androidVersion =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    "androidVersion",
                    Status.AndroidVersion::class.java
                ) as Status.AndroidVersion

            } else {
                intent.getSerializableExtra("androidVersion") as Status.AndroidVersion
            }

        resourceId = if (androidVersion == Status.AndroidVersion.ANDROID_13_PLUS) {
            R.raw.instructions_two
        } else {
            R.raw.instruction_one
        }
        val videoUri =
            Uri.parse("android.resource://${applicationContext.packageName}/$resourceId")

        supportActionBar?.hide()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playInstructionVideo(videoUri = videoUri)
    }

    private fun playInstructionVideo(videoUri: Uri) {
        val player = ExoPlayer.Builder(this@VideoActivity).build()
        activityVideoBinding.playerView.player = player
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

}