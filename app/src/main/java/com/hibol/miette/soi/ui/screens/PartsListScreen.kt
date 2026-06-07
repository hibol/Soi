package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.ui.navigation.MainBottomBar
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.viewmodel.PartsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsListScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: PartsViewModel = viewModel(
        factory = PartsViewModel.Factory(
            app.container.partRepository,
            app.container.profileRepository
        )
    )

    val parts by viewModel.parts.collectAsState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var showRolePicker by remember { mutableStateOf(false) }

    if (showRolePicker) {
        PartRolePicker(
            onRoleSelected = { role ->
                showRolePicker = false
                navController.navigate(Routes.partDetail(role = role))
            },
            onDismiss = { showRolePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Parties") })
        },
        bottomBar = {
            MainBottomBar(currentRoute = currentRoute, navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRolePicker = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle Partie")
            }
        }
    ) { innerPadding ->
        if (parts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Aucune partie pour l'instant",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Appuyer sur + pour créer une première partie IFS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(parts, key = { it.id }) { part ->
                    PartListItem(

                        part = part,
                        onClick = { navController.navigate(Routes.partRead(part.id)) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PartListItem(part: Part, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(part.name) },
        supportingContent = {
            val roleText = part.role?.let { roleLabel(it) }
            val ageText = part.age
            val text = listOfNotNull(roleText, ageText).joinToString(" · ")
            if (text.isNotEmpty()) Text(text)
        },
        leadingContent = {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

internal fun roleLabel(role: String): String = when (role) {
    "exile" -> "Exilé"
    "manager" -> "Manager"
    "firefighter" -> "Pompier"
    "other" -> "Autre"
    else -> role
}
