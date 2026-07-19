package com.cosmere.companion.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PlayerCharacterEntity::class], version = 1, exportSchema = true)
@TypeConverters(MapConverters::class)
abstract class CompanionDatabase : RoomDatabase() {
    abstract fun playerCharacterDao(): PlayerCharacterDao

    companion object {
        @Volatile
        private var instance: CompanionDatabase? = null

        fun get(context: Context): CompanionDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CompanionDatabase::class.java,
                    "cosmere-companion.db",
                ).build().also { instance = it }
            }
    }
}
