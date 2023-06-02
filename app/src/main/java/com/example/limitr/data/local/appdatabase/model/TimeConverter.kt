package com.example.limitr.data.local.appdatabase.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import java.time.Instant

class TimeConverter {

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toInstant(instant: Instant): Long = instant.toEpochMilli()

}