package com.painterai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.painterai.app.ui.navigation.PainterAINavGraph
import com.painterai.app.ui.theme.PainterAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PainterAITheme {
                val navController = rememberNavController()
                PainterAINavGraph(navController = navController)
            }
        }
    }
}
