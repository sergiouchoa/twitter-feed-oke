package br.com.escolamais.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.escolamais.app.data.Aluno
import br.com.escolamais.app.data.EscolaApi
import br.com.escolamais.app.data.LoginReq
import br.com.escolamais.app.data.Me
import br.com.escolamais.app.data.SessaoStore
import br.com.escolamais.app.data.mensagemDeErro
import br.com.escolamais.app.data.naoAutorizado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoApp {
    data object Iniciando : EstadoApp
    data class Deslogado(val url: String, val erro: String? = null, val carregando: Boolean = false) : EstadoApp
    data class Logado(val me: Me, val aluno: Aluno?) : EstadoApp
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessaoStore(app)
    private var token: String? = null
    private var url: String = BuildConfig.API_URL

    var api: EscolaApi = EscolaApi.criar(url) { token }
        private set

    private val _estado = MutableStateFlow<EstadoApp>(EstadoApp.Iniciando)
    val estado: StateFlow<EstadoApp> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val salva = store.ler()
            salva.url?.let { trocarUrl(it) }
            token = salva.token
            if (token == null) {
                _estado.value = EstadoApp.Deslogado(url)
                return@launch
            }
            try {
                entrar(api.me(), salva.alunoId)
            } catch (e: Exception) {
                if (e.naoAutorizado()) store.sair()
                _estado.value = EstadoApp.Deslogado(url, if (e.naoAutorizado()) null else mensagemDeErro(e))
                if (e.naoAutorizado()) token = null
            }
        }
    }

    private fun trocarUrl(nova: String) {
        val normalizada = EscolaApi.normalizarUrl(nova)
        if (normalizada != url || _estado.value == EstadoApp.Iniciando) {
            url = normalizada
            api = EscolaApi.criar(url) { token }
        }
    }

    private fun entrar(me: Me, alunoPreferido: Int?) {
        val aluno = me.alunos.firstOrNull { it.id == alunoPreferido } ?: me.alunos.firstOrNull()
        _estado.value = EstadoApp.Logado(me, aluno)
    }

    fun login(urlServidor: String, email: String, senha: String) {
        if (email.isBlank() || senha.isBlank()) {
            _estado.value = EstadoApp.Deslogado(urlServidor, "Informe e-mail e senha")
            return
        }
        _estado.value = EstadoApp.Deslogado(urlServidor, carregando = true)
        viewModelScope.launch {
            try {
                trocarUrl(urlServidor)
                val resp = api.login(LoginReq(email.trim(), senha))
                token = resp.token
                store.salvarLogin(url, resp.token)
                entrar(api.me(), null)
            } catch (e: Exception) {
                token = null
                _estado.value = EstadoApp.Deslogado(urlServidor, mensagemDeErro(e))
            }
        }
    }

    fun selecionarAluno(aluno: Aluno) {
        val atual = _estado.value as? EstadoApp.Logado ?: return
        _estado.value = atual.copy(aluno = aluno)
        viewModelScope.launch { store.salvarAluno(aluno.id) }
    }

    fun sair() {
        token = null
        viewModelScope.launch { store.sair() }
        _estado.value = EstadoApp.Deslogado(url)
    }
}
