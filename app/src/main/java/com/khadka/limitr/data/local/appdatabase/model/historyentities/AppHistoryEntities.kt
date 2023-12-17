package com.khadka.limitr.data.local.appdatabase.model.historyentities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.khadka.limitr.data.local.appdatabase.model.limitrentities.TimeConverter
@Entity(tableName = "HistoryTable")
data class AppHistoryEntities(
    @PrimaryKey
    val appName: String = "",
    val noOfTimesBlocked: Int? = 0,
    val appPackage: String = "",
    @TypeConverters(TimeConverter::class)
    var blockedDate: Long? = null,
    @TypeConverters(TimeConverter::class)
    var starTime: Long? = null,
    @TypeConverters(TimeConverter::class)
    var endTime: Long? = null
)
