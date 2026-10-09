// Backoffice Escola+ — SPA sem build. Telas geradas a partir de /api/admin/recursos.
const estado = { token: localStorage.getItem('token'), recursos: null, cacheOpcoes: {} };
const $ = (s) => document.querySelector(s);
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

async function api(caminho, opcoes = {}) {
  const r = await fetch(`/api${caminho}`, {
    ...opcoes,
    headers: { 'Content-Type': 'application/json', ...(estado.token ? { Authorization: `Bearer ${estado.token}` } : {}) },
    body: opcoes.body ? JSON.stringify(opcoes.body) : undefined,
  });
  const dados = await r.json().catch(() => ({}));
  if (r.status === 401 && estado.token) { sair(); throw new Error('Sessão expirada'); }
  if (!r.ok) throw new Error(dados.erro || `Erro ${r.status}`);
  return dados;
}

function sair() {
  localStorage.removeItem('token');
  estado.token = null;
  mostrar();
}

$('#form-login').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  $('#erro-login').textContent = '';
  try {
    const r = await api('/auth/login', { method: 'POST', body: { email: f.get('email'), senha: f.get('senha') } });
    if (r.usuario.papel !== 'admin') throw new Error('Acesso restrito a administradores');
    estado.token = r.token;
    localStorage.setItem('token', r.token);
    mostrar();
  } catch (err) { $('#erro-login').textContent = err.message; }
});
$('#sair').addEventListener('click', sair);
$('#cancelar').addEventListener('click', () => $('#dialogo').close());

async function mostrar() {
  const logado = !!estado.token;
  $('#tela-login').hidden = logado;
  $('#tela-app').hidden = !logado;
  if (!logado) return;
  try { estado.recursos ||= await api('/admin/recursos'); } catch { return; }
  $('#menu').innerHTML = `<a href="#painel">Painel</a>` +
    Object.entries(estado.recursos).map(([k, d]) => `<a href="#${k}">${esc(d.titulo)}</a>`).join('');
  rotear();
}
window.addEventListener('hashchange', rotear);

function rotear() {
  if (!estado.token || !estado.recursos) return;
  const rota = location.hash.slice(1) || 'painel';
  document.querySelectorAll('#menu a').forEach((a) => a.classList.toggle('ativo', a.getAttribute('href') === `#${rota}`));
  if (estado.recursos[rota]) listar(rota); else painel();
}

async function painel() {
  $('#titulo').textContent = 'Painel';
  $('#acoes').innerHTML = '';
  const d = await api('/admin/dashboard');
  const brl = (v) => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  const cartoes = [
    ['Alunos ativos', d.alunos], ['Professores', d.professores], ['Responsáveis', d.responsaveis], ['Turmas', d.turmas],
    ['Frequência geral', d.frequenciaGeral == null ? '—' : `${d.frequenciaGeral}%`],
    ['Mensalidades atrasadas', d.mensalidadesAtrasadas], ['Valor em aberto', brl(d.valorEmAberto)],
  ];
  $('#conteudo').innerHTML = `<div class="grade">${cartoes.map(([r, n]) => `<div class="cartao"><div class="num">${esc(n)}</div><div class="rot">${r}</div></div>`).join('')}</div>
    <div class="cartao"><h3>Próximos eventos</h3>${d.proximosEventos.length
      ? `<ul>${d.proximosEventos.map((e) => `<li>${esc(formatarData(e.data))} — ${esc(e.titulo)}</li>`).join('')}</ul>` : '<p>Nenhum evento agendado.</p>'}</div>`;
}

const formatarData = (iso) => (iso && /^\d{4}-\d{2}-\d{2}$/.test(iso) ? iso.split('-').reverse().join('/') : iso);

async function opcoes(campo) {
  const chave = `${campo.ref}:${campo.filtro || ''}`;
  estado.cacheOpcoes[chave] ||= await api(`/admin/${campo.ref}/opcoes${campo.filtro ? `?filtro=${campo.filtro}` : ''}`);
  return estado.cacheOpcoes[chave];
}

