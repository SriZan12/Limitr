package com.example.limitr.utils

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import com.example.limitr.utils.DateAndTime.formatTimeInNumbers
import com.example.limitr.utils.DateAndTime.formatTimeInWords

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