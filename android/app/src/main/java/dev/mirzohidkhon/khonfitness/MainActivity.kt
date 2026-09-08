package dev.mirzohidkhon.khonfitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.mirzohidkhon.khonfitness.ui.KhonNav
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { KhonTheme { KhonNav() } }
    }
}
