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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `part_trait` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `labelMasc` TEXT NOT NULL,
                `labelFem` TEXT NOT NULL,
                `labelIncl` TEXT NOT NULL,
                `source` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_part_trait_labelMasc` ON `part_trait`(`labelMasc`)"
        )
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `part_trait_link` (
                `partId` INTEGER NOT NULL,
                `traitId` INTEGER NOT NULL,
                PRIMARY KEY(`partId`, `traitId`),
                FOREIGN KEY(`partId`) REFERENCES `part`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`traitId`) REFERENCES `part_trait`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_part_trait_link_partId` ON `part_trait_link`(`partId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_part_trait_link_traitId` ON `part_trait_link`(`traitId`)"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `entry_part` (
                `entryId` INTEGER NOT NULL,
                `partId` INTEGER NOT NULL,
                `source` TEXT NOT NULL DEFAULT 'suggested',
                PRIMARY KEY(`entryId`, `partId`),
                FOREIGN KEY(`entryId`) REFERENCES `entry`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`partId`) REFERENCES `part`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_part_entryId` ON `entry_part`(`entryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_part_partId` ON `entry_part`(`partId`)")
        // session_entry_part remplacé par entry_part (couvre tous les types d'entrée)
        db.execSQL("DROP TABLE IF EXISTS `session_entry_part`")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Sur certains appareils, tag a encore DEFAULT 'manual' sur source
        // et n'a pas d'index unique — mismatch avec l'entité Tag actuelle.
        // On recrée la table proprement en préservant toutes les données.
        // Les noms d'index sont globaux dans SQLite : on supprime l'existant
        // avant de le recréer sur tag_new, pour éviter le conflit.
        db.execSQL("DROP INDEX IF EXISTS `index_tag_label`")
        db.execSQL("""
            CREATE TABLE `tag_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `label` TEXT NOT NULL,
                `source` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX `index_tag_label` ON `tag_new`(`label`)")
        // MIN(id) par label pour dédupliquer au cas où (sans casser les refs entry_tag)
        db.execSQL("""
            INSERT INTO `tag_new` (`id`, `label`, `source`)
            SELECT MIN(`id`), `label`, MIN(`source`)
            FROM `tag`
            GROUP BY LOWER(`label`)
        """.trimIndent())
        db.execSQL("""
            UPDATE `entry_tag`
            SET `tagId` = (
                SELECT MIN(`id`) FROM `tag`
                WHERE LOWER(`label`) = LOWER(
                    (SELECT `label` FROM `tag` WHERE `id` = `entry_tag`.`tagId`)
                )
            )
        """.trimIndent())
        db.execSQL("DROP TABLE `tag`")
        db.execSQL("ALTER TABLE `tag_new` RENAME TO `tag`")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recréer part_trait avec une seule colonne label (labelIncl devient label)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `part_trait_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `label` TEXT NOT NULL,
                `source` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL(
            "CREATE UNIQUE INDEX `index_part_trait_label` ON `part_trait_new`(`label`)"
        )
        // Copie des données existantes : labelIncl devient label
        db.execSQL("""
            INSERT OR IGNORE INTO `part_trait_new` (`id`, `label`, `source`)
            SELECT `id`, `labelIncl`, `source` FROM `part_trait`
        """.trimIndent())
        db.execSQL("DROP TABLE `part_trait`")
        db.execSQL("ALTER TABLE `part_trait_new` RENAME TO `part_trait`")
    }
}
