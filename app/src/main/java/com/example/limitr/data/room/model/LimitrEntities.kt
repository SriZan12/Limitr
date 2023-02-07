package com.example.limitr.data.room.model

import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.Instant

@Entity(tableName = "LimitrTable")
data class LimitrEntities(
    @PrimaryKey
    var appName: String,
    @TypeConverters(TimeConverter::class)
    var startTime: Long? = null,
    @TypeConverters(TimeConverter::class)
    var remainingTime: Long? = null
)
