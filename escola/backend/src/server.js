const bcrypt = require('bcryptjs');
const { openDb } = require('./db');
const { criarApp } = require('./app');
const { popularDemo } = require('./seed');

const db = openDb();

// Cria o primeiro administrador se ainda não existir nenhum
if (!db.prepare(`SELECT 1 FROM usuarios WHERE papel = 'admin'`).get()) {
  const email = process.env.ADMIN_EMAIL || 'admin@escola.local';
  const senha = process.env.ADMIN_SENHA || 'admin123';
  db.prepare(`INSERT INTO usuarios (nome, email, senha_hash, papel) VALUES ('Administrador', ?, ?, 'admin')`)
    .run(email, bcrypt.hashSync(senha, 10));
  console.log(`Administrador criado: ${email}`);
}

if (process.env.SEED_DEMO === 'true' && !db.prepare('SELECT 1 FROM turmas').get()) {
  popularDemo(db);
  console.log('Dados de demonstração carregados');
}

const porta = Number(process.env.PORT || 8080);
criarApp(db).listen(porta, () => console.log(`Escola+ ouvindo em http://0.0.0.0:${porta}`));
