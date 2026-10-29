package com.khadka.limitr.data.local.appdatabase.model

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "HistoryTable", primaryKeys = ["appName", "appPackage"], indices = [Index("blockedDate")])
data class AppHistoryEntities(
    val appName: String = "",
    val noOfTimesBlocked: Int? = 0,
    val appPackage: String = "",
    val blockedDate: Long? = null,
    val starTime: Long? = null,
    val endTime: Long? = null
)
