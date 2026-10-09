package br.com.escolamais.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import br.com.escolamais.app.data.Me
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
import br.com.escolamais.app.ui.theme.EscolaTema
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Gera as capturas de tela do app com dados de demonstração.
 * ./gradlew recordRoborazziDebug  → imagens em app/build/capturas/
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class CapturasDeTela {
    @get:Rule val regra = createComposeRule()
    private val api = ApiDemo()
    private val nada: () -> Unit = {}

    private fun capturar(nome: String, tela: @Composable () -> Unit) {
        regra.setContent { EscolaTema(escuro = false) { tela() } }
        regra.waitForIdle()
        regra.onRoot().captureRoboImage("build/capturas/$nome.png")
    }

    @Test fun login() = capturar("01_login") { LoginTela(EstadoApp.Deslogado("https://escola.exemplo.com.br/"), onEntrar = { _, _, _ -> }) }

    @Test fun inicioResponsavel() = capturar("02_inicio_responsavel") {
        InicioTela(api, EstadoApp.Logado(Me(api.responsavel, api.alunos), api.alunos[0]), {}, {}, nada)
    }

    @Test fun avisos() = capturar("03_comunicados") { AvisosTela(1, { api.avisos(4) }, false, nada, nada) }
    @Test fun agenda() = capturar("04_agenda") { AgendaTela(1, { api.eventos(4) }, nada) }
    @Test fun horario() = capturar("05_horario") { HorarioTela(1, { api.horario(4) }, nada) }
    @Test fun notas() = capturar("06_boletim") { NotasTela(1, { api.notas(4) }, nada) }
    @Test fun frequencia() = capturar("07_frequencia") { FrequenciaTela(1, { api.frequencia(4) }, nada) }
    @Test fun tarefas() = capturar("08_tarefas") { TarefasTela(1, { api.tarefas(4) }, nada) }
    @Test fun cardapio() = capturar("09_cardapio") { CardapioTela({ api.cardapio() }, nada) }
    @Test fun financeiro() = capturar("10_financeiro") { FinanceiroTela(1, { api.financeiro(4) }, nada) }

    @Test fun inicioProfessor() = capturar("11_inicio_professor") {
        InicioTela(api, EstadoApp.Logado(Me(api.professora), null), {}, {}, nada)
    }
    @Test fun chamada() = capturar("12_chamada") { ChamadaTela(api, 1, nada) }
    @Test fun novaTarefa() = capturar("13_nova_tarefa") { NovaTarefaTela(api, 1, nada) }
    @Test fun novoAviso() = capturar("14_novo_comunicado") { NovoAvisoTela(api, nada) }
}
