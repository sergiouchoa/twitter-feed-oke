package br.com.escolamais.app.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.escolamais.app.EstadoApp
import br.com.escolamais.app.data.Aluno
import br.com.escolamais.app.data.EscolaApi
import br.com.escolamais.app.ui.Rotas
import br.com.escolamais.app.ui.componentes.Carregavel
import br.com.escolamais.app.ui.componentes.TelaBase
import br.com.escolamais.app.ui.componentes.dataExtenso
import br.com.escolamais.app.ui.componentes.umaCasa
import br.com.escolamais.app.ui.theme.Verde
import br.com.escolamais.app.ui.theme.Vermelho

private data class Modulo(val titulo: String, val icone: ImageVector, val rota: String)

private val modulosAluno = listOf(
    Modulo("Comunicados", Icons.Filled.Campaign, Rotas.AVISOS),
    Modulo("Agenda", Icons.Filled.Event, Rotas.AGENDA),
    Modulo("Horário", Icons.Filled.Schedule, Rotas.HORARIO),
    Modulo("Notas", Icons.Filled.Grade, Rotas.NOTAS),
    Modulo("Frequência", Icons.AutoMirrored.Filled.FactCheck, Rotas.FREQUENCIA),
    Modulo("Tarefas", Icons.AutoMirrored.Filled.Assignment, Rotas.TAREFAS),
    Modulo("Cardápio", Icons.Filled.Restaurant, Rotas.CARDAPIO),
    Modulo("Financeiro", Icons.Filled.Payments, Rotas.FINANCEIRO),
)

private val modulosProfessor = listOf(
    Modulo("Comunicados", Icons.Filled.Campaign, Rotas.AVISOS),
    Modulo("Agenda", Icons.Filled.Event, Rotas.AGENDA),
    Modulo("Cardápio", Icons.Filled.Restaurant, Rotas.CARDAPIO),
)

@Composable
fun InicioTela(
    api: EscolaApi,
    estado: EstadoApp.Logado,
    onAbrir: (String) -> Unit,
    onSelecionarAluno: (Aluno) -> Unit,
    onSair: () -> Unit,
) {
    val usuario = estado.me.usuario
    TelaBase(
        titulo = "Olá, ${usuario.nome.substringBefore(' ')}",
        onVoltar = null,
        acoes = { IconButton(onClick = onSair) { Icon(Icons.AutoMirrored.Filled.Logout, "Sair") } },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (usuario.ehProfessor) {
                InicioProfessor(api, onAbrir)
            } else {
                InicioAluno(api, estado, onAbrir, onSelecionarAluno)
            }
        }
    }
}

@Composable
private fun InicioAluno(api: EscolaApi, estado: EstadoApp.Logado, onAbrir: (String) -> Unit, onSelecionarAluno: (Aluno) -> Unit) {
    val aluno = estado.aluno
    if (aluno == null) {
        Text(
            "Nenhum aluno vinculado à sua conta. Procure a secretaria da escola.",
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        )
        return
    }
    if (estado.me.alunos.size > 1) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.me.alunos.forEach { a ->
                FilterChip(selected = a.id == aluno.id, onClick = { onSelecionarAluno(a) }, label = { Text(a.nome) })
            }
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(aluno.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(listOfNotNull(aluno.turmaNome, aluno.matricula?.let { "Matrícula $it" }).joinToString(" • "))
        }
    }
    Carregavel(aluno.id, { api.resumo(aluno.id) }) { r, _ ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Média geral", r.mediaGeral?.umaCasa() ?: "—",
                    if ((r.mediaGeral ?: 10.0) >= 6) Verde else Vermelho, Modifier.weight(1f)) { onAbrir(Rotas.NOTAS) }
                Indicador("Frequência", "${r.frequencia.umaCasa()}%",
                    if (r.frequencia >= 75) Verde else Vermelho, Modifier.weight(1f)) { onAbrir(Rotas.FREQUENCIA) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Tarefas pendentes", r.tarefasPendentes.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { onAbrir(Rotas.TAREFAS) }
                Indicador("Mensalidades em aberto", r.mensalidadesEmAberto.toString(),
                    if (r.mensalidadesEmAberto == 0) Verde else MaterialTheme.colorScheme.secondary, Modifier.weight(1f)) { onAbrir(Rotas.FINANCEIRO) }
            }
            r.proximoEvento?.let { e ->
                Destaque(Icons.Filled.Event, "Próximo evento", "${e.titulo} — ${e.data.dataExtenso()}${e.hora?.let { " às $it" } ?: ""}") { onAbrir(Rotas.AGENDA) }
            }
            r.ultimoAviso?.let { a -> Destaque(Icons.Filled.Campaign, "Último comunicado", a.titulo) { onAbrir(Rotas.AVISOS) } }
        }
    }
    GradeModulos(modulosAluno, onAbrir)
}

@Composable
private fun InicioProfessor(api: EscolaApi, onAbrir: (String) -> Unit) {
    GradeModulos(modulosProfessor, onAbrir)
    Text("Minhas turmas", style = MaterialTheme.typography.titleLarge)
    Carregavel(Unit, { api.disciplinas() }) { disciplinas, _ ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (disciplinas.isEmpty()) Text("Nenhuma disciplina atribuída a você.")
            disciplinas.forEach { d ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(d.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${d.turmaNome} • ${d.totalAlunos} alunos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { onAbrir(Rotas.chamada(d.id)) }) {
                                Icon(Icons.AutoMirrored.Filled.FactCheck, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Chamada")
                            }
                            OutlinedButton(onClick = { onAbrir(Rotas.novaTarefa(d.id)) }) {
                                Icon(Icons.AutoMirrored.Filled.Assignment, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nova tarefa")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, cor: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = cor, fontWeight = FontWeight.Bold)
            Text(rotulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Destaque(icone: ImageVector, titulo: String, texto: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(texto, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GradeModulos(modulos: List<Modulo>, onAbrir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        modulos.chunked(4).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                linha.forEach { m ->
                    Card(Modifier.weight(1f).aspectRatio(0.95f).clickable { onAbrir(m.rota) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(6.dp).weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(m.icone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(m.titulo, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                repeat(4 - linha.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}
