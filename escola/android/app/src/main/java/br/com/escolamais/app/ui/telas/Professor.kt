package br.com.escolamais.app.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.escolamais.app.data.AlunoChamada
import br.com.escolamais.app.data.ChamadaReq
import br.com.escolamais.app.data.EscolaApi
import br.com.escolamais.app.data.NovaTarefaReq
import br.com.escolamais.app.data.NovoAvisoReq
import br.com.escolamais.app.data.Presenca
import br.com.escolamais.app.data.mensagemDeErro
import br.com.escolamais.app.ui.componentes.CartaoItem
import br.com.escolamais.app.ui.componentes.Carregavel
import br.com.escolamais.app.ui.componentes.TelaBase
import br.com.escolamais.app.ui.componentes.dataBr
import br.com.escolamais.app.ui.componentes.dataExtenso
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ------------------------------------------------------------------ Chamada
@Composable
fun ChamadaTela(api: EscolaApi, disciplinaId: Int, onVoltar: () -> Unit) {
    var data by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    var salvando by remember { mutableStateOf(false) }
    // alunoId -> presente (alterações locais antes de salvar)
    val marcacoes = remember(data) { mutableStateMapOf<Int, Boolean>() }

    TelaBase("Chamada", onVoltar, snackbar) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { data = LocalDate.parse(data).minusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Dia anterior")
                }
                Text(data.dataExtenso().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { data = LocalDate.parse(data).plusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próximo dia")
                }
            }
            Carregavel(disciplinaId to data, { api.chamada(disciplinaId, data) }) { chamada, recarregar ->
                val presenteDe = { a: AlunoChamada -> marcacoes[a.alunoId] ?: a.presente ?: true }
                Column {
                    Text(
                        if (chamada.registrada) "Chamada já registrada — você pode corrigi-la." else "Chamada ainda não registrada.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(chamada.alunos, key = { it.alunoId }) { a ->
                            val presente = presenteDe(a)
                            CartaoItem {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(a.nome, fontWeight = FontWeight.SemiBold)
                                        Text(if (presente) "Presente" else "Falta",
                                            color = if (presente) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(checked = presente, onCheckedChange = { marcacoes[a.alunoId] = it })
                                }
                            }
                        }
                    }
                    val faltas = chamada.alunos.count { !presenteDe(it) }
                    Button(
                        enabled = !salvando && chamada.alunos.isNotEmpty(),
                        onClick = {
                            salvando = true
                            escopo.launch {
                                try {
                                    api.salvarChamada(disciplinaId, ChamadaReq(data, chamada.alunos.map { Presenca(it.alunoId, presenteDe(it)) }))
                                    snackbar.showSnackbar("Chamada salva")
                                    recarregar()
                                } catch (e: Exception) {
                                    snackbar.showSnackbar(mensagemDeErro(e))
                                } finally {
                                    salvando = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                    ) { Text("Salvar chamada ($faltas falta${if (faltas == 1) "" else "s"})") }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Nova tarefa
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaTarefaTela(api: EscolaApi, disciplinaId: Int, onVoltar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var entrega by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var escolherData by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    if (escolherData) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(entrega).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { escolherData = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { entrega = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    escolherData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolherData = false }) { Text("Cancelar") } },
        ) { DatePicker(estado) }
    }

    TelaBase("Nova tarefa", onVoltar, snackbar) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(descricao, { descricao = it }, label = { Text("Descrição") }, minLines = 4, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { escolherData = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Event, null); Text("  Entrega: ${entrega.dataBr()}")
            }
            Button(
                enabled = !salvando && titulo.isNotBlank(),
                onClick = {
                    salvando = true
                    escopo.launch {
                        try {
                            api.criarTarefa(disciplinaId, NovaTarefaReq(titulo.trim(), descricao.trim().ifBlank { null }, entrega))
                            onVoltar()
                        } catch (e: Exception) {
                            snackbar.showSnackbar(mensagemDeErro(e))
                            salvando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Publicar tarefa") }
        }
    }
}

// ------------------------------------------------------------------ Novo comunicado
@Composable
fun NovoAvisoTela(api: EscolaApi, onVoltar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var conteudo by rememberSaveable { mutableStateOf("") }
    var turmaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var salvando by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    TelaBase("Novo comunicado", onVoltar, snackbar) { padding ->
        Box(Modifier.padding(padding)) {
            Carregavel(Unit, { api.disciplinas().distinctBy { it.turmaId } }) { turmas, _ ->
                val selecionada = turmaId ?: turmas.firstOrNull()?.turmaId
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Turma", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScrollCompat()) {
                        turmas.forEach { t ->
                            FilterChip(selected = selecionada == t.turmaId, onClick = { turmaId = t.turmaId }, label = { Text(t.turmaNome) })
                        }
                    }
                    OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(conteudo, { conteudo = it }, label = { Text("Mensagem") }, minLines = 5, modifier = Modifier.fillMaxWidth())
                    Button(
                        enabled = !salvando && titulo.isNotBlank() && conteudo.isNotBlank() && selecionada != null,
                        onClick = {
                            salvando = true
                            escopo.launch {
                                try {
                                    api.criarAviso(NovoAvisoReq(titulo.trim(), conteudo.trim(), selecionada))
                                    onVoltar()
                                } catch (e: Exception) {
                                    snackbar.showSnackbar(mensagemDeErro(e))
                                    salvando = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("Publicar") }
                }
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier =
    this.then(Modifier.horizontalScroll(rememberScrollState()))
