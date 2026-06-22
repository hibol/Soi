# Soi — Journal de rêves & IFS

Application Android personnelle pour tenir un journal de rêves et de sessions thérapeutiques IFS (Internal Family Systems), avec visualisation des émotions et gestion des Parties internes. Toutes les données restent locales — pas de compte, pas de réseau.

---

## Tech Stack

| Couche | Technologie |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Single-Activity, ViewModel + StateFlow + Flow |
| Base de données | Room 2.8.4 (SQLite), offline-first |
| Navigation | Navigation Compose 2.9.8 |
| Auth locale | androidx.biometric 1.1.0 (empreinte / PIN) |
| Async | Kotlin Coroutines 1.10.2 |
| Build | Gradle (KSP pour Room) |
| Min SDK | 26 (Android 8.0) — Target 36 |

---

## Fonctionnalités

**Journal**
- Trois types d'entrées : Rêve, Session thérapeutique, Événement de vie
- Texte libre avec option de floutage individuel (toggle œil)
- Tags libres avec suggestions à la saisie
- Orientation cognitive : axe −1.0 (confusion) → +1.0 (clarté), saisie via slider, null si non renseigné
- Calendrier mensuel avec indication des jours actifs ; swipe gauche/droite pour changer de mois
- Navigation par tap sur un jour vers les entrées du jour

**Émotions**
- Catalogue basé sur le Feelings Wheel de Gloria Willcox : 7 primaires + 41 secondaires
- Roue de saisie interactive (Canvas Compose) avec sélection par segment et intensité 1–5
- Constellation émotionnelle à la lecture d'une entrée : nœuds primaires sur anneau, secondaires en orbite, taille et couleur selon l'intensité
- Mini-constellation en résumé de liste : cercles primaires seuls, taille proportionnelle à l'intensité max
- Couleurs des émotions primaires personnalisables via un color picker HSV dans les réglages

**IFS — Parties internes**
- CRUD complet : création, lecture, édition, suppression
- Fiche de lecture : nom, âge, rôle IFS, description, traits de caractère
- Traits : liste prédéfinie + saisie libre
- Lien entrée ↔ Partie : détection automatique par regex Unicode dans les textes (noms ≥ 4 caractères), confirmation ou ajout manuel depuis la vue de lecture d'une entrée
- Section "Apparitions" dans la fiche d'une Partie : entrées confirmées groupées par date

**Exploration**
- Heatmap des émotions primaires sur la période sélectionnée (7 jours / 30 jours / 3 mois) ; tap sur une cellule → tooltip avec les émotions secondaires et intensités
- Stats générales (toute la durée de vie) : nombre total d'entrées, jours actifs depuis la première entrée, streak courant, record de streak
- Stats par période : répartition par type d'entrée, moyenne par semaine, jour de semaine le plus actif, qualité de mémorisation des rêves, top tags, top Parties mentionnées
- Filtre par type d'entrée

**Recherche**
- Barre de recherche plein texte sur tous les journaux via FTS4 (SQLite), tokenizer `unicode61` — insensible aux accents et à la casse
- Recherche préfixe : "rêv" remonte "rêve", "rêveur", etc.
- 5 filtres cumulables : type d'entrée, émotion primaire (et ses secondaires), tag, Partie, période (7 j / 30 j / 3 mois)
- Résultats avec extrait contextuel surligné, défloutage au maintien pour les entrées floutées

**Réglages**
- Color picker HSV par émotion primaire, persisté en base, mis à jour de façon réactive dans toutes les vues

---

## Schéma de base de données (v11)

| Table | Description |
|---|---|
| `profile` | Profil unique local (prénom, type d'auth, hash PIN) |
| `entry` | Socle commun à tous les types d'entrée (type, date, texte, floutage, orientation) |
| `dream_entry` | Qualité de mémorisation du rêve |
| `session_entry` | Champs spécifiques aux sessions thérapeutiques |
| `event_entry` | Événements de vie |
| `entry_emotion` | Émotions d'une entrée avec intensité (1–5) |
| `entry_tag` | Tags associés à une entrée |
| `entry_media` | Médias attachés à une entrée |
| `entry_part` | Lien entrée ↔ Partie, avec source (`suggested` / `explicit`) |
| `emotion` | Catalogue d'émotions : niveau, parenté, couleur personnalisable |
| `tag` | Tags uniques créés à la volée |
| `part` | Partie IFS : nom, âge, rôle, description |
| `part_trait` | Traits de caractère (preset ou saisis librement) |
| `part_trait_link` | Liaison many-to-many Part ↔ PartTrait |
| `health_entry` | Entrées santé — medication / illness / other *(prévu V2)* |
| `part_relation` | Relation directionnelle entre deux Parties *(prévu V2)* |
| `cycle_day` | Jours de cycle menstruel |
| `entry_fts` | Index FTS4 sur `entry.text` (table virtuelle, synchronisée par triggers) |

---

## Setup local

### Prérequis

- Android Studio Iguana ou plus récent
- JDK 17+
- Android SDK avec API 36 installé

### Lancement

```bash
git clone <repo>
# Ouvrir le projet dans Android Studio
# Run > Run 'app'  (ou Shift+F10)
```

Aucune configuration réseau, aucune variable d'environnement. La base de données Room est créée automatiquement au premier lancement ; le catalogue d'émotions est inséré par `DatabaseInitializer`.

### Conventions notables

- `DatabaseInitializer.EMOTION_CATALOG_VERSION` : incrémenter à chaque modification du catalogue d'émotions pour déclencher la re-synchronisation
- `soi_prefs` SharedPreferences : `emotion_catalog_version`, `emotion_view_constellation`
