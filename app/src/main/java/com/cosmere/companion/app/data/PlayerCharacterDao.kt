package com.cosmere.companion.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerCharacterDao {
    @Query("SELECT * FROM player_character ORDER BY id")
    fun getAll(): Flow<List<PlayerCharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlayerCharacterEntity)

    @Query("DELETE FROM player_character WHERE id = :id")
    suspend fun delete(id: Int)
}
