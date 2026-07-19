package com.cosmere.companion.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PlayerCharacterEntity::class], version = 4, exportSchema = true)
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
                )
                    // No migration path exists yet for this pre-release schema;
                    // the single stored row is disposable.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
    }
}
