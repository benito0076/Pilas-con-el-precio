package co.pilas.precio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import co.pilas.precio.ui.PilasApp
import co.pilas.precio.ui.PilasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PilasTheme {
                PilasApp()
            }
        }
    }
}
