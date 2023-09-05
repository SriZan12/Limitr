package com.khadka.limitr.utils

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import com.khadka.limitr.utils.DateAndTime.formatTimeInNumbers

class TimerClass(private val timerText: TextView, duration: Long) : CountDownTimer(
    duration,
    1000
) {

    @SuppressLint("SetTextI18n")
    override fun onTick(millisUntilFinished: Long) {

        val time = formatTimeInNumbers(millisUntilFinished)

        timerText.visibility = View.VISIBLE
        timerText.text = time
    }

    @SuppressLint("SetTextI18n")
    override fun onFinish() {

    }

}