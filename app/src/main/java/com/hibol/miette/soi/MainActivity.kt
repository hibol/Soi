package com.hibol.miette.soi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.hibol.miette.soi.ui.navigation.NavGraph
import com.hibol.miette.soi.ui.theme.SoiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoiTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}