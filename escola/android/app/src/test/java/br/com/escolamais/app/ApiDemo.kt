package br.com.escolamais.app

import br.com.escolamais.app.data.*
import java.time.LocalDate

/** Implementação de [EscolaApi] com dados fixos, usada para gerar as capturas de tela. */
class ApiDemo : EscolaApi {
    private fun dia(n: Long) = LocalDate.now().plusDays(n).toString()
    private val hoje = LocalDate.now()

    val responsavel = Usuario(10, "Paula Pereira", "paula@familia.local", "responsavel")
    val professora = Usuario(2, "Ana Souza", "ana.prof@escola.local", "professor")
    val alunos = listOf(
        Aluno(4, "João Pereira", "2025001", 1, "6º Ano A"),
        Aluno(8, "Lucas Pereira", "2024011", 2, "7º Ano B"),
    )

    private val avisos = listOf(
        Aviso(1, "Reunião de pais", "Reunião de pais e mestres no sábado às 9h no auditório. Contamos com a presença de todos!", true, "${dia(-1)} 10:00:00", null, "Secretaria"),
        Aviso(2, "Passeio ao museu", "Tragam a autorização assinada até sexta-feira. Saída às 8h em frente à escola.", false, "${dia(-2)} 14:30:00", "6º Ano A", "Ana Souza"),
        Aviso(3, "Campanha do agasalho", "Doações de roupas e cobertores podem ser entregues na secretaria até o fim do mês.", false, "${dia(-5)} 09:00:00", null, "Direção"),
    )
    private val eventos = listOf(
        Evento(2, "Prova de Matemática", "Conteúdo: frações e números decimais.", dia(5), "07:00", "Sala 100", "6º Ano A"),
        Evento(1, "Feira de Ciências", "Apresentação dos projetos das turmas.", dia(10), "08:00", "Quadra"),
        Evento(3, "Feriado — não haverá aula", null, dia(14)),
    )

    override suspend fun login(req: LoginReq) = LoginResp("demo", responsavel)
    override suspend fun me() = Me(responsavel, alunos)
    override suspend fun resumo(alunoId: Int) = Resumo(7.4, 92.5, 2, 1, eventos[0], avisos[0])
    override suspend fun avisos(alunoId: Int) = avisos
    override suspend fun eventos(alunoId: Int) = eventos
    override suspend fun horario(alunoId: Int): List<Aula> {
        val grade = listOf(
            Triple("Matemática", "Ana Souza", "Sala 100"), Triple("Português", "Carlos Lima", "Sala 101"),
            Triple("Ciências", "Ana Souza", "Laboratório"), Triple("História", "Carlos Lima", "Sala 103"),
        )
        return (1..5).flatMap { d ->
            grade.indices.map { i ->
                val (disc, prof, sala) = grade[(i + d) % grade.size]
                Aula(d, "%02d:00".format(7 + i), "%02d:50".format(7 + i), sala, disc, prof)
            }
        }
    }
    override suspend fun notas(alunoId: Int) = listOf(
        Boletim(3, "Ciências", 8.5, 9.0, null, null, 8.8),
        Boletim(4, "História", 7.0, 6.5, null, null, 6.8),
        Boletim(1, "Matemática", 5.5, 6.0, null, null, 5.8),
        Boletim(2, "Português", 9.0, 8.0, null, null, 8.5),
    )
    override suspend fun frequencia(alunoId: Int) = listOf(
        Frequencia(3, "Ciências", 20, 1, 95.0),
        Frequencia(4, "História", 18, 0, 100.0),
        Frequencia(1, "Matemática", 24, 7, 70.8),
        Frequencia(2, "Português", 22, 2, 90.9),
    )
    override suspend fun tarefas(alunoId: Int) = listOf(
        Tarefa(1, "Lista de exercícios 3", "Exercícios 1 a 15 da página 42.", dia(3), "Matemática"),
        Tarefa(2, "Redação", "Texto dissertativo sobre meio ambiente (25 linhas).", dia(7), "Português"),
        Tarefa(3, "Pesquisa sobre o Egito Antigo", "Trazer impressa, com fontes.", dia(-4), "História"),
    )
    override suspend fun financeiro(alunoId: Int): List<Mensalidade> {
        fun mes(n: Long) = hoje.plusMonths(n).withDayOfMonth(10)
        val linha = "00190.00009 01234.567890 12345.678901 1 000000085000"
        return listOf(
            Mensalidade(4, mes(1).toString().take(7), 850.0, mes(1).toString(), null, linha, "aberto"),
            Mensalidade(3, mes(0).toString().take(7), 850.0, mes(0).toString(), null, linha, if (hoje.dayOfMonth > 10) "atrasado" else "aberto"),
            Mensalidade(2, mes(-1).toString().take(7), 850.0, mes(-1).toString(), mes(-1).minusDays(2).toString(), linha, "pago"),
        )
    }
    override suspend fun cardapio(): List<ItemCardapio> {
        val pratos = listOf("Arroz, feijão, frango grelhado e salada", "Macarrão à bolonhesa e legumes", "Peixe assado, purê e salada", "Strogonoff, arroz e batata palha")
        return (0L..3L).flatMap { k ->
            listOf(
                ItemCardapio((k * 2).toInt(), dia(k), "Lanche", "Frutas, pão integral e suco natural"),
                ItemCardapio((k * 2 + 1).toInt(), dia(k), "Almoço", pratos[k.toInt()]),
            )
        }
    }

    override suspend fun avisosProfessor() = avisos
    override suspend fun eventosProfessor() = eventos
    override suspend fun disciplinas() = listOf(
        DisciplinaProfessor(1, "Matemática", 1, "6º Ano A", 28),
        DisciplinaProfessor(3, "Ciências", 1, "6º Ano A", 28),
        DisciplinaProfessor(5, "Matemática", 2, "7º Ano B", 31),
    )
    override suspend fun chamada(disciplinaId: Int, data: String) = Chamada(
        data, false,
        listOf("Ana Clara Ribeiro", "Bruno Almeida", "Carolina Dias", "Davi Martins", "Eduarda Lopes", "Felipe Rocha", "Gabriela Nunes", "João Pereira", "Maria Oliveira")
            .mapIndexed { i, n -> AlunoChamada(100 + i, n, "20250%02d".format(i + 1), if (i == 3) false else null) },
    )
    override suspend fun salvarChamada(disciplinaId: Int, req: ChamadaReq) = OkResp()
    override suspend fun criarTarefa(disciplinaId: Int, req: NovaTarefaReq) = IdResp(1)
    override suspend fun criarAviso(req: NovoAvisoReq) = IdResp(1)
}
