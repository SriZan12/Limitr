package com.example.limitr.ui.blocker

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView

class TimerClass(private val timerText: TextView, duration: Long) : CountDownTimer(
    duration,
    1000
) {


    @SuppressLint("SetTextI18n")
    override fun onTick(millisUntilFinished: Long) {

        val remainingTime = millisUntilFinished / 1000 // convert milliseconds to seconds
        val hours = remainingTime / 3600
        val minutes = (remainingTime % 3600) / 60
        val seconds = remainingTime % 60

        val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        timerText.visibility = View.VISIBLE
        timerText.text = "Duration: $time"
    }

    @SuppressLint("SetTextI18n")
    override fun onFinish() {

    }

}