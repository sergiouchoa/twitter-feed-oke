const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const bcrypt = require('bcryptjs');
const { openDb } = require('../src/db');
const { criarApp } = require('../src/app');
const { popularDemo } = require('../src/seed');

let servidor, base;

before(async () => {
  const db = openDb(':memory:');
  db.prepare(`INSERT INTO usuarios (nome, email, senha_hash, papel) VALUES ('Admin', 'admin@escola.local', ?, 'admin')`)
    .run(bcrypt.hashSync('admin123', 4));
  popularDemo(db);
  servidor = criarApp(db).listen(0);
  await new Promise((r) => servidor.once('listening', r));
  base = `http://127.0.0.1:${servidor.address().port}`;
});
after(() => servidor.close());

async function req(caminho, { token, method = 'GET', body } = {}) {
  const r = await fetch(base + caminho, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: r.status, dados: await r.json().catch(() => null) };
}
const login = async (email, senha = '123456') => (await req('/api/auth/login', { method: 'POST', body: { email, senha } })).dados;

test('login inválido é rejeitado', async () => {
  assert.equal((await req('/api/auth/login', { method: 'POST', body: { email: 'joao@escola.local', senha: 'x' } })).status, 401);
});

test('responsável vê os dependentes e os dados deles', async () => {
  const { token } = await login('paula@familia.local');
  const me = (await req('/api/me', { token })).dados;
  assert.equal(me.usuario.papel, 'responsavel');
  assert.deepEqual(me.alunos.map((a) => a.nome).sort(), ['João Pereira', 'Lucas Pereira']);
  const joao = me.alunos.find((a) => a.nome === 'João Pereira');
  for (const rota of ['resumo', 'avisos', 'eventos', 'horario', 'notas', 'frequencia', 'tarefas', 'financeiro']) {
    const r = await req(`/api/alunos/${joao.id}/${rota}`, { token });
    assert.equal(r.status, 200, rota);
  }
  const notas = (await req(`/api/alunos/${joao.id}/notas`, { token })).dados;
  assert.equal(notas.length, 4);
  assert.ok(notas[0].media >= 5);
  const fin = (await req(`/api/alunos/${joao.id}/financeiro`, { token })).dados;
  assert.ok(fin.some((m) => m.situacao === 'pago'));
});

test('responsável não acessa aluno de outra família', async () => {
  const { token } = await login('paula@familia.local');
  const maria = (await login('maria@escola.local')).usuario;
  assert.equal((await req(`/api/alunos/${maria.id}/notas`, { token })).status, 403);
});

test('aluno só vê os próprios dados e não acessa o backoffice', async () => {
  const { token, usuario } = await login('maria@escola.local');
  assert.equal((await req(`/api/alunos/${usuario.id}/horario`, { token })).status, 200);
  const joao = (await login('joao@escola.local')).usuario;
  assert.equal((await req(`/api/alunos/${joao.id}/horario`, { token })).status, 403);
  assert.equal((await req('/api/admin/turmas', { token })).status, 403);
  assert.equal((await req('/api/cardapio', { token })).dados.length, 14);
});

test('professor faz chamada, cria tarefa e publica aviso', async () => {
  const { token } = await login('ana.prof@escola.local');
  const disc = (await req('/api/professor/disciplinas', { token })).dados;
  assert.ok(disc.length > 0);
  const d = disc[0];
  const chamada = (await req(`/api/professor/disciplinas/${d.id}/chamada?data=2030-01-10`, { token })).dados;
  assert.equal(chamada.registrada, false);
  const presencas = chamada.alunos.map((a, i) => ({ alunoId: a.alunoId, presente: i !== 0 }));
  assert.equal((await req(`/api/professor/disciplinas/${d.id}/chamada`, { token, method: 'POST', body: { data: '2030-01-10', presencas } })).status, 200);
  const depois = (await req(`/api/professor/disciplinas/${d.id}/chamada?data=2030-01-10`, { token })).dados;
  assert.equal(depois.registrada, true);
  assert.equal(depois.alunos[0].presente, false);

  assert.equal((await req(`/api/professor/disciplinas/${d.id}/tarefas`, { token, method: 'POST', body: { titulo: 'Nova', entrega: '2030-02-01' } })).status, 201);
  assert.equal((await req('/api/professor/avisos', { token, method: 'POST', body: { titulo: 'Oi', conteudo: 'Turma', turmaId: d.turmaId } })).status, 201);
  assert.equal((await req('/api/professor/avisos', { token, method: 'POST', body: { titulo: 'Geral', conteudo: 'x' } })).status, 403);

  const outra = (await req('/api/professor/disciplinas', { token: (await login('carlos.prof@escola.local')).token })).dados[0];
  assert.equal((await req(`/api/professor/disciplinas/${outra.id}/chamada`, { token })).status, 403);
});

test('backoffice: CRUD genérico e criação de usuário', async () => {
  const { token } = await login('admin@escola.local', 'admin123');
  assert.ok((await req('/api/admin/recursos', { token })).dados.turmas);
  const dash = (await req('/api/admin/dashboard', { token })).dados;
  assert.equal(dash.alunos, 5);

  const t = await req('/api/admin/turmas', { token, method: 'POST', body: { nome: '8º Ano', ano: 2026, turno: 'manha' } });
  assert.equal(t.status, 201);
  assert.equal((await req(`/api/admin/turmas/${t.dados.id}`, { token, method: 'PUT', body: { nome: '8º Ano C' } })).status, 200);
  assert.equal((await req('/api/admin/turmas', { token, method: 'POST', body: { nome: 'Sem ano' } })).status, 400);

  const u = await req('/api/admin/usuarios', { token, method: 'POST', body: { nome: 'Nova', email: 'nova@escola.local', senha: 'abc', papel: 'aluno', turma_id: t.dados.id } });
  assert.equal(u.status, 201);
  const lista = (await req('/api/admin/usuarios', { token })).dados;
  assert.ok(lista.every((x) => !('senha_hash' in x)));
  assert.ok((await login('nova@escola.local', 'abc')).token);
  assert.equal((await req('/api/admin/usuarios', { token, method: 'POST', body: { nome: 'X', email: 'x@x', senha: 'a', papel: 'rei' } })).status, 400);

  assert.equal((await req(`/api/admin/turmas/${t.dados.id}`, { token, method: 'DELETE' })).status, 200);
  assert.equal((await req('/api/admin/naoexiste', { token })).status, 404);
  const ops = (await req('/api/admin/usuarios/opcoes?filtro=professor', { token })).dados;
  assert.equal(ops.length, 2);
});
