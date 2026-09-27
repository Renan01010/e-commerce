# Pesquisa: User Service e Autenticação

**Feature**: `003-user-auth`  
**Data**: 2026-09-27

## Decisões e justificativas

### Stack e arquitetura

**Decisão**: adicionar `backend/user-service` ao Maven multi-module existente, usando Java 21 e Spring Boot 3.3.x, com domínio independente e portas de aplicação. Adaptadores Spring MVC, Spring Security, JPA e PostgreSQL ficam em `adapter/`; migrations Flyway em `src/main/resources/db/migration/`.

**Justificativa**: preserva os princípios Hexagonal Architecture e database-per-service da Constituição e reutiliza as versões, convenções Maven e dependências já usadas pelo Product Service.

**Alternativa considerada**: adicionar autenticação diretamente ao Product Service. Rejeitada porque misturaria bounded contexts e faria o Product Service dono de dados de usuário.

### Persistência e bootstrap

**Decisão**: PostgreSQL com database lógico exclusivo `techstore_user_db`, schema gerenciado pelo Flyway e datasource configurada com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. O User Service é o único escritor/leitor da tabela de usuários. O banco pode compartilhar a instância física PostgreSQL com o catálogo, desde que use database lógico separado.

O bootstrap recebe `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD`. Se ambas estiverem ausentes, não cria conta inicial; se apenas uma estiver definida, a inicialização falha com mensagem sem revelar o valor. Se o e-mail normalizado já existir, não modifica a conta. A unicidade no banco é a proteção final em inicializações concorrentes.

**Alternativa considerada**: banco compartilhado com o Product Service. Rejeitada por violar isolamento e propriedade dos dados.

### Credenciais

**Decisão**: normalizar e-mail com `trim` e lowercase; validar formato e limite de 254 caracteres; armazenar somente hash BCrypt com cost 12. Senhas aceitas têm 8 a 72 caracteres e também devem caber no limite de 72 bytes UTF-8 do BCrypt, evitando truncamento silencioso. A senha original nunca será persistida, retornada ou registrada.

**Alternativa considerada**: aceitar qualquer string e depender apenas do encoder. Rejeitada porque implementações BCrypt têm limite de bytes e podem truncar entradas longas.

### Roles e autorização

**Decisão**: uma role por usuário (`USER` ou `ADMIN`). Cadastro público sempre cria `USER`; somente ADMIN autenticado pode criar ou promover contas via API administrativa. Administradores não podem remover a própria role ADMIN nem desativar a última conta ADMIN ativa. Usuário inativo não autentica. Operações administrativas devolvem 401 sem autenticação válida e 403 para usuário autenticado sem ADMIN.

### JWT

**Decisão**: o User Service emite Bearer JWT HS256 com `sub` contendo UUID do usuário, `roles` como array (por exemplo `["ADMIN"]`), `iat` e `exp`; validade de 24 horas, sem refresh token. A resposta de login contém `accessToken`, `tokenType: "Bearer"` e `expiresIn` em segundos. `TECHSTORE_JWT_SECRET` vem do ambiente e precisa ter pelo menos 32 bytes para coincidir com o validador existente do Product Service.

**Alternativas consideradas**: token opaco ou algoritmo assimétrico. Não adotadas neste incremento: a especificação escolheu JWT HS256 e o Product Service atual valida com a mesma chave simétrica e converte a claim `roles` em authorities `ROLE_*`.

`iss` e `aud` não são necessários para o contrato inicial: não há valores acordados nem validadores configurados no Product Service atual. Podem ser adicionados posteriormente como evolução coordenada de emissores e validadores.

### APIs e gateway

**Decisão**: o contrato público é exposto pelo Gateway em `/api/auth/register`, `/api/auth/login`, `/api/users` e `/api/users/{id}`. O Gateway encaminha `/api/auth/**` e `/api/users/**` ao endereço interno configurado por `USER_SERVICE_URL` e remove o primeiro segmento `/api`; o User Service recebe `/auth/**` e `/users/**`. O Gateway não decide autorização; o User Service protege suas rotas administrativas, assim como o Product Service protege suas próprias operações.

Cadastro e login são públicos. `POST /api/users` e `PUT /api/users/{id}` exigem ADMIN. POST aceita e-mail, senha inicial e role. PUT aceita apenas role e active; não altera e-mail nem senha. A senha só é definida no cadastro inicial neste incremento, pois recuperação e gerenciamento de perfil/senha estão fora de escopo.

### Erros e proteção de dados

**Decisão**: erros de login para e-mail inexistente, senha incorreta e conta inativa são indistinguíveis (HTTP 401, mensagem genérica). E-mail duplicado retorna 409; validação retorna 400; autorização insuficiente retorna 403; usuário-alvo inexistente retorna 404. Respostas não incluem hash, senha, segredo nem claims sensíveis. Logs incluem operação/correlation ID sem credenciais, bearer tokens ou conteúdo de senha.

### Testes

**Decisão**: JUnit 5 e Mockito para domínio/casos de uso; testes MVC para contratos, validação e segurança; Testcontainers PostgreSQL para migrations, constraints e persistência; teste de roteamento Gateway e contrato OpenAPI.

## Restrições e pendências de entrada

- A cópia atual de `spec.md` termina no meio do exemplo JSON de `POST /api/auth/register` e repete duas vezes os blocos P0. O plano consolida as decisões P0/P1 fornecidas pelo usuário, mas o texto da especificação deve ser finalizado/revisado antes da implementação.
- O contrato não cria logout, refresh token, recuperação de senha, verificação de e-mail ou gerenciamento de perfil.
- Valores de validação complementares definidos nesta pesquisa (e-mail até 254, BCrypt cost 12 e limite de bytes) devem permanecer registrados na documentação da feature e ser revisados no gate de análise antes de codificar.
