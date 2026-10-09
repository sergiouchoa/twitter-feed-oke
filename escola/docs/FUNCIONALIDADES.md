# Escola+ — Funcionalidades e telas

O Escola+ reúne num só app Android tudo o que alunos, responsáveis e professores usam no dia a dia,
gerenciado por um backoffice web na nuvem. As capturas do app são geradas a partir do código
(`./gradlew recordRoborazziDebug`) com dados de demonstração.

## App do aluno e do responsável

O responsável vê todos os filhos vinculados e troca entre eles no topo da tela inicial.

| Tela | O que mostra |
| --- | --- |
| Login | E-mail e senha; endereço do servidor configurável |
| Início | Média geral, frequência, tarefas pendentes, mensalidades em aberto, próximo evento, último comunicado e atalhos |
| Comunicados | Avisos gerais e da turma, com destaque "Importante" |
| Agenda | Provas, eventos e feriados com hora e local |
| Horário | Aulas da semana, com professor e sala; o dia atual marcado |
| Boletim | Notas dos 4 bimestres e média por disciplina |
| Frequência | Presença por disciplina, com alerta abaixo de 75% |
| Tarefas | A entregar e anteriores, com prazo |
| Cardápio | Refeições dos próximos 7 dias |
| Financeiro | Mensalidades pagas, em aberto e atrasadas; copiar código de pagamento |

| Login | Início | Comunicados |
| --- | --- | --- |
| <img src="capturas/01_login.png" width="240"> | <img src="capturas/02_inicio_responsavel.png" width="240"> | <img src="capturas/03_comunicados.png" width="240"> |
| **Agenda** | **Horário** | **Boletim** |
| <img src="capturas/04_agenda.png" width="240"> | <img src="capturas/05_horario.png" width="240"> | <img src="capturas/06_boletim.png" width="240"> |
| **Frequência** | **Tarefas** | **Cardápio** |
| <img src="capturas/07_frequencia.png" width="240"> | <img src="capturas/08_tarefas.png" width="240"> | <img src="capturas/09_cardapio.png" width="240"> |
| **Financeiro** | | |
| <img src="capturas/10_financeiro.png" width="240"> | | |

## App do professor

| Tela | O que faz |
| --- | --- |
| Início | Disciplinas e turmas com total de alunos |
| Chamada | Todos presentes por padrão; um toque marca falta; navega entre dias e corrige chamadas |
| Nova tarefa | Título, descrição e data de entrega |
| Novo comunicado | Publica aviso para uma das suas turmas |

| Início | Chamada | Nova tarefa | Novo comunicado |
| --- | --- | --- | --- |
| <img src="capturas/11_inicio_professor.png" width="200"> | <img src="capturas/12_chamada.png" width="200"> | <img src="capturas/13_nova_tarefa.png" width="200"> | <img src="capturas/14_novo_comunicado.png" width="200"> |

## Backoffice da secretaria

Painel com indicadores e cadastro de turmas, usuários, vínculos responsável↔aluno, disciplinas,
horários, comunicados, eventos, tarefas, notas, frequência, cardápio e mensalidades, com busca.

**Painel**

<img src="capturas/bo_02_painel.png" width="720">

**Usuários**

<img src="capturas/bo_03_usuarios.png" width="720">

**Comunicados**

<img src="capturas/bo_04_comunicados.png" width="720">

**Lançamento de nota**

<img src="capturas/bo_05_lancar_nota.png" width="720">

**Financeiro**

<img src="capturas/bo_06_financeiro.png" width="720">
