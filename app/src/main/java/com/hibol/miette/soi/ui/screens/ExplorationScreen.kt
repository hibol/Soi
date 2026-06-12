package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.ui.components.hexToHsl
import com.hibol.miette.soi.ui.navigation.MainBottomBar
import com.hibol.miette.soi.ui.viewmodel.ExplorationViewModel
import com.hibol.miette.soi.ui.viewmodel.HeatmapUiState
import com.hibol.miette.soi.ui.viewmodel.Period
import com.hibol.miette.soi.ui.viewmodel.SelectedCell
import com.hibol.miette.soi.ui.viewmodel.TooltipUiState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorationScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: ExplorationViewModel = viewModel(
        factory = ExplorationViewModel.Factory(
            app.container.profileRepository,
            app.container.entryRepository,
            app.container.emotionRepository
        )
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Exploration") }) },
        bottomBar = { MainBottomBar(currentRoute = currentRoute, navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            PeriodSelector(selected = selectedPeriod, onSelect = viewModel::selectPeriod)
            Spacer(modifier = Modifier.height(12.dp))
            ExplorationHeatmap(viewModel = viewModel)
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
                    selectedCell = selectedCell,
                    onCellPress = viewModel::pressCell,
                    onCellRelease = viewModel::releaseCell
                )
                val tooltip = tooltipState
                if (tooltip is TooltipUiState.Visible) {
                    TooltipCard(tooltip)
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
    selectedCell: SelectedCell?,
    onCellPress: (col: Int, emotionId: Long) -> Unit,
    onCellRelease: () -> Unit
) {
    val labelColWidth = 68.dp
    val cellHeight = 32.dp
    val headerHeight = 32.dp  // 2 lignes × 16dp

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
            }

            // Grille (scrollable pour 30j)
            val gridModifier = Modifier.weight(1f).let {
                if (state.period == Period.MONTH) it.horizontalScroll(rememberScrollState()) else it
            }

            Row(modifier = gridModifier) {
                for (col in 0 until state.columnCount) {
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
                                onPress = onCellPress,
                                onRelease = onCellRelease
                            )
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
    onPress: (col: Int, emotionId: Long) -> Unit,
    onRelease: () -> Unit
) {
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val computed = remember(emotionColor, intensity) {
        if (intensity != null) colorForIntensityFloat(hexToHsl(emotionColor), intensity) else null
    }
    val baseColor = computed ?: emptyColor
    val displayColor = if (isSelected) baseColor.copy(alpha = (baseColor.alpha * 0.55f).coerceAtLeast(0.12f))
                       else baseColor
    Box(
        modifier = Modifier
            .width(cellWidth)
            .height(cellHeight)
            .padding(1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(displayColor)
            .pointerInput(col, emotionId) {
                detectTapGestures(
                    onPress = {
                        onPress(col, emotionId)
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
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
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
            val d = LocalDate.ofEpochDay(state.startEpochDay + col)
            val abbr = arrayOf("L", "M", "M", "J", "V", "S", "D")
            Pair(abbr[d.dayOfWeek.value - 1], "${d.dayOfMonth}")
        }
        Period.MONTH -> {
            val d = LocalDate.ofEpochDay(state.startEpochDay + col)
            val abbr = arrayOf("L", "M", "M", "J", "V", "S", "D")
            // Numéro du mois visible tous les 5 jours pour éviter l'entassement
            val dayNum = if (col % 5 == 0) "${d.dayOfMonth}" else ""
            Pair(abbr[d.dayOfWeek.value - 1], dayNum)
        }
        Period.THREE_MONTHS -> {
            // Ligne 1 : abréviation du mois uniquement quand il change
            // Ligne 2 : numéro du jour de début de semaine, une semaine sur deux
            val weekStart = LocalDate.ofEpochDay(state.startEpochDay + col * 7L)
            val isNewMonth = col == 0 ||
                weekStart.monthValue != LocalDate.ofEpochDay(state.startEpochDay + (col - 1) * 7L).monthValue
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

private fun monthFr(month: Int): String =
    arrayOf("jan", "fév", "mars", "avr", "mai", "juin", "juil", "août", "sep", "oct", "nov", "déc")[month - 1]
