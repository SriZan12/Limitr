package com.example.limitr.data.room.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import java.sql.Time
import java.time.Instant

class TimeConverter {

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toInstant(instant: Instant): Long = instant.toEpochMilli()

}