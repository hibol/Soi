package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.hibol.miette.soi.ui.auth.launchBiometric

@Composable
fun LockScreen(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    var showRetry by remember { mutableStateOf(false) }

    fun triggerAuth() {
        showRetry = false
        launchBiometric(
            activity = context as FragmentActivity,
            onSuccess = currentOnAuthenticated,
            onError = { showRetry = true }
        )
    }

    LaunchedEffect(Unit) { triggerAuth() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Absorbe tous les événements pointer pour que le contenu derrière soit inactif.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                            .changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showRetry) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Soi",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = { triggerAuth() }) {
                    Text("Déverrouiller")
                }
            }
        }
    }
}
