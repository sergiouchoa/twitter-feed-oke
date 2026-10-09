package br.com.escolamais.app.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.escolamais.app.data.Aula
import br.com.escolamais.app.data.Aviso
import br.com.escolamais.app.data.Boletim
import br.com.escolamais.app.data.Evento
import br.com.escolamais.app.data.Frequencia
import br.com.escolamais.app.data.ItemCardapio
import br.com.escolamais.app.data.Mensalidade
import br.com.escolamais.app.data.Tarefa
import br.com.escolamais.app.ui.componentes.CartaoItem
import br.com.escolamais.app.ui.componentes.Etiqueta
import br.com.escolamais.app.ui.componentes.LinhaTitulo
import br.com.escolamais.app.ui.componentes.TelaLista
import br.com.escolamais.app.ui.componentes.dataBr
import br.com.escolamais.app.ui.componentes.dataExtenso
import br.com.escolamais.app.ui.componentes.nomeDiaSemana
import br.com.escolamais.app.ui.componentes.reais
import br.com.escolamais.app.ui.componentes.umaCasa
import br.com.escolamais.app.ui.theme.Verde
import br.com.escolamais.app.ui.theme.Vermelho
import java.time.LocalDate

@Composable
private fun Suave(texto: String) =
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

// ------------------------------------------------------------------ Comunicados
@Composable
fun AvisosTela(chave: Any?, carregar: suspend () -> List<Aviso>, podePublicar: Boolean, onNovo: () -> Unit, onVoltar: () -> Unit) {
    TelaLista(
        titulo = "Comunicados", onVoltar = onVoltar, chave = chave, carregar = carregar,
        vazio = "Nenhum comunicado por enquanto.",
        botaoFlutuante = { if (podePublicar) FloatingActionButton(onClick = onNovo) { Icon(Icons.Filled.Add, "Novo comunicado") } },
    ) { lista ->
        items(lista, key = { it.id }) { a ->
            CartaoItem {
                LinhaTitulo(a.titulo) { if (a.importante) Etiqueta("Importante", Vermelho) }
                Suave("${a.criadoEm.dataBr()} • ${a.turmaNome ?: "Toda a escola"}${a.autor?.let { " • $it" } ?: ""}")
                Spacer(Modifier.height(8.dp))
                Text(a.conteudo)
            }
        }
    }
}

