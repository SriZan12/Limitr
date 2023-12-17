package com.khadka.limitr.data.local.appdatabase.model.limitrentities

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