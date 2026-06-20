package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.data.entity.GlobalStats
import com.hibol.miette.soi.data.entity.MemoryQualityCount
import com.hibol.miette.soi.data.entity.PartCount
import com.hibol.miette.soi.data.entity.PeriodStats
import com.hibol.miette.soi.data.entity.TagCount
import com.hibol.miette.soi.ui.components.hexToHsl
import com.hibol.miette.soi.ui.navigation.MainBottomBar
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import com.hibol.miette.soi.ui.viewmodel.ExplorationViewModel
import com.hibol.miette.soi.ui.viewmodel.monthFr
import com.hibol.miette.soi.ui.viewmodel.HeatmapUiState
import com.hibol.miette.soi.ui.viewmodel.Period
import com.hibol.miette.soi.ui.viewmodel.SelectedCell
import com.hibol.miette.soi.ui.viewmodel.TooltipUiState
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorationScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: ExplorationViewModel = viewModel(
        factory = ExplorationViewModel.Factory(
            app.container.profileRepository,
            app.container.entryRepository,
            app.container.emotionRepository,
            app.container.partRepository,
            app.container.cycleDayRepository
        )
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val global by viewModel.globalStats.collectAsState()
    val period by viewModel.periodStats.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Exploration") }) },
        bottomBar = { MainBottomBar(currentRoute = currentRoute, navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bloc global — indépendant de la période
            global?.let { GlobalStatsCard(it) }

            // Sélecteur de période — régit tout ce qui suit
            PeriodSelector(selected = selectedPeriod, onSelect = viewModel::selectPeriod)

            // Répartition (avant la heatmap)
            period?.let { PeriodStatsSectionTop(it) }

            // Heatmap (filtrée par période + type)
            Text(
                "Carte des émotions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            ExplorationHeatmap(viewModel = viewModel)

            // Rêves, tags, parties (après la heatmap)
            period?.let { PeriodStatsSectionBottom(it) }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Widget heatmap ───────────────────────────────────────────────────────────

@Composable
fun ExplorationHeatmap(viewModel: ExplorationViewModel) {
    val heatmapState by viewModel.heatmapUiState.collectAsState()
    val selectedTypes by viewModel.selectedEntryTypes.collectAsState()
    val tooltipState by viewModel.tooltipUiState.collectAsState()
    val selectedCell by viewModel.selectedCell.collectAsState()
    val cycleDays by viewModel.heatmapCycleDays.collectAsState()
    var tooltipAnchor by remember { mutableStateOf(IntOffset.Zero) }

    Column(modifier = Modifier.fillMaxWidth()) {
        EntryTypeChips(selected = selectedTypes, onToggle = viewModel::toggleEntryType)
        Spacer(modifier = Modifier.height(12.dp))
        when (val state = heatmapState) {
            is HeatmapUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is HeatmapUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune émotion enregistrée sur cette période.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is HeatmapUiState.Ready -> {
                HeatmapGrid(
                    state = state,
                    cycleDays = cycleDays,
                    selectedCell = selectedCell,
                    onCellPress = { col, emotionId, offset ->
                        tooltipAnchor = offset
                        viewModel.pressCell(col, emotionId)
                    },
                    onCellRelease = viewModel::releaseCell
                )
                val tooltip = tooltipState
                if (tooltip is TooltipUiState.Visible) {
                    val anchor = tooltipAnchor
                    Popup(
                        popupPositionProvider = remember(anchor) {
                            object : PopupPositionProvider {
                                override fun calculatePosition(
                                    anchorBounds: IntRect,
                                    windowSize: IntSize,
                                    layoutDirection: LayoutDirection,
                                    popupContentSize: IntSize
                                ): IntOffset {
                                    val y = (anchor.y + 8)
                                        .coerceAtMost(windowSize.height - popupContentSize.height - 16)
                                    return IntOffset(x = 0, y = y)
                                }
                            }
                        }
                    ) {
                        TooltipCard(tooltip)
                    }
                }
            }
        }
    }
}

// ─── Chips filtre type d'entrée ───────────────────────────────────────────────

@Composable
private fun EntryTypeChips(selected: Set<EntryType>, onToggle: (EntryType) -> Unit) {
    val labels = remember {
        listOf(
            EntryType.DREAM to "Rêves",
            EntryType.SESSION to "Sessions",
            EntryType.LIFE_EVENT to "Événements"
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { (type, label) ->
            FilterChip(
                selected = type in selected,
                onClick = { onToggle(type) },
                label = { Text(label) }
            )
        }
    }
}

// ─── Sélecteur de période ─────────────────────────────────────────────────────

@Composable
private fun PeriodSelector(selected: Period, onSelect: (Period) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(period.label) }
            )
        }
    }
}

// ─── Grille heatmap ───────────────────────────────────────────────────────────

@Composable
private fun HeatmapGrid(
    state: HeatmapUiState.Ready,
    cycleDays: Set<java.time.LocalDate>,
    selectedCell: SelectedCell?,
    onCellPress: (col: Int, emotionId: Long, offset: IntOffset) -> Unit,
    onCellRelease: () -> Unit
) {
    val labelColWidth = 68.dp
    val cellHeight = 32.dp
    val headerHeight = 32.dp  // 2 lignes × 16dp
    val footerHeight = 10.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidth: Dp = if (state.period == Period.MONTH) 30.dp
                            else (maxWidth - labelColWidth) / state.columnCount

        Row(modifier = Modifier.fillMaxWidth()) {

            // Y-axis : noms des émotions
            Column(modifier = Modifier.width(labelColWidth)) {
                Spacer(modifier = Modifier.height(headerHeight))
                state.primaryEmotions.forEach { emotion ->
                    Box(
                        modifier = Modifier
                            .height(cellHeight + 2.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = emotion.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(footerHeight))
            }

            // Grille (scrollable pour 30j)
            val gridModifier = Modifier.weight(1f).let {
                if (state.period == Period.MONTH) it.horizontalScroll(rememberScrollState()) else it
            }

            Row(modifier = gridModifier) {
                for (col in 0 until state.columnCount) {
                    val isCycleCol = when (state.period) {
                        Period.THREE_MONTHS -> (0..6).any {
                            state.startDate.plusDays(col * 7L + it) in cycleDays
                        }
                        else -> state.startDate.plusDays(col.toLong()) in cycleDays
                    }
                    Column(modifier = Modifier.width(cellWidth)) {

                        // Header X : ligne 1 (lettre/mois) + ligne 2 (chiffre)
                        val (topLabel, bottomLabel) = buildXLabels(state, col)
                        val labelStyle = MaterialTheme.typography.labelSmall
                        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        Column(
                            modifier = Modifier.height(headerHeight).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (topLabel.isNotEmpty())
                                    Text(topLabel, style = labelStyle, color = labelColor, maxLines = 1)
                            }
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bottomLabel.isNotEmpty())
                                    Text(bottomLabel, style = labelStyle, color = labelColor, maxLines = 1)
                            }
                        }

                        // Cellules
                        state.primaryEmotions.forEach { emotion ->
                            val isSelected = selectedCell?.col == col &&
                                selectedCell.primaryEmotionId == emotion.id
                            HeatmapCell(
                                col = col,
                                emotionId = emotion.id,
                                emotionColor = emotion.color,
                                intensity = state.cells[Pair(col, emotion.id)],
                                cellWidth = cellWidth,
                                cellHeight = cellHeight,
                                isSelected = isSelected,
                                onPress = { c, e, offset -> onCellPress(c, e, offset) },
                                onRelease = onCellRelease
                            )
                        }

                        // Footer : goutte cycle
                        Box(
                            modifier = Modifier.height(footerHeight).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCycleCol) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    col: Int,
    emotionId: Long,
    emotionColor: String,
    intensity: Float?,
    cellWidth: Dp,
    cellHeight: Dp,
    isSelected: Boolean,
    onPress: (col: Int, emotionId: Long, offset: IntOffset) -> Unit,
    onRelease: () -> Unit
) {
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val computed = remember(emotionColor, intensity) {
        if (intensity != null) colorForIntensityFloat(hexToHsl(emotionColor), intensity) else null
    }
    val baseColor = computed ?: emptyColor
    val displayColor = if (isSelected) baseColor.copy(alpha = (baseColor.alpha * 0.55f).coerceAtLeast(0.12f))
                       else baseColor
    var cellBottom by remember { mutableStateOf(IntOffset.Zero) }
    Box(
        modifier = Modifier
            .width(cellWidth)
            .height(cellHeight)
            .padding(1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(displayColor)
            .onGloballyPositioned { coords ->
                val tl = coords.localToWindow(Offset.Zero)
                cellBottom = IntOffset(tl.x.toInt(), (tl.y + coords.size.height).toInt())
            }
            .pointerInput(col, emotionId) {
                detectTapGestures(
                    onPress = {
                        onPress(col, emotionId, cellBottom)
                        try { awaitRelease() } finally { onRelease() }
                    }
                )
            }
    )
}

// ─── Tooltip ─────────────────────────────────────────────────────────────────

@Composable
private fun TooltipCard(state: TooltipUiState.Visible) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = "${state.primaryLabel}  ·  ${state.colLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.emotions.forEach { emotion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = emotion.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IntensityDots(
                        intensity = emotion.maxIntensity,
                        color = Color(android.graphics.Color.parseColor(emotion.color))
                    )
                }
            }
        }
    }
}

