# Escola+ — app escolar tudo em um

Plataforma escolar composta por:

| Parte | Tecnologia | Pasta |
|---|---|---|
| **App Android** para alunos, responsáveis e professores | Kotlin + Jetpack Compose (Material 3), Retrofit | `android/` |
| **API + Backoffice web** da secretaria, rodando na nuvem | Node.js 22 + Express + SQLite | `backend/` |
| **Implantação na nuvem** | Docker, Kubernetes (OKE/GKE/EKS/AKS) ou Docker Compose | `deploy/` |

## Funcionalidades

Descrição completa com capturas de tela: [docs/FUNCIONALIDADES.md](docs/FUNCIONALIDADES.md).

**Aluno e responsável** (o responsável alterna entre os filhos vinculados)
- Painel com média geral, frequência, tarefas pendentes, mensalidades em aberto, próximo evento e último comunicado
- Comunicados (gerais ou da turma, com destaque para "importante")
- Agenda de eventos e provas
- Horário de aulas da semana
- Boletim por bimestre com média
- Frequência por disciplina (alerta abaixo de 75%)
- Tarefas de casa (a entregar / anteriores)
- Cardápio da semana
- Financeiro: mensalidades com situação e botão para copiar o código de pagamento

**Professor**
- Lista das suas disciplinas/turmas
- Chamada pelo celular (navega entre os dias e corrige chamadas já feitas)
- Publicação de tarefas com data de entrega
- Publicação de comunicados para as suas turmas

**Backoffice (secretaria / admin)** — `http://<servidor>/`
- Painel: alunos, professores, turmas, frequência geral, inadimplência e próximos eventos
- Cadastro completo de turmas, usuários, vínculos responsável↔aluno, disciplinas, horários,
  comunicados, eventos, tarefas, notas, frequência, cardápio e mensalidades (com busca)

Permissões são verificadas no servidor: o aluno vê só os próprios dados, o responsável só os dos filhos
vinculados, o professor só as turmas em que leciona, e apenas administradores acessam `/api/admin`.

## Rodando localmente

```bash
cd escola/backend
npm install
SEED_DEMO=true npm start            # http://localhost:8080
npm test                            # testes da API
```

Usuários de demonstração (com `SEED_DEMO=true`):

| Perfil | E-mail | Senha |
|---|---|---|
| Admin (backoffice) | admin@escola.local | admin123 |
| Professora | ana.prof@escola.local | 123456 |
| Responsável (2 filhos) | paula@familia.local | 123456 |
| Aluno | joao@escola.local | 123456 |

### App Android

Abra `escola/android` no Android Studio (ou `./gradlew assembleDebug`). No emulador o app já aponta para
`http://10.0.2.2:8080/` (o backend rodando na sua máquina). Para outro servidor, toque em
**Configurar servidor** na tela de login ou gere o APK com a URL embutida:

```bash
./gradlew assembleRelease -PapiUrl=https://escola.suaescola.com.br/
```

Por segurança o app só aceita **HTTPS**; HTTP é liberado apenas para `10.0.2.2`, `localhost` e `127.0.0.1`
(ver `app/src/main/res/xml/network_security_config.xml`).

O workflow `.github/workflows/escola.yml` roda os testes da API e gera o APK a cada push; o APK fica
disponível como artefato da execução. Pela aba *Actions → Escola+ → Run workflow* é possível informar a URL
do servidor que será embutida no APK.

## Publicando o backoffice na nuvem

### Opção 1 — Kubernetes (OKE, GKE, EKS, AKS)

```bash
docker build -t <registro>/escola-backoffice:1.0 escola/backend
docker push <registro>/escola-backoffice:1.0
# edite a imagem e os segredos (JWT_SECRET, ADMIN_EMAIL, ADMIN_SENHA) em escola/deploy/escola.kubernetes.yml
kubectl apply -f escola/deploy/escola.kubernetes.yml
kubectl get svc escola-backoffice      # IP externo do LoadBalancer
```

Coloque um Ingress/Load Balancer com certificado TLS na frente para servir em HTTPS.

### Opção 2 — VM simples (qualquer nuvem)

```bash
docker compose -f escola/deploy/docker-compose.yml up -d
```

Use um proxy reverso com HTTPS (Caddy, Nginx + Let's Encrypt) apontando para a porta 8080.

### Variáveis de ambiente

| Variável | Descrição | Padrão |
|---|---|---|
| `PORT` | Porta HTTP | `8080` |
| `DB_PATH` | Arquivo do banco SQLite | `backend/data/escola.db` (`/data/escola.db` na imagem) |
| `JWT_SECRET` | Segredo de assinatura dos tokens (**obrigatório em produção**) | — |
| `ADMIN_EMAIL` / `ADMIN_SENHA` | Primeiro administrador, criado se não houver nenhum | `admin@escola.local` / `admin123` |
| `SEED_DEMO` | `true` carrega dados de demonstração num banco vazio | `false` |

O banco SQLite fica em volume persistente e suporta uma réplica. Para várias réplicas ou alta
disponibilidade, o próximo passo é migrar para PostgreSQL gerenciado (as consultas usam SQL padrão).

## API (resumo)

| Método | Rota | Quem |
|---|---|---|
| POST | `/api/auth/login` | todos |
| GET | `/api/me` | todos |
| GET | `/api/alunos/:id/{resumo,avisos,eventos,horario,notas,frequencia,tarefas,financeiro}` | aluno, responsável, professor da turma, admin |
| GET | `/api/cardapio` | todos |
| GET | `/api/avisos`, `/api/eventos` | professor, admin |
| GET | `/api/professor/disciplinas` | professor, admin |
| GET/POST | `/api/professor/disciplinas/:id/chamada` | professor da disciplina, admin |
| POST | `/api/professor/disciplinas/:id/tarefas` | professor da disciplina, admin |
| POST | `/api/professor/avisos` | professor, admin |
| GET/POST/PUT/DELETE | `/api/admin/:recurso[/:id]` | admin |

Autenticação por `Authorization: Bearer <token>` (JWT válido por 30 dias).

## Próximos passos sugeridos

- Notificações push (Firebase Cloud Messaging) para comunicados e notas novas
- Pagamento via PIX integrado a um gateway
- Envio de anexos (fotos, PDFs) nos comunicados e tarefas
- Chat entre responsáveis e professores
- Migração para PostgreSQL para escalar horizontalmente
