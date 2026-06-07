package com.hibol.miette.soi.data.db

import android.content.Context
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.PartTrait

object DatabaseInitializer {

    // Incrémenter à chaque modification du catalogue correspondant
    private const val EMOTION_CATALOG_VERSION = 3
    private const val TRAIT_CATALOG_VERSION = 2  // v2 : passage à colonne label unique
    private const val PREFS_NAME = "soi_prefs"
    private const val PREFS_KEY_EMOTION_VERSION = "emotion_catalog_version"
    private const val PREFS_KEY_TRAIT_VERSION = "part_trait_catalog_version"

    suspend fun sync(db: SoiDatabase, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getInt(PREFS_KEY_EMOTION_VERSION, 0) != EMOTION_CATALOG_VERSION) {
            doSync(db)
            prefs.edit().putInt(PREFS_KEY_EMOTION_VERSION, EMOTION_CATALOG_VERSION).apply()
        }

        if (prefs.getInt(PREFS_KEY_TRAIT_VERSION, 0) != TRAIT_CATALOG_VERSION) {
            doSyncTraits(db)
            prefs.edit().putInt(PREFS_KEY_TRAIT_VERSION, TRAIT_CATALOG_VERSION).apply()
        }
    }

    private suspend fun doSync(db: SoiDatabase) {
        val emotionDao = db.emotionDao()
        val entryEmotionDao = db.entryEmotionDao()

        val existing = emotionDao.getAllOnce()
        val existingPrimariesByLabel = existing.filter { it.level == 1 }.associateBy { it.label }
        // Clé : (label secondaire, label du parent) → permet de distinguer les homonymes inter-catégories
        val existingSecondariesByKey = existing
            .filter { it.level == 2 }
            .associateBy { sec ->
                val parentLabel = existingPrimariesByLabel.values
                    .find { it.id == sec.parentId }?.label ?: ""
                sec.label to parentLabel
            }

        val retainedIds = mutableSetOf<Long>()
        val newPrimaryIds = mutableMapOf<String, Long>()
        val catalog = buildCatalog()

        // Phase 1 : sync des émotions primaires
        for (entry in catalog) {
            val existingPrimary = existingPrimariesByLabel[entry.label]
            if (existingPrimary != null) {
                val updated = existingPrimary.copy(color = entry.color, valence = entry.valence)
                if (updated != existingPrimary) emotionDao.update(updated)
                newPrimaryIds[entry.label] = existingPrimary.id
                retainedIds.add(existingPrimary.id)
            } else {
                val id = emotionDao.insert(
                    Emotion(id = 0, label = entry.label, level = 1, parentId = null,
                            valence = entry.valence, arousal = null, color = entry.color)
                )
                newPrimaryIds[entry.label] = id
                retainedIds.add(id)
            }
        }

        // Phase 2 : sync des émotions secondaires
        for (entry in catalog) {
            val parentId = newPrimaryIds[entry.label]!!
            for (childLabel in entry.children) {
                val key = childLabel to entry.label
                val existingSec = existingSecondariesByKey[key]
                if (existingSec != null) {
                    val updated = existingSec.copy(parentId = parentId, color = entry.color,
                                                   valence = entry.valence)
                    if (updated != existingSec) emotionDao.update(updated)
                    retainedIds.add(existingSec.id)
                } else {
                    val id = emotionDao.insert(
                        Emotion(id = 0, label = childLabel, level = 2, parentId = parentId,
                                valence = entry.valence, arousal = null, color = entry.color)
                    )
                    retainedIds.add(id)
                }
            }
        }

        // Phase 3 : suppression des émotions absentes de la nouvelle liste
        // Les secondaires d'abord : la FK self-référentielle (parentId) bloque si on supprime
        // une primaire alors que ses secondaires existent encore
        val toDelete = existing.filter { it.id !in retainedIds }.sortedByDescending { it.level }
        for (emotion in toDelete) {
            entryEmotionDao.deleteByEmotionId(emotion.id)
            emotionDao.deleteById(emotion.id)
        }
    }

    private suspend fun doSyncTraits(db: SoiDatabase) {
        val dao = db.partTraitDao()
        // INSERT OR IGNORE : les traits existants (même label) ne sont jamais écrasés
        for (trait in buildTraitCatalog()) {
            dao.insert(trait)
        }
    }

    private fun buildTraitCatalog(): List<PartTrait> = listOf(
        // Protection / contrôle
        PartTrait(label = "hypervigilant·e"),
        PartTrait(label = "contrôlant·e"),
        PartTrait(label = "perfectionniste"),
        PartTrait(label = "rigide"),
        PartTrait(label = "méfiant·e"),
        PartTrait(label = "prudent·e"),
        PartTrait(label = "évitant·e"),
        PartTrait(label = "replié·e"),
        PartTrait(label = "dissocié·e"),
        PartTrait(label = "rationnel·le"),
        PartTrait(label = "intellectuel·le"),
        // Impulsivité / réactivité
        PartTrait(label = "impulsif·ve"),
        PartTrait(label = "réactif·ve"),
        PartTrait(label = "explosif·ve"),
        PartTrait(label = "agité·e"),
        PartTrait(label = "téméraire"),
        // Relation aux autres
        PartTrait(label = "dépendant·e"),
        PartTrait(label = "fusionnel·le"),
        PartTrait(label = "isolé·e"),
        PartTrait(label = "séducteur·rice"),
        PartTrait(label = "loyal·e"),
        PartTrait(label = "généreux·se"),
        PartTrait(label = "empathique"),
        PartTrait(label = "sensible"),
        PartTrait(label = "discret·e"),
        PartTrait(label = "invisible"),
        // Rapport à soi
        PartTrait(label = "honteux·se"),
        PartTrait(label = "coupable"),
        PartTrait(label = "auto-saboteur·se"),
        PartTrait(label = "autocritique"),
        PartTrait(label = "grandiose"),
        PartTrait(label = "fier·ère"),
        PartTrait(label = "authentique"),
        // Ressources
        PartTrait(label = "créatif·ve"),
        PartTrait(label = "curieux·se"),
        PartTrait(label = "courageux·se"),
        PartTrait(label = "humoristique"),
        PartTrait(label = "résilient·e"),
        PartTrait(label = "intuitif·ve"),
        PartTrait(label = "adaptable"),
        PartTrait(label = "persévérant·e"),
        PartTrait(label = "doux·ce"),
        PartTrait(label = "tendre"),
        PartTrait(label = "sage"),
        PartTrait(label = "ancré·e"),
        // Valeurs
        PartTrait(label = "sécurisant·e"),
        PartTrait(label = "autonome"),
        PartTrait(label = "connecté·e"),
        PartTrait(label = "libre"),
        PartTrait(label = "juste"),
        PartTrait(label = "reconnaissant·e")
    )

    private data class PrimaryEntry(
        val label: String,
        val color: String,
        val valence: Double?,
        val children: List<String>
    )

    private fun buildCatalog(): List<PrimaryEntry> = listOf(
        PrimaryEntry("Joie", "#F9C74F", 1.0, listOf(
            "Amour", "Tendresse", "Gratitude", "Reconnaissance",
            "Admiration", "Émerveillement", "Amusement", "Excitation",
            "Délice", "Satisfaction", "Contentement",
            "Sérénité", "Plénitude", "Présence", "Harmonie",
            "Inspiration", "Espoir", "Fierté", "Encouragement",
            "Soulagement", "Légèreté", "Valorisation"
        )),
        PrimaryEntry("Surprise", "#43AA8B", 0.0, listOf(
            "Étonnement", "Stupéfaction", "Incrédulité",
            "Choc", "Curiosité", "Fascination", "Intrigue"
        )),
        PrimaryEntry("Peur", "#6D597A", -1.0, listOf(
            "Anxiété", "Inquiétude", "Nervosité", "Appréhension",
            "Méfiance", "Insécurité", "Gêne",
            "Effroi", "Terreur", "Horreur"
        )),
        PrimaryEntry("Tristesse", "#4895EF", -1.0, listOf(
            "Souffrance", "Mélancolie", "Désespoir", "Solitude",
            "Dépression", "Apathie", "Vide", "Fatigue émotionnelle",
            "Découragement", "Perte", "Deuil",
            "Honte", "Culpabilité", "Remords",
            "Infériorité", "Inadéquation"
        )),
        PrimaryEntry("Colère", "#F94144", -1.0, listOf(
            "Agacement", "Irritation", "Exaspération",
            "Frustration", "Rancœur", "Hostilité",
            "Rage", "Haine",
            "Injustice", "Indignation", "Révolte",
            "Jalousie", "Mépris", "Condescendance"
        )),
        PrimaryEntry("Dégoût", "#7A9E7E", -1.0, listOf(
            "Répulsion", "Écœurement", "Aversion",
            "Détestation", "Dédain", "Rejet",
            "Dégoût de soi"
        )),
        PrimaryEntry("Orientation", "#577590", 0.0, listOf(
            "Clarté", "Compréhension", "Lucidité",
            "Confusion", "Doute", "Indécision",
            "Perte de repères"
        ))
    )
}
