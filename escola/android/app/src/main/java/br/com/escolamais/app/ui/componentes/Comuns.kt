package br.com.escolamais.app.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.escolamais.app.data.mensagemDeErro
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PtBr: Locale = Locale.forLanguageTag("pt-BR")

/** "2026-10-09" (ou "2026-10-09 12:00:00") → "09/10/2026" */
fun String.dataBr(): String = runCatching {
    LocalDate.parse(take(10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(this)

/** "2026-10-09" → "sex., 09 de out." */
fun String.dataExtenso(): String = runCatching {
    LocalDate.parse(take(10)).format(DateTimeFormatter.ofPattern("EEE, dd 'de' MMM", PtBr))
}.getOrDefault(this)

fun nomeDiaSemana(dia: Int): String =
    java.time.DayOfWeek.of(dia).getDisplayName(TextStyle.FULL, PtBr).replaceFirstChar { it.uppercase() }

fun Double.reais(): String = NumberFormat.getCurrencyInstance(PtBr).format(this)

fun Double.umaCasa(): String = String.format(PtBr, "%.1f", this)

sealed interface Carga<out T> {
    data object Carregando : Carga<Nothing>
    data class Erro(val mensagem: String) : Carga<Nothing>
    data class Pronto<T>(val dados: T) : Carga<T>
}

/**
 * Carrega dados de forma assíncrona e mostra indicador de carregamento / erro com botão
 * "Tentar novamente". [conteudo] recebe os dados e uma função para recarregar.
 */
@Composable
fun <T> Carregavel(
    chave: Any?,
    carregar: suspend () -> T,
    conteudo: @Composable (dados: T, recarregar: () -> Unit) -> Unit,
) {
    var versao by remember { mutableIntStateOf(0) }
    var carga by remember(chave) { mutableStateOf<Carga<T>>(Carga.Carregando) }
    LaunchedEffect(chave, versao) {
        if (carga !is Carga.Pronto) carga = Carga.Carregando
        carga = try {
            Carga.Pronto(carregar())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Carga.Erro(mensagemDeErro(e))
        }
    }
    when (val c = carga) {
        Carga.Carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is Carga.Erro -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(c.mensagem, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { versao++ }) { Text("Tentar novamente") }
        }
        is Carga.Pronto -> conteudo(c.dados) { versao++ }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaBase(
    titulo: String,
    onVoltar: (() -> Unit)?,
    snackbar: SnackbarHostState? = null,
    acoes: @Composable () -> Unit = {},
    botaoFlutuante: @Composable () -> Unit = {},
    conteudo: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    if (onVoltar != null) IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = { acoes() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        floatingActionButton = botaoFlutuante,
        containerColor = MaterialTheme.colorScheme.background,
        content = conteudo,
    )
}

/** Tela padrão de lista: barra superior + carregamento + LazyColumn. */
@Composable
fun <T> TelaLista(
    titulo: String,
    onVoltar: () -> Unit,
    chave: Any?,
    carregar: suspend () -> List<T>,
    vazio: String,
    botaoFlutuante: @Composable () -> Unit = {},
    itens: LazyListScope.(List<T>) -> Unit,
) {
    var versao by remember { mutableIntStateOf(0) }
    TelaBase(
        titulo = titulo,
        onVoltar = onVoltar,
        acoes = {
            IconButton(onClick = { versao++ }) { Icon(Icons.Filled.Refresh, contentDescription = "Atualizar") }
        },
        botaoFlutuante = botaoFlutuante,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            Carregavel(chave to versao, carregar) { lista, _ ->
                if (lista.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(vazio, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { itens(lista) }
                }
            }
        }
    }
}

@Composable
fun CartaoItem(modifier: Modifier = Modifier, conteudo: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { conteudo() }
    }
}

@Composable
fun Etiqueta(texto: String, cor: Color) {
    Surface(color = cor.copy(alpha = 0.14f), shape = MaterialTheme.shapes.small) {
        Text(
            texto,
            color = cor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun LinhaTitulo(titulo: String, direita: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        direita()
    }
}

@Composable
fun rememberSalvo(inicial: String) = rememberSaveable { mutableStateOf(inicial) }
