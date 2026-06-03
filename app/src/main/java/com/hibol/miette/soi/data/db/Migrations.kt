package com.hibol.miette.soi.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `entry` ADD COLUMN `isBlurred` INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recréer tag sans DEFAULT sql sur source (Room stocke 'undefined' quand pas de défaut SQL)
        // L'index unique est créé séparément avec le nom que Room attend : index_tag_label
        db.execSQL("""
            CREATE TABLE tag_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                label TEXT NOT NULL,
                source TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX index_tag_label ON tag_new(label)")

        // Insérer les tags dédupliqués : le plus ancien id par label survit
        db.execSQL("""
            INSERT INTO tag_new (id, label, source)
            SELECT id, label, source FROM tag
            WHERE id IN (SELECT MIN(id) FROM tag GROUP BY LOWER(label))
        """.trimIndent())

        // Rediriger entry_tag vers les ids survivants avant de supprimer les doublons
        db.execSQL("""
            UPDATE entry_tag
            SET tagId = (
                SELECT MIN(id) FROM tag
                WHERE LOWER(label) = LOWER((SELECT label FROM tag WHERE id = entry_tag.tagId))
            )
        """.trimIndent())

        db.execSQL("DROP TABLE tag")
        db.execSQL("ALTER TABLE tag_new RENAME TO tag")
    }
}
