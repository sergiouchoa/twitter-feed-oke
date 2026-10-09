package br.com.escolamais.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("sessao")

data class SessaoSalva(val url: String?, val token: String?, val alunoId: Int?)

class SessaoStore(private val context: Context) {
    private val chaveUrl = stringPreferencesKey("url")
    private val chaveToken = stringPreferencesKey("token")
    private val chaveAluno = intPreferencesKey("aluno")

    suspend fun ler(): SessaoSalva = context.dataStore.data.first().let {
        SessaoSalva(it[chaveUrl], it[chaveToken], it[chaveAluno])
    }

    suspend fun salvarLogin(url: String, token: String) = context.dataStore.edit {
        it[chaveUrl] = url
        it[chaveToken] = token
    }

    suspend fun salvarAluno(id: Int) = context.dataStore.edit { it[chaveAluno] = id }

    suspend fun sair() = context.dataStore.edit {
        it.remove(chaveToken)
        it.remove(chaveAluno)
    }
}
