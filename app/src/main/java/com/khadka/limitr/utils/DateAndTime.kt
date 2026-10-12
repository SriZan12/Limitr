package com.khadka.limitr.utils

import java.text.SimpleDateFormat
import java.util.Locale

object DateAndTime {
    fun formatDate(date: Long?) = date?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }.orEmpty()
    fun formatTime(time: Long) = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(time)
    fun getTodayDate() = formatDate(System.currentTimeMillis())
    fun formatTimeInNumbers(millis: Long): String {
        val seconds = (millis.coerceAtLeast(0) + 999) / 1000
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    }
}
