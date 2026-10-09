// Definição dos recursos editáveis no backoffice.
// A interface web (public/app.js) monta tabelas e formulários a partir daqui,
// e as rotas /api/admin/:recurso só aceitam as colunas listadas.
//
// tipos: text, textarea, number, date, time, bool, select (opcoes), ref (tabela relacionada)

const RECURSOS = {
  turmas: {
    titulo: 'Turmas',
    campos: [
      { nome: 'nome', rotulo: 'Nome', tipo: 'text', obrigatorio: true },
      { nome: 'ano', rotulo: 'Ano letivo', tipo: 'number', obrigatorio: true },
      { nome: 'turno', rotulo: 'Turno', tipo: 'select', opcoes: ['manha', 'tarde', 'noite', 'integral'], obrigatorio: true },
    ],
    exibir: 'nome',
  },
  usuarios: {
    titulo: 'Usuários',
    campos: [
      { nome: 'nome', rotulo: 'Nome', tipo: 'text', obrigatorio: true },
      { nome: 'email', rotulo: 'E-mail', tipo: 'text', obrigatorio: true },
      { nome: 'senha', rotulo: 'Senha (deixe vazio para manter)', tipo: 'password', virtual: true },
      { nome: 'papel', rotulo: 'Perfil', tipo: 'select', opcoes: ['aluno', 'responsavel', 'professor', 'admin'], obrigatorio: true },
      { nome: 'turma_id', rotulo: 'Turma (alunos)', tipo: 'ref', ref: 'turmas' },
      { nome: 'matricula', rotulo: 'Matrícula', tipo: 'text' },
      { nome: 'telefone', rotulo: 'Telefone', tipo: 'text' },
      { nome: 'ativo', rotulo: 'Ativo', tipo: 'bool' },
    ],
    exibir: 'nome',
    filtroRef: { aluno: "papel = 'aluno'", professor: "papel = 'professor'", responsavel: "papel = 'responsavel'" },
  },
  responsavel_aluno: {
    titulo: 'Vínculos responsável ↔ aluno',
    campos: [
      { nome: 'responsavel_id', rotulo: 'Responsável', tipo: 'ref', ref: 'usuarios', filtro: 'responsavel', obrigatorio: true },
      { nome: 'aluno_id', rotulo: 'Aluno', tipo: 'ref', ref: 'usuarios', filtro: 'aluno', obrigatorio: true },
    ],
  },
  disciplinas: {
    titulo: 'Disciplinas',
    campos: [
      { nome: 'nome', rotulo: 'Nome', tipo: 'text', obrigatorio: true },
      { nome: 'turma_id', rotulo: 'Turma', tipo: 'ref', ref: 'turmas', obrigatorio: true },
      { nome: 'professor_id', rotulo: 'Professor', tipo: 'ref', ref: 'usuarios', filtro: 'professor' },
    ],
    exibir: 'nome',
  },
  horarios: {
    titulo: 'Horário de aulas',
    campos: [
      { nome: 'disciplina_id', rotulo: 'Disciplina', tipo: 'ref', ref: 'disciplinas', obrigatorio: true },
      { nome: 'dia_semana', rotulo: 'Dia (1=seg … 7=dom)', tipo: 'number', obrigatorio: true },
      { nome: 'inicio', rotulo: 'Início', tipo: 'time', obrigatorio: true },
      { nome: 'fim', rotulo: 'Fim', tipo: 'time', obrigatorio: true },
      { nome: 'sala', rotulo: 'Sala', tipo: 'text' },
    ],
  },
  avisos: {
    titulo: 'Comunicados',
    campos: [
      { nome: 'titulo', rotulo: 'Título', tipo: 'text', obrigatorio: true },
      { nome: 'conteudo', rotulo: 'Conteúdo', tipo: 'textarea', obrigatorio: true },
      { nome: 'turma_id', rotulo: 'Turma (vazio = toda a escola)', tipo: 'ref', ref: 'turmas' },
      { nome: 'importante', rotulo: 'Importante', tipo: 'bool' },
    ],
  },
  eventos: {
    titulo: 'Agenda / Eventos',
    campos: [
      { nome: 'titulo', rotulo: 'Título', tipo: 'text', obrigatorio: true },
      { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea' },
      { nome: 'data', rotulo: 'Data', tipo: 'date', obrigatorio: true },
      { nome: 'hora', rotulo: 'Hora', tipo: 'time' },
      { nome: 'local', rotulo: 'Local', tipo: 'text' },
      { nome: 'turma_id', rotulo: 'Turma (vazio = toda a escola)', tipo: 'ref', ref: 'turmas' },
    ],
  },
  tarefas: {
    titulo: 'Tarefas',
    campos: [
      { nome: 'disciplina_id', rotulo: 'Disciplina', tipo: 'ref', ref: 'disciplinas', obrigatorio: true },
      { nome: 'titulo', rotulo: 'Título', tipo: 'text', obrigatorio: true },
      { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea' },
      { nome: 'entrega', rotulo: 'Entrega', tipo: 'date', obrigatorio: true },
    ],
  },
  notas: {
    titulo: 'Notas',
    campos: [
      { nome: 'aluno_id', rotulo: 'Aluno', tipo: 'ref', ref: 'usuarios', filtro: 'aluno', obrigatorio: true },
      { nome: 'disciplina_id', rotulo: 'Disciplina', tipo: 'ref', ref: 'disciplinas', obrigatorio: true },
      { nome: 'bimestre', rotulo: 'Bimestre', tipo: 'number', obrigatorio: true },
      { nome: 'valor', rotulo: 'Nota (0–10)', tipo: 'number', obrigatorio: true },
    ],
  },
  frequencias: {
    titulo: 'Frequência',
    campos: [
      { nome: 'aluno_id', rotulo: 'Aluno', tipo: 'ref', ref: 'usuarios', filtro: 'aluno', obrigatorio: true },
      { nome: 'disciplina_id', rotulo: 'Disciplina', tipo: 'ref', ref: 'disciplinas', obrigatorio: true },
      { nome: 'data', rotulo: 'Data', tipo: 'date', obrigatorio: true },
      { nome: 'presente', rotulo: 'Presente', tipo: 'bool' },
    ],
  },
  cardapio: {
    titulo: 'Cardápio',
    campos: [
      { nome: 'data', rotulo: 'Data', tipo: 'date', obrigatorio: true },
      { nome: 'refeicao', rotulo: 'Refeição', tipo: 'select', opcoes: ['Café da manhã', 'Lanche', 'Almoço', 'Lanche da tarde'], obrigatorio: true },
      { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea', obrigatorio: true },
    ],
  },
  mensalidades: {
    titulo: 'Financeiro',
    campos: [
      { nome: 'aluno_id', rotulo: 'Aluno', tipo: 'ref', ref: 'usuarios', filtro: 'aluno', obrigatorio: true },
      { nome: 'referencia', rotulo: 'Referência (AAAA-MM)', tipo: 'text', obrigatorio: true },
      { nome: 'valor', rotulo: 'Valor (R$)', tipo: 'number', obrigatorio: true },
      { nome: 'vencimento', rotulo: 'Vencimento', tipo: 'date', obrigatorio: true },
      { nome: 'pago_em', rotulo: 'Pago em', tipo: 'date' },
      { nome: 'linha_digitavel', rotulo: 'Linha digitável / PIX', tipo: 'text' },
    ],
  },
};

module.exports = { RECURSOS };
