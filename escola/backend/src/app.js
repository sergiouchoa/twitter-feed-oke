const express = require('express');
const path = require('node:path');
const bcrypt = require('bcryptjs');
const { gerarToken, autenticar, exigirPapel } = require('./auth');
const { RECURSOS } = require('./recursos');

const hoje = () => new Date().toISOString().slice(0, 10);

function criarApp(db) {
  const app = express();
  app.use(express.json({ limit: '1mb' }));

  const auth = autenticar(db);
  const q = (sql) => db.prepare(sql);

  // ---------------------------------------------------------------- saúde
  app.get('/healthz', (_req, res) => res.json({ ok: true }));

  // ---------------------------------------------------------------- login
  app.post('/api/auth/login', (req, res) => {
    const { email, senha } = req.body || {};
    if (!email || !senha) return res.status(400).json({ erro: 'Informe e-mail e senha' });
    const u = q('SELECT * FROM usuarios WHERE lower(email) = lower(?)').get(String(email).trim());
    if (!u || !u.ativo || !bcrypt.compareSync(String(senha), u.senha_hash)) {
      return res.status(401).json({ erro: 'E-mail ou senha inválidos' });
    }
    res.json({ token: gerarToken(u), usuario: { id: u.id, nome: u.nome, email: u.email, papel: u.papel } });
  });

  // ------------------------------------------------------- perfil e alunos
  const alunosDoUsuario = (u) => {
    const base = `SELECT a.id, a.nome, a.matricula, a.turma_id AS turmaId, t.nome AS turmaNome
                  FROM usuarios a LEFT JOIN turmas t ON t.id = a.turma_id`;
    if (u.papel === 'aluno') return q(`${base} WHERE a.id = ?`).all(u.id);
    if (u.papel === 'responsavel') {
      return q(`${base} JOIN responsavel_aluno r ON r.aluno_id = a.id
                WHERE r.responsavel_id = ? AND a.ativo = 1 ORDER BY a.nome`).all(u.id);
    }
    return [];
  };

  app.get('/api/me', auth, (req, res) => {
    const { id, nome, email, papel } = req.usuario;
    res.json({ usuario: { id, nome, email, papel }, alunos: alunosDoUsuario(req.usuario) });
  });

  // Garante que o usuário logado pode ver dados do aluno :alunoId
  const acessoAluno = (req, res, next) => {
    const alunoId = Number(req.params.alunoId);
    const u = req.usuario;
    const aluno = q(`SELECT id, nome, turma_id FROM usuarios WHERE id = ? AND papel = 'aluno'`).get(alunoId);
    if (!aluno) return res.status(404).json({ erro: 'Aluno não encontrado' });
    let permitido = u.papel === 'admin' || (u.papel === 'aluno' && u.id === alunoId);
    if (u.papel === 'responsavel') {
      permitido = !!q('SELECT 1 FROM responsavel_aluno WHERE responsavel_id = ? AND aluno_id = ?').get(u.id, alunoId);
    }
    if (u.papel === 'professor') {
      permitido = !!q('SELECT 1 FROM disciplinas WHERE professor_id = ? AND turma_id = ?').get(u.id, aluno.turma_id);
    }
    if (!permitido) return res.status(403).json({ erro: 'Sem permissão para este aluno' });
    req.aluno = aluno;
    next();
  };

  const rotaAluno = (sufixo, handler) => app.get(`/api/alunos/:alunoId/${sufixo}`, auth, acessoAluno, handler);

  const avisosDaTurma = (turmaId) =>
    q(`SELECT a.id, a.titulo, a.conteudo, a.importante, a.criado_em AS criadoEm,
              t.nome AS turmaNome, u.nome AS autor
       FROM avisos a LEFT JOIN turmas t ON t.id = a.turma_id LEFT JOIN usuarios u ON u.id = a.autor_id
       WHERE a.turma_id IS NULL OR a.turma_id = ?
       ORDER BY a.importante DESC, a.criado_em DESC LIMIT 100`).all(turmaId ?? -1)
      .map((a) => ({ ...a, importante: !!a.importante }));

  const eventosDaTurma = (turmaId) =>
    q(`SELECT e.id, e.titulo, e.descricao, e.data, e.hora, e.local, t.nome AS turmaNome
       FROM eventos e LEFT JOIN turmas t ON t.id = e.turma_id
       WHERE (e.turma_id IS NULL OR e.turma_id = ?) AND e.data >= ?
       ORDER BY e.data, e.hora LIMIT 100`).all(turmaId ?? -1, hoje());

  const notasDoAluno = (aluno) => {
    const disciplinas = q('SELECT id, nome FROM disciplinas WHERE turma_id = ? ORDER BY nome').all(aluno.turma_id ?? -1);
    const notas = q('SELECT disciplina_id, bimestre, valor FROM notas WHERE aluno_id = ?').all(aluno.id);
    return disciplinas.map((d) => {
      const b = [1, 2, 3, 4].map((n) => notas.find((x) => x.disciplina_id === d.id && x.bimestre === n)?.valor ?? null);
      const lancadas = b.filter((v) => v !== null);
      const media = lancadas.length ? Math.round((lancadas.reduce((s, v) => s + v, 0) / lancadas.length) * 10) / 10 : null;
      return { disciplinaId: d.id, disciplina: d.nome, b1: b[0], b2: b[1], b3: b[2], b4: b[3], media };
    });
  };

  const frequenciaDoAluno = (aluno) =>
    q(`SELECT d.id AS disciplinaId, d.nome AS disciplina,
              COUNT(f.id) AS aulas, COALESCE(SUM(CASE WHEN f.presente = 0 THEN 1 ELSE 0 END), 0) AS faltas
       FROM disciplinas d LEFT JOIN frequencias f ON f.disciplina_id = d.id AND f.aluno_id = ?
       WHERE d.turma_id = ? GROUP BY d.id ORDER BY d.nome`).all(aluno.id, aluno.turma_id ?? -1)
      .map((r) => ({ ...r, percentual: r.aulas ? Math.round(((r.aulas - r.faltas) / r.aulas) * 1000) / 10 : 100 }));

  const tarefasDoAluno = (aluno, apenasFuturas) =>
    q(`SELECT tf.id, tf.titulo, tf.descricao, tf.entrega, d.nome AS disciplina
       FROM tarefas tf JOIN disciplinas d ON d.id = tf.disciplina_id
       WHERE d.turma_id = ? ${apenasFuturas ? 'AND tf.entrega >= ?' : ''}
       ORDER BY tf.entrega`).all(...[aluno.turma_id ?? -1, ...(apenasFuturas ? [hoje()] : [])]);

  const financeiroDoAluno = (aluno) =>
    q(`SELECT id, referencia, valor, vencimento, pago_em AS pagoEm, linha_digitavel AS linhaDigitavel
       FROM mensalidades WHERE aluno_id = ? ORDER BY vencimento DESC`).all(aluno.id)
      .map((m) => ({ ...m, situacao: m.pagoEm ? 'pago' : m.vencimento < hoje() ? 'atrasado' : 'aberto' }));

  rotaAluno('resumo', (req, res) => {
    const notas = notasDoAluno(req.aluno).filter((n) => n.media !== null);
    const freq = frequenciaDoAluno(req.aluno);
    const aulas = freq.reduce((s, f) => s + f.aulas, 0);
    const faltas = freq.reduce((s, f) => s + f.faltas, 0);
    const financeiro = financeiroDoAluno(req.aluno);
    res.json({
      mediaGeral: notas.length ? Math.round((notas.reduce((s, n) => s + n.media, 0) / notas.length) * 10) / 10 : null,
      frequencia: aulas ? Math.round(((aulas - faltas) / aulas) * 1000) / 10 : 100,
      tarefasPendentes: tarefasDoAluno(req.aluno, true).length,
      mensalidadesEmAberto: financeiro.filter((m) => m.situacao !== 'pago').length,
      proximoEvento: eventosDaTurma(req.aluno.turma_id)[0] || null,
      ultimoAviso: avisosDaTurma(req.aluno.turma_id)[0] || null,
    });
  });
  rotaAluno('avisos', (req, res) => res.json(avisosDaTurma(req.aluno.turma_id)));
  rotaAluno('eventos', (req, res) => res.json(eventosDaTurma(req.aluno.turma_id)));
  rotaAluno('horario', (req, res) =>
    res.json(q(`SELECT h.dia_semana AS diaSemana, h.inicio, h.fim, h.sala, d.nome AS disciplina, p.nome AS professor
                FROM horarios h JOIN disciplinas d ON d.id = h.disciplina_id LEFT JOIN usuarios p ON p.id = d.professor_id
                WHERE d.turma_id = ? ORDER BY h.dia_semana, h.inicio`).all(req.aluno.turma_id ?? -1)));
  rotaAluno('notas', (req, res) => res.json(notasDoAluno(req.aluno)));
  rotaAluno('frequencia', (req, res) => res.json(frequenciaDoAluno(req.aluno)));
  rotaAluno('tarefas', (req, res) => res.json(tarefasDoAluno(req.aluno, false)));
  rotaAluno('financeiro', (req, res) => res.json(financeiroDoAluno(req.aluno)));

  // ------------------------------------------------------ comum a todos
  app.get('/api/cardapio', auth, (req, res) => {
    const de = /^\d{4}-\d{2}-\d{2}$/.test(req.query.de || '') ? req.query.de : hoje();
    res.json(q(`SELECT id, data, refeicao, descricao FROM cardapio
                WHERE data >= ? AND data < date(?, '+7 day') ORDER BY data, id`).all(de, de));
  });

  // Professores/admin: comunicados e eventos gerais + das turmas em que dão aula
  const turmasDoProfessor = (u) =>
    u.papel === 'admin'
      ? q('SELECT id FROM turmas').all().map((t) => t.id)
      : q('SELECT DISTINCT turma_id AS id FROM disciplinas WHERE professor_id = ?').all(u.id).map((t) => t.id);

  app.get('/api/avisos', auth, exigirPapel('professor', 'admin'), (req, res) => {
    const ids = turmasDoProfessor(req.usuario);
    const vistos = new Map();
    for (const id of [null, ...ids]) for (const a of avisosDaTurma(id)) vistos.set(a.id, a);
    res.json([...vistos.values()].sort((a, b) => b.importante - a.importante || b.criadoEm.localeCompare(a.criadoEm)));
  });

  app.get('/api/eventos', auth, exigirPapel('professor', 'admin'), (req, res) => {
    const ids = turmasDoProfessor(req.usuario);
    const vistos = new Map();
    for (const id of [null, ...ids]) for (const e of eventosDaTurma(id)) vistos.set(e.id, e);
    res.json([...vistos.values()].sort((a, b) => (a.data + (a.hora || '')).localeCompare(b.data + (b.hora || ''))));
  });

  // ------------------------------------------------------------ professor
  const prof = [auth, exigirPapel('professor', 'admin')];

  const disciplinaDoProfessor = (req, res, next) => {
    const d = q(`SELECT d.*, t.nome AS turma_nome FROM disciplinas d JOIN turmas t ON t.id = d.turma_id WHERE d.id = ?`)
      .get(Number(req.params.disciplinaId));
    if (!d) return res.status(404).json({ erro: 'Disciplina não encontrada' });
    if (req.usuario.papel !== 'admin' && d.professor_id !== req.usuario.id) {
      return res.status(403).json({ erro: 'Disciplina de outro professor' });
    }
    req.disciplina = d;
    next();
  };

  app.get('/api/professor/disciplinas', ...prof, (req, res) => {
    const filtro = req.usuario.papel === 'admin' ? '' : 'WHERE d.professor_id = ?';
    const args = req.usuario.papel === 'admin' ? [] : [req.usuario.id];
    res.json(q(`SELECT d.id, d.nome, d.turma_id AS turmaId, t.nome AS turmaNome,
                       (SELECT COUNT(*) FROM usuarios a WHERE a.turma_id = d.turma_id AND a.papel = 'aluno' AND a.ativo = 1) AS totalAlunos
                FROM disciplinas d JOIN turmas t ON t.id = d.turma_id ${filtro} ORDER BY t.nome, d.nome`).all(...args));
  });

  app.get('/api/professor/disciplinas/:disciplinaId/chamada', ...prof, disciplinaDoProfessor, (req, res) => {
    const data = /^\d{4}-\d{2}-\d{2}$/.test(req.query.data || '') ? req.query.data : hoje();
    const linhas = q(`SELECT a.id AS alunoId, a.nome, a.matricula, f.presente
                      FROM usuarios a LEFT JOIN frequencias f ON f.aluno_id = a.id AND f.disciplina_id = ? AND f.data = ?
                      WHERE a.turma_id = ? AND a.papel = 'aluno' AND a.ativo = 1 ORDER BY a.nome`)
      .all(req.disciplina.id, data, req.disciplina.turma_id);
    res.json({ data, registrada: linhas.some((l) => l.presente !== null),
      alunos: linhas.map((l) => ({ ...l, presente: l.presente === null ? null : !!l.presente })) });
  });

  app.post('/api/professor/disciplinas/:disciplinaId/chamada', ...prof, disciplinaDoProfessor, (req, res) => {
    const { data, presencas } = req.body || {};
    if (!/^\d{4}-\d{2}-\d{2}$/.test(data || '') || !Array.isArray(presencas)) {
      return res.status(400).json({ erro: 'Envie data (AAAA-MM-DD) e presencas[]' });
    }
    const daTurma = new Set(q(`SELECT id FROM usuarios WHERE turma_id = ? AND papel = 'aluno'`)
      .all(req.disciplina.turma_id).map((a) => a.id));
    const salvar = q(`INSERT INTO frequencias (aluno_id, disciplina_id, data, presente) VALUES (?, ?, ?, ?)
                      ON CONFLICT (aluno_id, disciplina_id, data) DO UPDATE SET presente = excluded.presente`);
    db.exec('BEGIN');
    try {
      for (const p of presencas) {
        if (daTurma.has(Number(p.alunoId))) salvar.run(Number(p.alunoId), req.disciplina.id, data, p.presente ? 1 : 0);
      }
      db.exec('COMMIT');
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
    res.json({ ok: true });
  });

  app.post('/api/professor/disciplinas/:disciplinaId/tarefas', ...prof, disciplinaDoProfessor, (req, res) => {
    const { titulo, descricao, entrega } = req.body || {};
    if (!titulo || !/^\d{4}-\d{2}-\d{2}$/.test(entrega || '')) return res.status(400).json({ erro: 'Informe título e data de entrega' });
    const r = q('INSERT INTO tarefas (disciplina_id, titulo, descricao, entrega) VALUES (?, ?, ?, ?)')
      .run(req.disciplina.id, titulo, descricao || null, entrega);
    res.status(201).json({ id: Number(r.lastInsertRowid) });
  });

  app.post('/api/professor/avisos', ...prof, (req, res) => {
    const { titulo, conteudo, turmaId } = req.body || {};
    if (!titulo || !conteudo) return res.status(400).json({ erro: 'Informe título e conteúdo' });
    const turmas = turmasDoProfessor(req.usuario);
    if (turmaId == null && req.usuario.papel !== 'admin') return res.status(403).json({ erro: 'Professores publicam apenas para suas turmas' });
    if (turmaId != null && !turmas.includes(Number(turmaId))) return res.status(403).json({ erro: 'Turma não permitida' });
    const r = q('INSERT INTO avisos (titulo, conteudo, turma_id, autor_id) VALUES (?, ?, ?, ?)')
      .run(titulo, conteudo, turmaId == null ? null : Number(turmaId), req.usuario.id);
    res.status(201).json({ id: Number(r.lastInsertRowid) });
  });

  // --------------------------------------------------------- backoffice
  const admin = [auth, exigirPapel('admin')];

  app.get('/api/admin/recursos', ...admin, (_req, res) => res.json(RECURSOS));

  app.get('/api/admin/dashboard', ...admin, (_req, res) => {
    const c = (sql, ...a) => q(sql).get(...a).n;
    res.json({
      alunos: c(`SELECT COUNT(*) n FROM usuarios WHERE papel = 'aluno' AND ativo = 1`),
      professores: c(`SELECT COUNT(*) n FROM usuarios WHERE papel = 'professor' AND ativo = 1`),
      responsaveis: c(`SELECT COUNT(*) n FROM usuarios WHERE papel = 'responsavel' AND ativo = 1`),
      turmas: c('SELECT COUNT(*) n FROM turmas'),
      mensalidadesAtrasadas: c('SELECT COUNT(*) n FROM mensalidades WHERE pago_em IS NULL AND vencimento < ?', hoje()),
      valorEmAberto: q('SELECT COALESCE(SUM(valor), 0) n FROM mensalidades WHERE pago_em IS NULL').get().n,
      frequenciaGeral: (() => {
        const r = q('SELECT COUNT(*) total, COALESCE(SUM(presente), 0) presentes FROM frequencias').get();
        return r.total ? Math.round((r.presentes / r.total) * 1000) / 10 : null;
      })(),
      proximosEventos: q('SELECT titulo, data FROM eventos WHERE data >= ? ORDER BY data LIMIT 5').all(hoje()),
    });
  });

  const recurso = (req, res, next) => {
    const def = RECURSOS[req.params.recurso];
    if (!def) return res.status(404).json({ erro: 'Recurso inexistente' });
    req.def = def;
    next();
  };

  const ROTULOS = {
    turmas: `SELECT id, nome || ' (' || ano || ')' AS rotulo FROM turmas`,
    usuarios: `SELECT id, nome || ' — ' || papel AS rotulo FROM usuarios`,
    disciplinas: `SELECT d.id, d.nome || ' — ' || t.nome AS rotulo FROM disciplinas d JOIN turmas t ON t.id = d.turma_id`,
  };

  app.get('/api/admin/:recurso/opcoes', ...admin, recurso, (req, res) => {
    const base = ROTULOS[req.params.recurso];
    if (!base) return res.status(400).json({ erro: 'Recurso não referenciável' });
    const filtro = RECURSOS.usuarios.filtroRef[req.query.filtro];
    const where = req.params.recurso === 'usuarios' && filtro ? ` WHERE ${filtro}` : '';
    res.json(q(`${base}${where} ORDER BY rotulo`).all());
  });

  app.get('/api/admin/:recurso', ...admin, recurso, (req, res) => {
    const cols = ['id', ...req.def.campos.filter((c) => !c.virtual).map((c) => c.nome)];
    res.json(q(`SELECT ${cols.join(', ')} FROM ${req.params.recurso} ORDER BY id DESC LIMIT 1000`).all());
  });

  const valoresDoCorpo = (def, corpo, parcial) => {
    const vals = {};
    for (const c of def.campos) {
      if (c.virtual) continue;
      if (!(c.nome in corpo)) {
        if (!parcial && c.obrigatorio) throw Object.assign(new Error(`Campo obrigatório: ${c.rotulo}`), { status: 400 });
        continue;
      }
      let v = corpo[c.nome];
      if (v === '' || v === undefined) v = null;
      if (v !== null && (c.tipo === 'number' || c.tipo === 'ref')) v = Number(v);
      if (c.tipo === 'bool') v = v ? 1 : 0;
      if (v === null && c.obrigatorio) throw Object.assign(new Error(`Campo obrigatório: ${c.rotulo}`), { status: 400 });
      vals[c.nome] = v;
    }
    return vals;
  };

  app.post('/api/admin/:recurso', ...admin, recurso, (req, res) => {
    const vals = valoresDoCorpo(req.def, req.body || {}, false);
    if (req.params.recurso === 'usuarios') {
      if (!req.body.senha) return res.status(400).json({ erro: 'Defina uma senha para o novo usuário' });
      vals.senha_hash = bcrypt.hashSync(String(req.body.senha), 10);
      if (!('ativo' in req.body)) vals.ativo = 1;
    }
    const cols = Object.keys(vals);
    const r = q(`INSERT INTO ${req.params.recurso} (${cols.join(', ')}) VALUES (${cols.map(() => '?').join(', ')})`)
      .run(...cols.map((c) => vals[c]));
    res.status(201).json({ id: Number(r.lastInsertRowid) });
  });

  app.put('/api/admin/:recurso/:id', ...admin, recurso, (req, res) => {
    const vals = valoresDoCorpo(req.def, req.body || {}, true);
    if (req.params.recurso === 'usuarios' && req.body.senha) vals.senha_hash = bcrypt.hashSync(String(req.body.senha), 10);
    const cols = Object.keys(vals);
    if (!cols.length) return res.status(400).json({ erro: 'Nada para atualizar' });
    const r = q(`UPDATE ${req.params.recurso} SET ${cols.map((c) => `${c} = ?`).join(', ')} WHERE id = ?`)
      .run(...cols.map((c) => vals[c]), Number(req.params.id));
    if (!r.changes) return res.status(404).json({ erro: 'Registro não encontrado' });
    res.json({ ok: true });
  });

  app.delete('/api/admin/:recurso/:id', ...admin, recurso, (req, res) => {
    if (req.params.recurso === 'usuarios' && Number(req.params.id) === req.usuario.id) {
      return res.status(400).json({ erro: 'Você não pode excluir o próprio usuário' });
    }
    const r = q(`DELETE FROM ${req.params.recurso} WHERE id = ?`).run(Number(req.params.id));
    if (!r.changes) return res.status(404).json({ erro: 'Registro não encontrado' });
    res.json({ ok: true });
  });

  // ------------------------------------------------- backoffice (web)
  app.use('/', express.static(path.join(__dirname, '..', 'public')));

  app.use('/api', (_req, res) => res.status(404).json({ erro: 'Rota não encontrada' }));

  // eslint-disable-next-line no-unused-vars
  app.use((err, _req, res, _next) => {
    if (err.status) return res.status(err.status).json({ erro: err.message });
    if (/constraint/i.test(err.message || '')) {
      return res.status(400).json({ erro: `Dados inválidos: ${err.message.replace(/^.*constraint failed: /i, '')}` });
    }
    console.error(err);
    res.status(500).json({ erro: 'Erro interno' });
  });

  return app;
}

module.exports = { criarApp };
