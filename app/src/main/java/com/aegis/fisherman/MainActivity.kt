package com.aegis.fisherman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aegis.fisherman.navigation.AegisNavGraph
import com.aegis.fisherman.ui.theme.AegisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AegisTheme {
                AegisNavGraph()
            }
        }
    }
}
