package br.com.escolamais.app.data

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

interface EscolaApi {
    @POST("api/auth/login") suspend fun login(@Body req: LoginReq): LoginResp
    @GET("api/me") suspend fun me(): Me

    // Aluno / responsável
    @GET("api/alunos/{id}/resumo") suspend fun resumo(@Path("id") alunoId: Int): Resumo
    @GET("api/alunos/{id}/avisos") suspend fun avisos(@Path("id") alunoId: Int): List<Aviso>
    @GET("api/alunos/{id}/eventos") suspend fun eventos(@Path("id") alunoId: Int): List<Evento>
    @GET("api/alunos/{id}/horario") suspend fun horario(@Path("id") alunoId: Int): List<Aula>
    @GET("api/alunos/{id}/notas") suspend fun notas(@Path("id") alunoId: Int): List<Boletim>
    @GET("api/alunos/{id}/frequencia") suspend fun frequencia(@Path("id") alunoId: Int): List<Frequencia>
    @GET("api/alunos/{id}/tarefas") suspend fun tarefas(@Path("id") alunoId: Int): List<Tarefa>
    @GET("api/alunos/{id}/financeiro") suspend fun financeiro(@Path("id") alunoId: Int): List<Mensalidade>
    @GET("api/cardapio") suspend fun cardapio(): List<ItemCardapio>

    // Professor
    @GET("api/avisos") suspend fun avisosProfessor(): List<Aviso>
    @GET("api/eventos") suspend fun eventosProfessor(): List<Evento>
    @GET("api/professor/disciplinas") suspend fun disciplinas(): List<DisciplinaProfessor>
    @GET("api/professor/disciplinas/{id}/chamada")
    suspend fun chamada(@Path("id") disciplinaId: Int, @Query("data") data: String): Chamada
    @POST("api/professor/disciplinas/{id}/chamada")
    suspend fun salvarChamada(@Path("id") disciplinaId: Int, @Body req: ChamadaReq): OkResp
    @POST("api/professor/disciplinas/{id}/tarefas")
    suspend fun criarTarefa(@Path("id") disciplinaId: Int, @Body req: NovaTarefaReq): IdResp
    @POST("api/professor/avisos") suspend fun criarAviso(@Body req: NovoAvisoReq): IdResp

    companion object {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        fun criar(urlBase: String, token: () -> String?): EscolaApi {
            val autenticacao = Interceptor { chain ->
                val req = chain.request().newBuilder()
                token()?.let { req.header("Authorization", "Bearer $it") }
                chain.proceed(req.build())
            }
            val cliente = OkHttpClient.Builder()
                .addInterceptor(autenticacao)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(normalizarUrl(urlBase))
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(EscolaApi::class.java)
        }

        fun normalizarUrl(url: String): String {
            var u = url.trim()
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
            return if (u.endsWith("/")) u else "$u/"
        }
    }
}

/** Converte exceções de rede/HTTP em mensagens amigáveis em português. */
fun mensagemDeErro(e: Throwable): String = when (e) {
    is HttpException -> e.response()?.errorBody()?.string()
        ?.let { runCatching { EscolaApi.json.decodeFromString<ErroResp>(it).erro }.getOrNull() }
        ?: "Erro do servidor (${e.code()})"
    is IOException -> "Sem conexão com o servidor. Verifique sua internet."
    else -> e.message ?: "Erro inesperado"
}

fun Throwable.naoAutorizado() = this is HttpException && code() == 401
