const { DatabaseSync } = require('node:sqlite');
const path = require('node:path');
const fs = require('node:fs');

const SCHEMA = `
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS turmas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  ano INTEGER NOT NULL,
  turno TEXT NOT NULL DEFAULT 'manha'
);

CREATE TABLE IF NOT EXISTS usuarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  senha_hash TEXT NOT NULL,
  papel TEXT NOT NULL CHECK (papel IN ('admin','professor','aluno','responsavel')),
  turma_id INTEGER REFERENCES turmas(id) ON DELETE SET NULL,
  matricula TEXT,
  telefone TEXT,
  ativo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS responsavel_aluno (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  responsavel_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  aluno_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  UNIQUE (responsavel_id, aluno_id)
);

CREATE TABLE IF NOT EXISTS disciplinas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  turma_id INTEGER NOT NULL REFERENCES turmas(id) ON DELETE CASCADE,
  professor_id INTEGER REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS horarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  disciplina_id INTEGER NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
  dia_semana INTEGER NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
  inicio TEXT NOT NULL,
  fim TEXT NOT NULL,
  sala TEXT
);

CREATE TABLE IF NOT EXISTS avisos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  titulo TEXT NOT NULL,
  conteudo TEXT NOT NULL,
  turma_id INTEGER REFERENCES turmas(id) ON DELETE CASCADE,
  autor_id INTEGER REFERENCES usuarios(id) ON DELETE SET NULL,
  importante INTEGER NOT NULL DEFAULT 0,
  criado_em TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS eventos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  titulo TEXT NOT NULL,
  descricao TEXT,
  data TEXT NOT NULL,
  hora TEXT,
  local TEXT,
  turma_id INTEGER REFERENCES turmas(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tarefas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  disciplina_id INTEGER NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
  titulo TEXT NOT NULL,
  descricao TEXT,
  entrega TEXT NOT NULL,
  criado_em TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS notas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  disciplina_id INTEGER NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
  bimestre INTEGER NOT NULL CHECK (bimestre BETWEEN 1 AND 4),
  valor REAL NOT NULL CHECK (valor BETWEEN 0 AND 10),
  UNIQUE (aluno_id, disciplina_id, bimestre)
);

CREATE TABLE IF NOT EXISTS frequencias (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  disciplina_id INTEGER NOT NULL REFERENCES disciplinas(id) ON DELETE CASCADE,
  data TEXT NOT NULL,
  presente INTEGER NOT NULL DEFAULT 1,
  UNIQUE (aluno_id, disciplina_id, data)
);

CREATE TABLE IF NOT EXISTS cardapio (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  data TEXT NOT NULL,
  refeicao TEXT NOT NULL,
  descricao TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS mensalidades (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  aluno_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  referencia TEXT NOT NULL,
  valor REAL NOT NULL,
  vencimento TEXT NOT NULL,
  pago_em TEXT,
  linha_digitavel TEXT
);
`;

function openDb(file = process.env.DB_PATH || path.join(__dirname, '..', 'data', 'escola.db')) {
  if (file !== ':memory:') fs.mkdirSync(path.dirname(file), { recursive: true });
  const db = new DatabaseSync(file);
  db.exec(SCHEMA);
  return db;
}

module.exports = { openDb };
