package com.khadka.limitr.data.local.appdatabase.room.appblock

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.khadka.limitr.data.local.appdatabase.model.historyentities.AppHistoryEntities
import com.khadka.limitr.data.local.appdatabase.model.limitrentities.LimitrEntities
import com.khadka.limitr.data.local.appdatabase.model.limitrentities.TimeConverter

@Database(
    entities = [LimitrEntities::class, AppHistoryEntities::class],
    version = 12
)
@TypeConverters(TimeConverter::class)
abstract class LimitrDatabase : RoomDatabase() {

    abstract fun getLimitrDao(): LimitrDao
}