package com.example.limitr.data.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "LimitrTable")
data class LimitrEntities(
    @PrimaryKey
    var appName: String,
    @TypeConverters(TimeConverter::class)
    var blockedTime: Long? = null,
    @TypeConverters(TimeConverter::class)
    var remainingTime: Long? = null,
    var appPackage: String? = null,
    @TypeConverters(TimeConverter::class)
    var starTime: Long? = null,
    @TypeConverters(TimeConverter::class)
    var endTime: Long? = null,
    var notificationStatus: Boolean? = null
)