@Composable
private fun IntensityDots(intensity: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (i < intensity) color else color.copy(alpha = 0.18f))
            )
        }
    }
}

// ─── Utilitaires ──────────────────────────────────────────────────────────────

// Retourne (ligne 1 : lettre/mois, ligne 2 : chiffre)
private fun buildXLabels(state: HeatmapUiState.Ready, col: Int): Pair<String, String> =
    when (state.period) {
        Period.WEEK -> {
            val d = state.startDate.plusDays(col.toLong())
            val abbr = arrayOf("L", "M", "M", "J", "V", "S", "D")
            Pair(abbr[d.dayOfWeek.value - 1], "${d.dayOfMonth}")
        }
        Period.MONTH -> {
            val d = state.startDate.plusDays(col.toLong())
            val abbr = arrayOf("L", "M", "M", "J", "V", "S", "D")
            val dayNum = if (col % 5 == 0) "${d.dayOfMonth}" else ""
            Pair(abbr[d.dayOfWeek.value - 1], dayNum)
        }
        Period.THREE_MONTHS -> {
            val weekStart = state.startDate.plusDays(col * 7L)
            val isNewMonth = col == 0 ||
                weekStart.monthValue != state.startDate.plusDays((col - 1) * 7L).monthValue
            val topLabel = if (isNewMonth) monthFr(weekStart.monthValue) else ""
            val bottomLabel = if (col % 2 == 0) "${weekStart.dayOfMonth}" else ""
            Pair(topLabel, bottomLabel)
        }
    }

