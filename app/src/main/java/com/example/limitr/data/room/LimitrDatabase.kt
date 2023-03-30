package com.example.limitr.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.limitr.data.room.model.LimitrEntities
import com.example.limitr.data.room.model.TimeConverter

@Database(
    entities = [LimitrEntities::class],
    version = 7
)
@TypeConverters(TimeConverter::class)
abstract class LimitrDatabase: RoomDatabase() {

    abstract fun getLimitrDao(): LimitrDao
}