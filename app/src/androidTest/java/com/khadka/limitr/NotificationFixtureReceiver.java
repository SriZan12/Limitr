package com.khadka.limitr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.Collections;

/** Runs in the standalone test APK, which does not contain the app's Kotlin runtime. */
public class NotificationFixtureReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if ("qa.POST".equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new NotificationChannel(
                    "qa", "LIMITR QA fixture", NotificationManager.IMPORTANCE_DEFAULT));
            }
            @SuppressWarnings("deprecation")
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, "qa") : new Notification.Builder(context);
            manager.notify(intent.getIntExtra("id", 201), builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("LIMITR QA notification").setContentText("Local test fixture")
                .setOngoing(intent.getBooleanExtra("ongoing", false)).build());
        } else if ("qa.CLEAR".equals(intent.getAction())) {
            manager.cancelAll();
        }
        ArrayList<Integer> ids = new ArrayList<>();
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            // Android 16 may also return its automatic ongoing group summary (id 0).
            // Assert only the notifications explicitly posted by this fixture.
            if (notification.getId() >= 201 && notification.getId() <= 205) ids.add(notification.getId());
        }
        Collections.sort(ids);
        StringBuilder result = new StringBuilder();
        for (int id : ids) {
            if (result.length() > 0) result.append(',');
            result.append(id);
        }
        setResultData(result.toString());
    }
}
