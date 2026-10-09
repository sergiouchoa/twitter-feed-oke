package br.com.escolamais.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Azul = Color(0xFF1E5BD8)
val Amarelo = Color(0xFFFFB020)
val Verde = Color(0xFF2E7D32)
val Vermelho = Color(0xFFC62828)

private val Claro = lightColorScheme(
    primary = Azul,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FB),
    onPrimaryContainer = Color(0xFF0B2A6B),
    secondary = Amarelo,
    onSecondary = Color(0xFF1D2433),
    background = Color(0xFFF4F6FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF2FB),
    error = Vermelho,
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF9DB9FF),
    onPrimary = Color(0xFF002C71),
    primaryContainer = Color(0xFF15418F),
    onPrimaryContainer = Color(0xFFDCE6FB),
    secondary = Amarelo,
    background = Color(0xFF10131A),
    surface = Color(0xFF181C24),
    surfaceVariant = Color(0xFF232836),
)

@Composable
fun EscolaTema(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (escuro) Escuro else Claro, content = conteudo)
}
