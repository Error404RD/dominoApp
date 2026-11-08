package com.error404.dominoapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.error404.dominoapp.data.local.entity.HandEntity
import com.error404.dominoapp.data.local.entity.HandScoreEntity
import com.error404.dominoapp.data.local.entity.MatchEntity
import com.error404.dominoapp.data.local.entity.ParticipantEntity

/**
 * The app's database. If a table ever changes, bump [version] and add a
 * migration so saved matches are not lost.
 */
@Database(
    entities = [
        MatchEntity::class,
        ParticipantEntity::class,
        HandEntity::class,
        HandScoreEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DominoDatabase : RoomDatabase() {

    abstract fun matchDao(): MatchDao

    companion object {
        fun create(context: Context): DominoDatabase =
            Room.databaseBuilder(context, DominoDatabase::class.java, "domino.db").build()
    }
}
