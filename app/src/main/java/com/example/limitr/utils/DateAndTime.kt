package com.example.limitr.utils

import android.util.Log
import android.view.View
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateAndTime {

    fun getTodayDate(): String {
        val calendar = Calendar.getInstance()
        val today = Date(calendar.time.time)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return dateFormat.format(today)
    }

    fun formatTimeInWords(timeInMillis: Long): String {
        val totalSeconds = timeInMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val formattedTime = StringBuilder()

        if (hours > 0) {
            if (hours == 1L) {
                formattedTime.append("$hours hour ")
            } else {
                formattedTime.append("$hours hours ")
            }
        }

        if (minutes > 0) {
            if (minutes == 1L) {
                formattedTime.append("$minutes minute ")
            } else {
                formattedTime.append("$minutes minutes ")
            }
        }

        if (seconds > 0) {
            if (seconds == 1L) {
                formattedTime.append("$seconds second")
            } else {
                formattedTime.append("$seconds seconds")
            }

        }

        return formattedTime.toString().trimEnd()
    }

    fun formatTimeInNumbers(timeInMillis: Long): String {
        val remainingTime = timeInMillis / 1000 // convert milliseconds to seconds
        val hours = remainingTime / 3600
        val minutes = (remainingTime % 3600) / 60
        val seconds = remainingTime % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun setIntervalText(startTime: Long, endTime: Long): String {
        val timeFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val formattedEndTime = timeFormat.format(endTime)
        val formattedStartTime = timeFormat.format(startTime)

        return "$formattedStartTime-$formattedEndTime"
    }

    fun getRemainingTime(starTime: Long?, remainingTime: Long?): Long? {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - starTime!! // elapsed time means बितेको time
        return remainingTime?.minus(elapsedTime)
    }

    fun getIntervalForBlocking(
        startTime: Long?,
        endTime: Long?,
        remainingTime: Long?,
        timerText: TextView,
        intervalTextView: TextView?,
    ): Boolean {

        val currentRemainingTime = getRemainingTime(startTime, remainingTime)
        val currentTime = System.currentTimeMillis()

        if (currentRemainingTime != null && currentRemainingTime > 0) {
            intervalTextView?.text =
                setIntervalText(startTime!!, endTime!!)
            if (currentTime >= startTime &&
                currentTime <= endTime
            ) {
                timerText.visibility = View.VISIBLE
                ViewUtils.startTimer(timerText, currentRemainingTime)
            }
        } else {
            return true
        }

        return false

    }

    fun getTimer(
        starTime: Long?,
        remainingTime: Long?,
        timerText: TextView
    ): Boolean {
        val currentRemainingTime = getRemainingTime(starTime, remainingTime)
        if (currentRemainingTime != null && currentRemainingTime > 0) {
            ViewUtils.startTimer(timerText, currentRemainingTime)
        } else {
            return true
        }
        return false
    }


}