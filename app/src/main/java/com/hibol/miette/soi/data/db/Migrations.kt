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

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Ajouter le champ orientation à entry
        db.execSQL("ALTER TABLE `entry` ADD COLUMN `orientation` REAL")

        // 2. Calculer l'orientation depuis les entry_emotion liées au groupe "Orientation".
        //    Positif : Clarté, Compréhension, Lucidité → +1.0
        //    Négatif : Confusion, Doute, Indécision, Perte de repères → -1.0
        //    Formule : somme_pondérée / total_intensité  → toujours dans [-1, +1]
        db.execSQL("""
            UPDATE `entry` SET `orientation` = (
                SELECT CAST(
                    SUM(ee.intensity * CASE
                        WHEN em.label IN ('Clarté', 'Compréhension', 'Lucidité') THEN 1.0
                        ELSE -1.0
                    END)
                AS REAL) / SUM(ee.intensity)
                FROM entry_emotion ee
                JOIN emotion em ON ee.emotionId = em.id
                WHERE ee.entryId = entry.id
                  AND em.label IN ('Clarté', 'Compréhension', 'Lucidité',
                                   'Confusion', 'Doute', 'Indécision', 'Perte de repères')
            )
            WHERE EXISTS (
                SELECT 1 FROM entry_emotion ee
                JOIN emotion em ON ee.emotionId = em.id
                WHERE ee.entryId = entry.id
                  AND em.label IN ('Clarté', 'Compréhension', 'Lucidité',
                                   'Confusion', 'Doute', 'Indécision', 'Perte de repères')
            )
        """.trimIndent())

        // 3. Supprimer les entry_emotion liées à toutes les émotions Orientation
        db.execSQL("""
            DELETE FROM `entry_emotion`
            WHERE emotionId IN (
                SELECT id FROM `emotion`
                WHERE label IN ('Orientation', 'Clarté', 'Compréhension', 'Lucidité',
                                'Confusion', 'Doute', 'Indécision', 'Perte de repères')
            )
        """.trimIndent())

        // 4. Supprimer les secondaires avant la primaire (contrainte FK parentId)
        db.execSQL("""
            DELETE FROM `emotion`
            WHERE label IN ('Clarté', 'Compréhension', 'Lucidité',
                            'Confusion', 'Doute', 'Indécision', 'Perte de repères')
        """.trimIndent())
        db.execSQL("DELETE FROM `emotion` WHERE label = 'Orientation' AND level = 1")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cycle_day` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `profileId` INTEGER NOT NULL,
                `epochDay` INTEGER NOT NULL,
                FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cycle_day_profileId` ON `cycle_day`(`profileId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cycle_day_profileId_epochDay` ON `cycle_day`(`profileId`, `epochDay`)")
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
