package com.example.limitr.ui.home.activity

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.limitr.R
import com.example.limitr.ui.theme.LimitrTheme
import com.example.limitr.utils.Status

class VideoActivity : AppCompatActivity() {

    private var resourceId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val androidVersion =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    "androidVersion",
                    Status.AndroidVersion::class.java,
                ) as Status.AndroidVersion
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("androidVersion") as Status.AndroidVersion
            }

        resourceId = if (androidVersion == Status.AndroidVersion.ANDROID_13_PLUS) {
            R.raw.instructions_two
        } else {
            R.raw.instruction_one
        }

        val videoUri = Uri.parse("android.resource://${applicationContext.packageName}/$resourceId")

        supportActionBar?.hide()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            LimitrTheme {
                VideoPlayerScreen(videoUri = videoUri)
            }
        }
    }
}

@Composable
private fun VideoPlayerScreen(videoUri: Uri) {
    FullscreenVideoPlayer(videoUri = videoUri)
}

@Composable
private fun FullscreenVideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                player = exoPlayer
                useController = true
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
    )
}
