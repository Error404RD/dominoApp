package com.error404.dominoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.error404.dominoapp.ui.DominoNavHost
import com.error404.dominoapp.ui.theme.DominoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DominoAppTheme {
                DominoNavHost()
            }
        }
    }
}
