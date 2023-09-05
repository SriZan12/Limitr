package com.khadka.limitr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.khadka.limitr.R
import com.khadka.limitr.utils.ViewUtils.getAppIconByPackageName
import timber.log.Timber

class OverlayScreen {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing: Boolean = false
    lateinit var remainingTime: TextView


    @SuppressLint("SetTextI18n")
    fun showOverlayScreen(
        appName: String,
        context: Context,
        onButtonClicked: () -> Unit,
        onExit: () -> Unit,
        appPackage: String?
    ) {
        if (!isOverlayShowing) {
            overlayView = LayoutInflater.from(context).inflate(R.layout.overlay, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            )

            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager!!.addView(overlayView, params)

            val textView: TextView = overlayView!!.findViewById(R.id.appBlock)
            textView.text = appName
            val appImage: ImageView = overlayView!!.findViewById(R.id.appIcon)

            val appIcon = getAppIconByPackageName(context = context, packageName = appPackage!!)
            appImage.setImageDrawable(appIcon)


            val button: Button = overlayView!!.findViewById(R.id.goToActivityBlocked)
            val exitButton: Button = overlayView!!.findViewById(R.id.exit)
            remainingTime = overlayView!!.findViewById(R.id.remainingTime)

            button.setOnClickListener {
                onButtonClicked()
            }

            exitButton.setOnClickListener {
                onExit()
            }

            isOverlayShowing = true
            Timber.d("INSIDE OVERLAY")
        }
    }


    fun removeOverlayView() {
        if (isOverlayShowing && overlayView != null && windowManager != null) {
            windowManager!!.removeView(overlayView)
            overlayView = null
            windowManager = null
            isOverlayShowing = false
        }
    }
}