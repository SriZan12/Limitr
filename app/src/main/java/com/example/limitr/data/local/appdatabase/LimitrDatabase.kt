package com.example.limitr.data.local.appdatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.data.local.appdatabase.model.TimeConverter

@Database(
    entities = [LimitrEntities::class],
    version = 9
)
@TypeConverters(TimeConverter::class)
abstract class LimitrDatabase: RoomDatabase() {

    abstract fun getLimitrDao(): LimitrDao
}