async function listar(nome) {
  const def = estado.recursos[nome];
  $('#titulo').textContent = def.titulo;
  $('#acoes').innerHTML = `<input id="busca" placeholder="Buscar..."><button id="novo">+ Novo</button>`;
  $('#novo').onclick = () => editar(nome, null);
  const campos = def.campos.filter((c) => !c.virtual);
  const [linhas, ...listasRef] = await Promise.all([api(`/admin/${nome}`), ...campos.map((c) => (c.tipo === 'ref' ? opcoes({ ref: c.ref }) : null))]);
  const rotuloRef = (i, v) => listasRef[i]?.find((o) => o.id === v)?.rotulo ?? v;
  const celula = (c, i, v) => {
    if (v === null || v === undefined || v === '') return '<span class="tag">—</span>';
    if (c.tipo === 'bool') return `<span class="tag ${v ? 'sim' : 'nao'}">${v ? 'Sim' : 'Não'}</span>`;
    if (c.tipo === 'ref') return esc(rotuloRef(i, v));
    if (c.tipo === 'date') return esc(formatarData(v));
    if (c.tipo === 'textarea') return esc(String(v).length > 80 ? `${String(v).slice(0, 80)}…` : v);
    return esc(v);
  };
  const desenhar = (filtro) => {
    const visiveis = linhas.filter((l) => !filtro || campos.some((c, i) => String(celula(c, i, l[c.nome])).toLowerCase().includes(filtro)));
    $('#conteudo').innerHTML = `<table><thead><tr>${campos.map((c) => `<th>${esc(c.rotulo)}</th>`).join('')}<th></th></tr></thead><tbody>
      ${visiveis.map((l) => `<tr>${campos.map((c, i) => `<td>${celula(c, i, l[c.nome])}</td>`).join('')}
        <td class="acoes"><button class="secundario pequeno" data-editar="${l.id}">Editar</button><button class="perigo pequeno" data-excluir="${l.id}">Excluir</button></td></tr>`).join('')
        || `<tr><td colspan="${campos.length + 1}">Nenhum registro.</td></tr>`}</tbody></table>`;
    $('#conteudo').querySelectorAll('[data-editar]').forEach((b) => (b.onclick = () => editar(nome, linhas.find((l) => l.id === Number(b.dataset.editar)))));
    $('#conteudo').querySelectorAll('[data-excluir]').forEach((b) => (b.onclick = async () => {
      if (!confirm('Excluir este registro?')) return;
      try { await api(`/admin/${nome}/${b.dataset.excluir}`, { method: 'DELETE' }); estado.cacheOpcoes = {}; listar(nome); } catch (err) { alert(err.message); }
    }));
  };
  $('#busca').oninput = (e) => desenhar(e.target.value.trim().toLowerCase());
  desenhar('');
}

async function editar(nome, registro) {
  const def = estado.recursos[nome];
  $('#dialogo-titulo').textContent = `${registro ? 'Editar' : 'Novo'} — ${def.titulo}`;
  $('#erro-registro').textContent = '';
  const html = [];
  for (const c of def.campos) {
    const v = registro?.[c.nome] ?? (c.tipo === 'bool' && !registro ? 1 : '');
    const req = c.obrigatorio ? 'required' : '';
    if (c.tipo === 'bool') html.push(`<label class="check"><input type="checkbox" name="${c.nome}" ${v ? 'checked' : ''}> ${esc(c.rotulo)}</label>`);
    else if (c.tipo === 'textarea') html.push(`<label>${esc(c.rotulo)}<textarea name="${c.nome}" rows="4" ${req}>${esc(v)}</textarea></label>`);
    else if (c.tipo === 'select') html.push(`<label>${esc(c.rotulo)}<select name="${c.nome}" ${req}><option value=""></option>${c.opcoes.map((o) => `<option ${o === v ? 'selected' : ''}>${esc(o)}</option>`).join('')}</select></label>`);
    else if (c.tipo === 'ref') {
      const ops = await opcoes(c);
      html.push(`<label>${esc(c.rotulo)}<select name="${c.nome}" ${req}><option value=""></option>${ops.map((o) => `<option value="${o.id}" ${o.id === v ? 'selected' : ''}>${esc(o.rotulo)}</option>`).join('')}</select></label>`);
    } else {
      const tipo = { number: 'number" step="any', date: 'date', time: 'time', password: 'password' }[c.tipo] || 'text';
      html.push(`<label>${esc(c.rotulo)}<input type="${tipo}" name="${c.nome}" value="${esc(v)}" ${c.tipo === 'password' && !registro ? 'required' : req}></label>`);
    }
  }
  $('#campos').innerHTML = html.join('');
  $('#form-registro').onsubmit = async (e) => {
    e.preventDefault();
    const corpo = {};
    for (const c of def.campos) {
      const el = e.target.elements[c.nome];
      if (c.tipo === 'bool') corpo[c.nome] = el.checked;
      else if (c.tipo === 'password') { if (el.value) corpo[c.nome] = el.value; }
      else corpo[c.nome] = el.value;
    }
    try {
      if (registro) await api(`/admin/${nome}/${registro.id}`, { method: 'PUT', body: corpo });
      else await api(`/admin/${nome}`, { method: 'POST', body: corpo });
      estado.cacheOpcoes = {};
      $('#dialogo').close();
      listar(nome);
    } catch (err) { $('#erro-registro').textContent = err.message; }
  };
  $('#dialogo').showModal();
}

mostrar();
