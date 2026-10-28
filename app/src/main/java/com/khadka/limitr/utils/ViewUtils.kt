package com.khadka.limitr.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.app.Activity
import android.content.ContextWrapper
import com.google.android.material.snackbar.Snackbar

object ViewUtils {
    fun getAppIconByPackageName(context: Context, packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) { context.packageManager.defaultActivityIcon }
    fun getAppNameByPackageName(context: Context, packageName: String): String = try {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) { packageName }
    fun showToast(context: Context, message: String, anchor: View? = null) {
        var owner = context
        while (owner is ContextWrapper && owner !is Activity) owner = owner.baseContext
        val root = anchor ?: (owner as? Activity)?.window?.decorView ?: return
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).apply {
            AcmeTypography.applyTree(view)
            view.findViewById<android.widget.TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 5
            show()
        }
    }
}
