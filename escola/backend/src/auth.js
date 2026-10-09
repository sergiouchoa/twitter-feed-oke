const jwt = require('jsonwebtoken');

const SEGREDO = process.env.JWT_SECRET || 'troque-este-segredo-em-producao';
if (!process.env.JWT_SECRET && process.env.NODE_ENV === 'production') {
  throw new Error('Defina JWT_SECRET em produção');
}

function gerarToken(usuario) {
  return jwt.sign({ sub: usuario.id, papel: usuario.papel }, SEGREDO, { expiresIn: '30d' });
}

function autenticar(db) {
  const buscar = db.prepare('SELECT id, nome, email, papel, turma_id, ativo FROM usuarios WHERE id = ?');
  return (req, res, next) => {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : null;
    if (!token) return res.status(401).json({ erro: 'Não autenticado' });
    try {
      const payload = jwt.verify(token, SEGREDO);
      const usuario = buscar.get(payload.sub);
      if (!usuario || !usuario.ativo) return res.status(401).json({ erro: 'Usuário inválido' });
      req.usuario = usuario;
      next();
    } catch {
      res.status(401).json({ erro: 'Token inválido' });
    }
  };
}

function exigirPapel(...papeis) {
  return (req, res, next) =>
    papeis.includes(req.usuario.papel) ? next() : res.status(403).json({ erro: 'Sem permissão' });
}

module.exports = { gerarToken, autenticar, exigirPapel };
