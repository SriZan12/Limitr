package com.khadka.limitr.data.local.appdatabase.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.khadka.limitr.data.local.appdatabase.model.LimitrEntities
import com.khadka.limitr.data.local.appdatabase.model.TimeConverter

@Database(
    entities = [LimitrEntities::class],
    version = 10
)
@TypeConverters(TimeConverter::class)
abstract class LimitrDatabase: RoomDatabase() {

    abstract fun getLimitrDao(): LimitrDao
}