package com.khadka.limitr.data.local.appdatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.khadka.limitr.data.local.appdatabase.model.LimitrEntities
import com.khadka.limitr.data.local.appdatabase.model.TimeConverter
import com.khadka.limitr.data.local.appdatabase.model.AppHistoryEntities
import com.khadka.limitr.blocking.BlockingSession
import com.khadka.limitr.blocking.SessionDao

@Database(
    entities = [LimitrEntities::class, AppHistoryEntities::class, BlockingSession::class],
    version = 14,
    exportSchema = true
)
@TypeConverters(TimeConverter::class)
abstract class LimitrDatabase: RoomDatabase() {

    abstract fun getLimitrDao(): LimitrDao
    abstract fun sessions(): SessionDao
}
