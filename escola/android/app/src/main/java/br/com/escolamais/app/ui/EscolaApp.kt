package br.com.escolamais.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.escolamais.app.AppViewModel
import br.com.escolamais.app.EstadoApp
import br.com.escolamais.app.ui.telas.AgendaTela
import br.com.escolamais.app.ui.telas.AvisosTela
import br.com.escolamais.app.ui.telas.CardapioTela
import br.com.escolamais.app.ui.telas.ChamadaTela
import br.com.escolamais.app.ui.telas.FinanceiroTela
import br.com.escolamais.app.ui.telas.FrequenciaTela
import br.com.escolamais.app.ui.telas.HorarioTela
import br.com.escolamais.app.ui.telas.InicioTela
import br.com.escolamais.app.ui.telas.LoginTela
import br.com.escolamais.app.ui.telas.NotasTela
import br.com.escolamais.app.ui.telas.NovaTarefaTela
import br.com.escolamais.app.ui.telas.NovoAvisoTela
import br.com.escolamais.app.ui.telas.TarefasTela

object Rotas {
    const val INICIO = "inicio"
    const val AVISOS = "avisos"
    const val AGENDA = "agenda"
    const val HORARIO = "horario"
    const val NOTAS = "notas"
    const val FREQUENCIA = "frequencia"
    const val TAREFAS = "tarefas"
    const val CARDAPIO = "cardapio"
    const val FINANCEIRO = "financeiro"
    const val NOVO_AVISO = "novo_aviso"
    fun chamada(disciplinaId: Int) = "chamada/$disciplinaId"
    fun novaTarefa(disciplinaId: Int) = "nova_tarefa/$disciplinaId"
}

@Composable
fun EscolaApp(vm: AppViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoApp.Iniciando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is EstadoApp.Deslogado -> LoginTela(e, onEntrar = vm::login)
        is EstadoApp.Logado -> Navegacao(vm, e)
    }
}

@Composable
private fun Navegacao(vm: AppViewModel, estado: EstadoApp.Logado) {
    val nav = rememberNavController()
    val api = vm.api
    val professor = estado.me.usuario.ehProfessor
    val alunoId = estado.aluno?.id ?: -1
    val voltar: () -> Unit = { nav.popBackStack() }

    NavHost(nav, startDestination = Rotas.INICIO) {
        composable(Rotas.INICIO) {
            InicioTela(
                api = api,
                estado = estado,
                onAbrir = { nav.navigate(it) },
                onSelecionarAluno = vm::selecionarAluno,
                onSair = vm::sair,
            )
        }
        composable(Rotas.AVISOS) {
            AvisosTela(
                chave = alunoId,
                carregar = { if (professor) api.avisosProfessor() else api.avisos(alunoId) },
                podePublicar = professor,
                onNovo = { nav.navigate(Rotas.NOVO_AVISO) },
                onVoltar = voltar,
            )
        }
        composable(Rotas.AGENDA) {
            AgendaTela(alunoId, { if (professor) api.eventosProfessor() else api.eventos(alunoId) }, voltar)
        }
        composable(Rotas.HORARIO) { HorarioTela(alunoId, { api.horario(alunoId) }, voltar) }
        composable(Rotas.NOTAS) { NotasTela(alunoId, { api.notas(alunoId) }, voltar) }
        composable(Rotas.FREQUENCIA) { FrequenciaTela(alunoId, { api.frequencia(alunoId) }, voltar) }
        composable(Rotas.TAREFAS) { TarefasTela(alunoId, { api.tarefas(alunoId) }, voltar) }
        composable(Rotas.CARDAPIO) { CardapioTela({ api.cardapio() }, voltar) }
        composable(Rotas.FINANCEIRO) { FinanceiroTela(alunoId, { api.financeiro(alunoId) }, voltar) }
        composable(Rotas.NOVO_AVISO) { NovoAvisoTela(api, voltar) }
        composable("chamada/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
            ChamadaTela(api, it.arguments?.getInt("id") ?: 0, voltar)
        }
        composable("nova_tarefa/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
            NovaTarefaTela(api, it.arguments?.getInt("id") ?: 0, voltar)
        }
    }
}
