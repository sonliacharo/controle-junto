# ControleJunto

Aplicação web de **controle financeiro pessoal e compartilhado**. O usuário registra seus gastos do dia a dia e organiza despesas feitas em grupo (viagens, república, eventos). O sistema calcula automaticamente quem deve quanto a quem e usa **Inteligência Artificial** para sugerir a categoria de cada gasto.

Trabalho semestral da disciplina **Tópicos Avançados em Programação**, Engenharia de Software, UCSal (2026.2). Professor: Elton Figueiredo da Silva.

| Integrante | Matrícula |
|---|---|
| Alison Andrade Rocha | 200033160 |
| Denilson Melo de Souza | 200028969 |
| João Bruno Reis Carvalho dos Santos | 200026858 |
| Lívia Alves de Souza | 200032754 |
| Lucas Danilo dos Santos Araújo | 200027019 |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Back-end | Java 21, Spring Boot 3.5 (Web, Data JPA, Security, Validation), JWT (jjwt) |
| Front-end | React 18, Vite, React Router, Recharts |
| Banco de dados | PostgreSQL no **Supabase** (nuvem) ou H2 em memória (automático, sem configurar nada) |
| IA | API do Google Gemini, com classificação local por palavras-chave como reserva |
| Testes | JUnit 5 |

## Arquitetura

```
Navegador ──► Front-end React (porta 5173) ──/api──► API Spring Boot (porta 8080) ──► Supabase / H2
                                                          │
                                                          └──► API de IA (Gemini)
```

O modelo C4 completo (contexto, contêineres, componentes e código) e as capturas das telas estão no documento do projeto, feito no Overleaf.

Organização do back-end (`backend/src/main/java/br/ucsal/controlejunto`):

| Pacote | O que tem |
|---|---|
| `domain` | Entidades JPA: Usuario, Gasto, Categoria, Orcamento, Grupo, Participacao, DespesaGrupo, Divisao, Divida, Notificacao |
| `repository` | Interfaces Spring Data JPA (acesso ao banco) |
| `service` | Regras de negócio. `CalculadoraDivisao` e `CalculadoraSaldos` têm as regras puras, testadas sem banco |
| `controller` | Endpoints REST |
| `security` | Login com JWT (sessão de 30 minutos) |
| `dto` | Dados de entrada e saída da API |
| `exception` | Erros de regra de negócio viram respostas JSON `{"erro": "..."}` |
| `config` | Segurança, CORS e dados de exemplo |

---

## Como rodar

### Opção 1: GitHub Codespaces (recomendado)

1. No GitHub, clique em **Code > Codespaces > Create codespace on main**. O ambiente já vem com Java 21, Maven e Node.
2. Em um terminal: `bash iniciar-backend.sh`
3. Em outro terminal: `bash iniciar-frontend.sh`
4. Abra a porta **5173** (aba *Portas* do VS Code).

### Opção 2: no seu computador

Precisa de **Java 21** e **Node 18+**.

```bash
# Terminal 1 - API
bash iniciar-backend.sh       # ou: cd backend && sh mvnw spring-boot:run

# Terminal 2 - Front-end
bash iniciar-frontend.sh      # ou: cd frontend && npm install && npm run dev
```

Abra http://localhost:5173

### Usuários de exemplo

Na primeira execução o sistema cria dados de demonstração. **Senha de todos: `123456`**

| Usuário | E-mail |
|---|---|
| Lívia Alves (tem gastos, orçamento e 3 grupos) | `livia@controlejunto.com` |
| Alison Rocha | `alison@controlejunto.com` |
| Denilson Souza | `denilson@controlejunto.com` |
| João Bruno | `joao@controlejunto.com` |
| Lucas Danilo | `lucas@controlejunto.com` |

---

## Configuração (opcional)

Sem configurar nada, o sistema usa **H2 em memória**, e os dados somem ao reiniciar. Também usa a **classificação local** no lugar da IA. Para usar o Supabase e a IA de verdade:

1. Copie `.env.exemplo` para `.env` (o `.env` não vai para o GitHub).
2. Preencha as variáveis:

