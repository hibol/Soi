package com.hibol.miette.soi.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hibol.miette.soi.data.entity.*

@Database(
    entities = [
        Profile::class,
        Emotion::class,
        Tag::class,
        Entry::class,
        EntryEmotion::class,
        EntryTag::class,
        EntryMedia::class,
        DreamEntry::class,
        SessionEntry::class,
        EventEntry::class,
        Part::class,
        PartTrait::class,
        PartTraitLink::class,
        EntryPart::class
    ],
    version = 6,
    exportSchema = true
)
abstract class SoiDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun emotionDao(): EmotionDao
    abstract fun tagDao(): TagDao
    abstract fun entryDao(): EntryDao
    abstract fun dreamEntryDao(): DreamEntryDao
    abstract fun sessionEntryDao(): SessionEntryDao
    abstract fun eventEntryDao(): EventEntryDao
    abstract fun entryEmotionDao(): EntryEmotionDao
    abstract fun entryTagDao(): EntryTagDao
    abstract fun entryMediaDao(): EntryMediaDao
    abstract fun partDao(): PartDao
    abstract fun partTraitDao(): PartTraitDao
    abstract fun entryPartDao(): EntryPartDao

    companion object {
        @Volatile
        private var INSTANCE: SoiDatabase? = null

        fun getInstance(context: Context): SoiDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SoiDatabase::class.java,
                    "soi_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { INSTANCE = it }
            }
        }
    }
}