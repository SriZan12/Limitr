package com.khadka.limitr.data.local.appdatabase.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "HistoryTable")
data class AppHistoryEntities(
    @PrimaryKey val appName: String = "",
    val noOfTimesBlocked: Int? = 0,
    val appPackage: String = "",
    val blockedDate: Long? = null,
    val starTime: Long? = null,
    val endTime: Long? = null
)
