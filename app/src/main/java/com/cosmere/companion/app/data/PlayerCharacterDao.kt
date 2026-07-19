package com.cosmere.companion.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlayerCharacterDao {
    @Query("SELECT * FROM player_character WHERE id = ${PlayerCharacterEntity.CURRENT_CHARACTER_ID}")
    suspend fun getCurrent(): PlayerCharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlayerCharacterEntity)

    @Query("DELETE FROM player_character WHERE id = ${PlayerCharacterEntity.CURRENT_CHARACTER_ID}")
    suspend fun clear()
}
