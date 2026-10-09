// Dados de demonstração. Uso: `npm run seed` (ou SEED_DEMO=true no servidor).
const bcrypt = require('bcryptjs');

function data(deslocDias) {
  const d = new Date();
  d.setDate(d.getDate() + deslocDias);
  return d.toISOString().slice(0, 10);
}

function popularDemo(db) {
  const senha = bcrypt.hashSync('123456', 10);
  const ins = (sql, ...a) => Number(db.prepare(sql).run(...a).lastInsertRowid);
  const usuario = (nome, email, papel, turmaId = null, matricula = null) =>
    ins('INSERT INTO usuarios (nome, email, senha_hash, papel, turma_id, matricula) VALUES (?, ?, ?, ?, ?, ?)',
      nome, email, senha, papel, turmaId, matricula);

  db.exec('BEGIN');
  try {
    const ano = new Date().getFullYear();
    const t6 = ins('INSERT INTO turmas (nome, ano, turno) VALUES (?, ?, ?)', '6º Ano A', ano, 'manha');
    const t7 = ins('INSERT INTO turmas (nome, ano, turno) VALUES (?, ?, ?)', '7º Ano B', ano, 'tarde');

    const pAna = usuario('Ana Souza', 'ana.prof@escola.local', 'professor');
    const pCarlos = usuario('Carlos Lima', 'carlos.prof@escola.local', 'professor');

    const alunos6 = [
      usuario('João Pereira', 'joao@escola.local', 'aluno', t6, '2025001'),
      usuario('Maria Oliveira', 'maria@escola.local', 'aluno', t6, '2025002'),
      usuario('Pedro Santos', 'pedro@escola.local', 'aluno', t6, '2025003'),
    ];
    const alunos7 = [
      usuario('Beatriz Costa', 'beatriz@escola.local', 'aluno', t7, '2024010'),
      usuario('Lucas Pereira', 'lucas@escola.local', 'aluno', t7, '2024011'),
    ];
    const resp = usuario('Paula Pereira', 'paula@familia.local', 'responsavel');
    ins('INSERT INTO responsavel_aluno (responsavel_id, aluno_id) VALUES (?, ?)', resp, alunos6[0]);
    ins('INSERT INTO responsavel_aluno (responsavel_id, aluno_id) VALUES (?, ?)', resp, alunos7[1]);

    const disc = {};
    for (const [turma, nomes] of [[t6, ['Matemática', 'Português', 'Ciências', 'História']], [t7, ['Matemática', 'Português', 'Geografia', 'Inglês']]]) {
      nomes.forEach((nome, i) => {
        const id = ins('INSERT INTO disciplinas (nome, turma_id, professor_id) VALUES (?, ?, ?)', nome, turma, i % 2 ? pCarlos : pAna);
        (disc[turma] ||= []).push(id);
        for (let dia = 1; dia <= 5; dia++) {
          const h = 7 + ((i + dia) % 4) + (turma === t7 ? 6 : 0);
          ins('INSERT INTO horarios (disciplina_id, dia_semana, inicio, fim, sala) VALUES (?, ?, ?, ?, ?)',
            id, dia, `${String(h).padStart(2, '0')}:00`, `${String(h).padStart(2, '0')}:50`, `Sala ${turma === t6 ? 10 : 20}${i}`);
        }
      });
    }

    let semente = 7;
    const aleatorio = () => ((semente = (semente * 9301 + 49297) % 233280) / 233280);
    for (const [turma, alunos] of [[t6, alunos6], [t7, alunos7]]) {
      for (const a of alunos) {
        for (const d of disc[turma]) {
          for (const b of [1, 2]) ins('INSERT INTO notas (aluno_id, disciplina_id, bimestre, valor) VALUES (?, ?, ?, ?)', a, d, b, Math.round((5 + aleatorio() * 5) * 10) / 10);
          for (let k = 1; k <= 10; k++) ins('INSERT INTO frequencias (aluno_id, disciplina_id, data, presente) VALUES (?, ?, ?, ?)', a, d, data(-k * 2), aleatorio() > 0.1 ? 1 : 0);
        }
        for (let m = -2; m <= 1; m++) {
          const v = new Date(); v.setMonth(v.getMonth() + m, 10);
          ins('INSERT INTO mensalidades (aluno_id, referencia, valor, vencimento, pago_em, linha_digitavel) VALUES (?, ?, ?, ?, ?, ?)',
            a, v.toISOString().slice(0, 7), 850, v.toISOString().slice(0, 10), m < 0 ? v.toISOString().slice(0, 10) : null,
            '00190.00009 01234.567890 12345.678901 1 000000085000');
        }
      }
      ins('INSERT INTO tarefas (disciplina_id, titulo, descricao, entrega) VALUES (?, ?, ?, ?)', disc[turma][0], 'Lista de exercícios 3', 'Exercícios 1 a 15 da página 42.', data(3));
      ins('INSERT INTO tarefas (disciplina_id, titulo, descricao, entrega) VALUES (?, ?, ?, ?)', disc[turma][1], 'Redação', 'Texto dissertativo sobre meio ambiente (25 linhas).', data(7));
    }

    const admin = db.prepare(`SELECT id FROM usuarios WHERE papel = 'admin'`).get()?.id ?? null;
    ins('INSERT INTO avisos (titulo, conteudo, turma_id, autor_id, importante) VALUES (?, ?, NULL, ?, 1)', 'Reunião de pais', 'Reunião de pais e mestres no sábado às 9h no auditório.', admin);
    ins('INSERT INTO avisos (titulo, conteudo, turma_id, autor_id) VALUES (?, ?, ?, ?)', 'Passeio ao museu', 'Tragam a autorização assinada até sexta-feira.', t6, pAna);
    ins('INSERT INTO eventos (titulo, descricao, data, hora, local) VALUES (?, ?, ?, ?, ?)', 'Feira de Ciências', 'Apresentação dos projetos.', data(10), '08:00', 'Quadra');
    ins('INSERT INTO eventos (titulo, descricao, data, hora, local, turma_id) VALUES (?, ?, ?, ?, ?, ?)', 'Prova de Matemática', 'Conteúdo: frações.', data(5), '07:00', 'Sala 100', t6);
    ins('INSERT INTO eventos (titulo, data) VALUES (?, ?)', 'Feriado — não haverá aula', data(14));

    const pratos = ['Arroz, feijão, frango grelhado e salada', 'Macarrão à bolonhesa e legumes', 'Peixe assado, purê e salada', 'Strogonoff, arroz e batata palha', 'Feijoada leve, couve e laranja'];
    for (let k = 0; k < 7; k++) {
      ins('INSERT INTO cardapio (data, refeicao, descricao) VALUES (?, ?, ?)', data(k), 'Lanche', 'Frutas, pão integral e suco natural');
      ins('INSERT INTO cardapio (data, refeicao, descricao) VALUES (?, ?, ?)', data(k), 'Almoço', pratos[k % pratos.length]);
    }
    db.exec('COMMIT');
  } catch (e) {
    db.exec('ROLLBACK');
    throw e;
  }
}

module.exports = { popularDemo };

if (require.main === module) {
  const { openDb } = require('./db');
  const db = openDb();
  if (!db.prepare(`SELECT 1 FROM usuarios WHERE papel = 'admin'`).get()) {
    db.prepare(`INSERT INTO usuarios (nome, email, senha_hash, papel) VALUES ('Administrador', 'admin@escola.local', ?, 'admin')`)
      .run(bcrypt.hashSync('admin123', 10));
  }
  popularDemo(db);
  console.log('Dados de demonstração inseridos. Senha de todos os usuários demo: 123456');
}
