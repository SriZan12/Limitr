package com.khadka.limitr.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast

object ViewUtils {
    fun getAppIconByPackageName(context: Context, packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) { context.packageManager.defaultActivityIcon }
    fun getAppNameByPackageName(context: Context, packageName: String): String = try {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) { packageName }
    fun showToast(context: Context, message: String) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    fun animateProgressBar(progressBar: ProgressBar, currentProgress: Int) { progressBar.progress = currentProgress }
    fun setFadeInAnimation(view: View, position: Int) { view.clearAnimation() }
}