private fun colorForIntensityFloat(hsl: Triple<Float, Float, Float>, intensity: Float): Color {
    val (h, s, l) = hsl
    val t = ((intensity - 1f) / 4f).coerceIn(0f, 1f)
    val sat = (15f + t * (s - 15f)) / 100f
    val lum = (78f - t * (78f - l)) / 100f
    return Color.hsl(h, sat.coerceIn(0f, 1f), lum.coerceIn(0f, 1f))
}

// ─── Statistiques ────────────────────────────────────────────────────────────

private val DOW_LABELS = arrayOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

@Composable
private fun GlobalStatsCard(stats: GlobalStats) {
    StatsCard("Entrées") {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            StatBigNumber(stats.totalEntries.toString(), "entrées")
            StatBigNumber("${stats.activeSinceDays}j", "d'activité")
        }
        if (stats.recordStreak > 1) {
            Text(
                "Record : ${stats.recordStreak} jours consécutifs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeriodStatsSectionTop(stats: PeriodStats) {
    if (stats.byType.isNotEmpty()) {
        StatsCard("Répartition") {
            TypeBreakdownBar(stats.byType)
            val avgStr = String.format(Locale.FRENCH, "%.1f", stats.avgPerWeek)
            val dowLabel = stats.topDayOfWeek?.let { DOW_LABELS[(it - 1).coerceIn(0, 6)] }
            Text(
                buildString {
                    append("Moy. $avgStr / sem")
                    if (dowLabel != null) append("  ·  $dowLabel le plus actif")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodStatsSectionBottom(stats: PeriodStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Qualité souvenir (rêves) ──────────────────────────────────────────
        if (stats.memoryQuality.isNotEmpty()) {
            StatsCard("Rêves") {
                MemoryQualityRow(stats.memoryQuality)
            }
        }

        // ── Top tags ──────────────────────────────────────────────────────────
        if (stats.topTags.isNotEmpty()) {
            StatsCard("Tags fréquents") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stats.topTags.forEach { TagChip(it) }
                }
            }
        }

        // ── Top parties ───────────────────────────────────────────────────────
        if (stats.topParts.isNotEmpty()) {
            StatsCard("Parties") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stats.topParts.forEach { PartChip(it) }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun StatBigNumber(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TypeBreakdownBar(byType: Map<EntryType, Int>) {
    val total = byType.values.sum().toFloat()
    if (total == 0f) return
    val order = listOf(EntryType.DREAM, EntryType.SESSION, EntryType.LIFE_EVENT)
    val extended = extendedColorScheme()
    val labels = mapOf(EntryType.DREAM to "Rêves", EntryType.SESSION to "Sessions", EntryType.LIFE_EVENT to "Événements")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            order.forEach { type ->
                val count = byType[type] ?: 0
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .weight(count / total)
                            .fillMaxHeight()
                            .background(extended.colorFamilyForType(type).color)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            order.forEach { type ->
                val count = byType[type] ?: 0
                if (count > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(extended.colorFamilyForType(type).color))
                        Text(
                            "${labels[type]} $count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryQualityRow(memQuality: List<MemoryQualityCount>) {
    val order = listOf("flou", "partiel", "clair")
    val labelMap = mapOf("flou" to "Flou", "partiel" to "Partiel", "clair" to "Clair")
    val countMap = memQuality.associate { it.quality to it.count }
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        order.forEach { q ->
            val count = countMap[q] ?: 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), style = MaterialTheme.typography.titleLarge)
                Text(
                    labelMap[q] ?: q,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TagChip(tag: TagCount) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#${tag.label}", style = MaterialTheme.typography.labelMedium)
        Text(
            tag.count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PartChip(part: PartCount) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(part.name, style = MaterialTheme.typography.labelMedium)
        Text(
            part.count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

