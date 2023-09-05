package com.example.limitr.ui.home.activity

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.limitr.R
import com.example.limitr.databinding.ActivityVideoBinding
import com.example.limitr.utils.Status
import com.example.limitr.utils.ViewUtils.showToast
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class VideoActivity : AppCompatActivity() {

    private lateinit var activityVideoBinding: ActivityVideoBinding
    private var resourceId: String = ""
    private var storage = FirebaseStorage.getInstance()
    private var storageReference = storage.reference
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
            "instructions_video/instructions_two.mp4"
        } else {
            "instructions_video/instruction_one.mp4"
        }

        supportActionBar?.hide()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        downloadInstructionsVideo(resourceId)

    }

    private fun playInstructionVideo(videoUri: Uri) {
        val player = ExoPlayer.Builder(this@VideoActivity).build()
        activityVideoBinding.playerView.player = player
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun downloadInstructionsVideo(childRef: String) {
        val instruction_oneRed: StorageReference =
            storageReference.child(childRef)
        instruction_oneRed.downloadUrl.addOnSuccessListener { uri ->
            playInstructionVideo(videoUri = uri)
            activityVideoBinding.progressBar.isVisible = false
        }.addOnFailureListener {
            showToast(this@VideoActivity, "Check Internet Connection!")
            finish()
        }

    }

}