| Variável | Para que serve | Onde conseguir |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Banco Supabase compartilhado pelo time | Supabase > Project Settings > Database > Connection string (use a opção **JDBC**, porta 5432, com `?sslmode=require`) |
| `GEMINI_API_KEY` | Sugestão de categoria pela IA | Chave gratuita em https://aistudio.google.com/apikey |
| `JWT_SECRET` | Assinatura do token de login | Qualquer texto longo (32+ caracteres) |

3. Rode `bash iniciar-backend.sh`. O script carrega o `.env` sozinho.

As tabelas são criadas automaticamente no Supabase na primeira execução (`ddl-auto=update`). Para não criar os usuários de exemplo no banco compartilhado, defina `DADOS_EXEMPLO=false`.

---

## Funcionalidades implementadas

| Requisito | Onde |
|---|---|
| RF-001 Cadastro / RF-002 Login | `AuthController`, `AuthService`, tela de login |
| RF-003 Registrar gasto / RF-004 Editar e excluir | `GastoController`, tela *Meus gastos* |
| RF-005 Criar grupo / RF-006 Participantes | `GrupoController`, tela *Grupos* |
| RF-007 Despesa em grupo (divisão igual ou personalizada) | `GrupoService.registrarDespesa` |
| RF-008 Cálculo automático de saldos | `CalculadoraSaldos` |
| RF-009 Marcar dívida como paga | `GrupoService.quitarDivida` |
| RF-010 Categoria sugerida por IA | `ServicoCategorizacaoIA` |
| RF-011 Dashboard | `OrcamentoService.dashboard`, tela *Painel* |
| RF-012 Notificações | `NotificacaoService`, sino no topo da tela |
| RF-013 Orçamento mensal | `OrcamentoService` |
| RF-014 Filtro de gastos | `GET /api/gastos?categoriaId=` |
| RF-016 Comentário em gasto | campo `comentario` |

Regras de negócio validadas no back-end: RN-001 (divisão igual), RN-002 (só o autor edita), RN-003 (encerrar só sem pendências), RN-004 (alerta de 90%), RN-005 (sugestão da IA pode ser trocada), RN-006 (máx. 20 participantes), RN-007 (excluir grupo vazio), RN-008 (soma da divisão = total), RN-009 (valor > 0).

Fica para uma próxima versão: RF-015 (exportar PDF).

## Endpoints da API

Todas as rotas, exceto cadastro e login, exigem o cabeçalho `Authorization: Bearer <token>`.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/cadastro` | Cria conta e devolve o token |
| POST | `/api/auth/login` | Login e devolve o token |
| GET | `/api/auth/me` | Dados do usuário logado |
| GET | `/api/categorias` | Lista as categorias |
| POST | `/api/categorias/sugestao` | Sugere a categoria (IA) `{ "descricao": "Uber aeroporto" }` |
| GET | `/api/gastos?mes=&ano=&categoriaId=` | Gastos do mês |
| POST / PUT / DELETE | `/api/gastos[/{id}]` | Cria, edita e exclui gasto |
| GET / PUT | `/api/orcamento` | Consulta e define o orçamento do mês |
| GET | `/api/dashboard?mes=&ano=` | Dados do painel |
| GET / POST | `/api/grupos` | Lista e cria grupos |
| GET / DELETE | `/api/grupos/{id}` | Detalhe e exclusão de grupo |
| POST / DELETE | `/api/grupos/{id}/participantes[/{usuarioId}]` | Adiciona e remove participante |
| POST | `/api/grupos/{id}/despesas` | Registra despesa no grupo |
| POST | `/api/grupos/{id}/encerrar` | Encerra o grupo |
| POST | `/api/dividas/{id}/quitar` | Marca a dívida como paga |
| GET | `/api/notificacoes` | Lista as notificações |
| POST | `/api/notificacoes/{id}/lida` | Marca a notificação como lida |

## Testes

```bash
cd backend && sh mvnw test
```

Os testes cobrem a divisão igual e personalizada, o cálculo de saldos (a soma de todos os saldos é sempre zero) e a classificação de categorias.