// ------------------------------------------------------------------ Agenda
@Composable
fun AgendaTela(chave: Any?, carregar: suspend () -> List<Evento>, onVoltar: () -> Unit) {
    TelaLista("Agenda", onVoltar, chave, carregar, "Nenhum evento agendado.") { lista ->
        items(lista, key = { it.id }) { e ->
            CartaoItem {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                        Text(e.data.takeLast(2), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(e.data.dataExtenso().substringAfter("de ").trimEnd('.'), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.titulo, style = MaterialTheme.typography.titleMedium)
                        Suave(listOfNotNull(e.data.dataExtenso(), e.hora, e.local, e.turmaNome).joinToString(" • "))
                        e.descricao?.let { Spacer(Modifier.height(4.dp)); Text(it) }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Horário
@Composable
fun HorarioTela(chave: Any?, carregar: suspend () -> List<Aula>, onVoltar: () -> Unit) {
    val hoje = LocalDate.now().dayOfWeek.value
    TelaLista("Horário de aulas", onVoltar, chave, carregar, "Horário ainda não cadastrado.") { lista ->
        lista.groupBy { it.diaSemana }.toSortedMap().forEach { (dia, aulas) ->
            item(key = "dia$dia") {
                CartaoItem {
                    LinhaTitulo(nomeDiaSemana(dia)) { if (dia == hoje) Etiqueta("Hoje", MaterialTheme.colorScheme.primary) }
                    aulas.forEachIndexed { i, a ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp)) else Spacer(Modifier.height(8.dp))
                        Row {
                            Text("${a.inicio}–${a.fim}", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(110.dp))
                            Column {
                                Text(a.disciplina)
                                Suave(listOfNotNull(a.professor, a.sala).joinToString(" • "))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Notas
@Composable
fun NotasTela(chave: Any?, carregar: suspend () -> List<Boletim>, onVoltar: () -> Unit) {
    TelaLista("Boletim", onVoltar, chave, carregar, "Nenhuma disciplina encontrada.") { lista ->
        items(lista, key = { it.disciplinaId }) { b ->
            CartaoItem {
                LinhaTitulo(b.disciplina) {
                    b.media?.let { Etiqueta("Média ${it.umaCasa()}", if (it >= 6) Verde else Vermelho) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(b.b1, b.b2, b.b3, b.b4).forEachIndexed { i, n ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Suave("${i + 1}º bim.")
                            Text(n?.umaCasa() ?: "—", style = MaterialTheme.typography.titleMedium,
                                color = when { n == null -> MaterialTheme.colorScheme.onSurfaceVariant; n >= 6 -> Verde; else -> Vermelho })
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Frequência
@Composable
fun FrequenciaTela(chave: Any?, carregar: suspend () -> List<Frequencia>, onVoltar: () -> Unit) {
    TelaLista("Frequência", onVoltar, chave, carregar, "Sem registros de frequência.") { lista ->
        item {
            Text("A frequência mínima exigida é de 75% por disciplina.", style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        items(lista, key = { it.disciplinaId }) { f ->
            val cor = if (f.percentual >= 75) Verde else Vermelho
            CartaoItem {
                LinhaTitulo(f.disciplina) { Text("${f.percentual.umaCasa()}%", color = cor, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { (f.percentual / 100).toFloat() }, color = cor, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Suave("${f.aulas} aulas • ${f.faltas} falta(s)")
            }
        }
    }
}

// ------------------------------------------------------------------ Tarefas
@Composable
fun TarefasTela(chave: Any?, carregar: suspend () -> List<Tarefa>, onVoltar: () -> Unit) {
    val hoje = LocalDate.now().toString()
    TelaLista("Tarefas", onVoltar, chave, carregar, "Nenhuma tarefa cadastrada.") { lista ->
        val (pendentes, passadas) = lista.partition { it.entrega >= hoje }
        if (pendentes.isNotEmpty()) item { Text("A entregar", style = MaterialTheme.typography.titleMedium) }
        items(pendentes, key = { it.id }) { CartaoTarefa(it, false) }
        if (passadas.isNotEmpty()) item { Text("Anteriores", style = MaterialTheme.typography.titleMedium) }
        items(passadas.reversed(), key = { it.id }) { CartaoTarefa(it, true) }
    }
}

@Composable
private fun CartaoTarefa(t: Tarefa, passada: Boolean) {
    CartaoItem {
        LinhaTitulo(t.titulo) {
            Etiqueta(t.entrega.dataBr(), if (passada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        }
        Suave(t.disciplina)
        t.descricao?.let { Spacer(Modifier.height(6.dp)); Text(it) }
    }
}

// ------------------------------------------------------------------ Cardápio
@Composable
fun CardapioTela(carregar: suspend () -> List<ItemCardapio>, onVoltar: () -> Unit) {
    TelaLista("Cardápio da semana", onVoltar, Unit, carregar, "Cardápio ainda não publicado.") { lista ->
        lista.groupBy { it.data }.forEach { (data, refeicoes) ->
            item(key = data) {
                CartaoItem {
                    LinhaTitulo(data.dataExtenso().replaceFirstChar { it.uppercase() })
                    refeicoes.forEach { r ->
                        Spacer(Modifier.height(8.dp))
                        Text(r.refeicao, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(r.descricao)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Financeiro
@Composable
fun FinanceiroTela(chave: Any?, carregar: suspend () -> List<Mensalidade>, onVoltar: () -> Unit) {
    val area = LocalClipboardManager.current
    TelaLista("Financeiro", onVoltar, chave, carregar, "Nenhuma cobrança encontrada.") { lista ->
        items(lista, key = { it.id }) { m ->
            CartaoItem {
                LinhaTitulo("Mensalidade ${m.referencia.split("-").reversed().joinToString("/")}") {
                    when (m.situacao) {
                        "pago" -> Etiqueta("Pago", Verde)
                        "atrasado" -> Etiqueta("Atrasado", Vermelho)
                        else -> Etiqueta("Em aberto", MaterialTheme.colorScheme.primary)
                    }
                }
                Text(m.valor.reais(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Suave(m.pagoEm?.let { "Pago em ${it.dataBr()}" } ?: "Vencimento ${m.vencimento.dataBr()}")
                if (m.pagoEm == null && !m.linhaDigitavel.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(m.linhaDigitavel, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { area.setText(AnnotatedString(m.linhaDigitavel)) }) {
                        Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copiar código de pagamento")
                    }
                }
            }
        }
    }
}
