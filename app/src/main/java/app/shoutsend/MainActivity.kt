package app.shoutsend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShoutSendApp
        val themeController = app.appContainer.themeController
        val devTools = app.appContainer.devTools

        setContent {
            val tokens by themeController.tokens.collectAsStateWithLifecycle()
            ChirpTheme(tokens = tokens) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CdScaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ShoutSend",
                                style = ChirpTheme.typography.headlineMedium,
                                color = ChirpTheme.colors.primary
                            )
                        }
                    }

                    // DevTools overlay (rendered in dev flavor, no-op in user flavor)
                    devTools.entryPoint?.invoke()
                }
            }
        }
    }
}
