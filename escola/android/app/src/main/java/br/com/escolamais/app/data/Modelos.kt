package br.com.escolamais.app.data

import kotlinx.serialization.Serializable

@Serializable data class LoginReq(val email: String, val senha: String)
@Serializable data class LoginResp(val token: String, val usuario: Usuario)

@Serializable
data class Usuario(val id: Int, val nome: String, val email: String, val papel: String) {
    val ehProfessor get() = papel == "professor" || papel == "admin"
}

@Serializable
data class Aluno(
    val id: Int,
    val nome: String,
    val matricula: String? = null,
    val turmaId: Int? = null,
    val turmaNome: String? = null,
)

@Serializable data class Me(val usuario: Usuario, val alunos: List<Aluno> = emptyList())

@Serializable
data class Aviso(
    val id: Int,
    val titulo: String,
    val conteudo: String,
    val importante: Boolean = false,
    val criadoEm: String,
    val turmaNome: String? = null,
    val autor: String? = null,
)

@Serializable
data class Evento(
    val id: Int,
    val titulo: String,
    val descricao: String? = null,
    val data: String,
    val hora: String? = null,
    val local: String? = null,
    val turmaNome: String? = null,
)

@Serializable
data class Resumo(
    val mediaGeral: Double? = null,
    val frequencia: Double = 100.0,
    val tarefasPendentes: Int = 0,
    val mensalidadesEmAberto: Int = 0,
    val proximoEvento: Evento? = null,
    val ultimoAviso: Aviso? = null,
)

@Serializable
data class Aula(
    val diaSemana: Int,
    val inicio: String,
    val fim: String,
    val sala: String? = null,
    val disciplina: String,
    val professor: String? = null,
)

@Serializable
data class Boletim(
    val disciplinaId: Int,
    val disciplina: String,
    val b1: Double? = null,
    val b2: Double? = null,
    val b3: Double? = null,
    val b4: Double? = null,
    val media: Double? = null,
)

@Serializable
data class Frequencia(
    val disciplinaId: Int,
    val disciplina: String,
    val aulas: Int,
    val faltas: Int,
    val percentual: Double,
)

@Serializable
data class Tarefa(
    val id: Int,
    val titulo: String,
    val descricao: String? = null,
    val entrega: String,
    val disciplina: String,
)

@Serializable data class ItemCardapio(val id: Int, val data: String, val refeicao: String, val descricao: String)

@Serializable
data class Mensalidade(
    val id: Int,
    val referencia: String,
    val valor: Double,
    val vencimento: String,
    val pagoEm: String? = null,
    val linhaDigitavel: String? = null,
    val situacao: String,
)

@Serializable
data class DisciplinaProfessor(
    val id: Int,
    val nome: String,
    val turmaId: Int,
    val turmaNome: String,
    val totalAlunos: Int = 0,
)

@Serializable data class AlunoChamada(val alunoId: Int, val nome: String, val matricula: String? = null, val presente: Boolean? = null)
@Serializable data class Chamada(val data: String, val registrada: Boolean, val alunos: List<AlunoChamada>)
@Serializable data class Presenca(val alunoId: Int, val presente: Boolean)
@Serializable data class ChamadaReq(val data: String, val presencas: List<Presenca>)
@Serializable data class NovaTarefaReq(val titulo: String, val descricao: String?, val entrega: String)
@Serializable data class NovoAvisoReq(val titulo: String, val conteudo: String, val turmaId: Int?)
@Serializable data class OkResp(val ok: Boolean = true)
@Serializable data class IdResp(val id: Int)
@Serializable data class ErroResp(val erro: String)
