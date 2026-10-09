package br.com.escolamais.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.escolamais.app.ui.EscolaApp
import br.com.escolamais.app.ui.theme.EscolaTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EscolaTema {
                EscolaApp()
            }
        }
    }
}
