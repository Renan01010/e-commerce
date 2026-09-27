---

description: "Tarefas da feature User Service e Autenticação"
---

# Tasks: User Service e Autenticação

**Input**: Design documents em `specs/003-user-auth/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/user-service-api.openapi.yaml, quickstart.md

**Total**: 28 tarefas. Testes estão incluídos por exigência da Constituição TechStore (princípio IV) para regras de negócio, APIs, segurança e persistência.

**Organization**: tarefas por história de usuário; todas as tasks seguem `- [ ] T### [P?] [US#?] descrição com caminho exato`.

## Phase 1: Setup

**Purpose**: corrigir gate documental e preparar módulo, ambiente e integração com Gateway.

- [x] T001 Finalizar e revisar `specs/003-user-auth/spec.md`: completar o JSON interrompido de registro, consolidar as clarificações P0 duplicadas e registrar as decisões aceitas em login/JWT, gestão ADMIN, validação e datasource antes de qualquer implementação.
- [x] T002 [P] Registrar `user-service` em `backend/pom.xml` e criar `backend/user-service/pom.xml` com dependências gerenciadas pelo parent (Spring Web, JPA, Security Resource Server, Validation, Flyway, PostgreSQL, Actuator, OpenAPI, JUnit, Mockito e Testcontainers); criar `backend/user-service/src/main/java/com/techstore/user/UserServiceApplication.java`.
- [x] T003 [P] Adicionar runtime User Service em `backend/user-service/Dockerfile` e configuração local do serviço em `backend/user-service/src/main/resources/application.yml`, incluindo porta padrão 8082/`PORT`, datasource, Flyway, Hibernate validate e secret externo.
- [x] T004 Preparar PostgreSQL lógico separado `techstore_user_db`, variáveis de desenvolvimento e serviço User Service em `docker-compose.yml` e `.env.example`; usar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `TECHSTORE_JWT_SECRET`, `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD` sem valores secretos versionados.

## Phase 2: Foundational

**Purpose**: construir as capacidades comuns que bloqueiam as histórias.

- [x] T005 Criar domínio puro `backend/user-service/src/main/java/com/techstore/user/domain/User.java` e `backend/user-service/src/main/java/com/techstore/user/domain/Role.java`, com UUID, email normalizado, passwordHash, role única USER/ADMIN, active e timestamps UTC; impor 8–72 caracteres e máximo 72 bytes UTF-8 para senha e nunca expor hash.
- [x] T006 Criar `backend/user-service/src/main/java/com/techstore/user/application/port/in/RegisterUserUseCase.java`, `backend/user-service/src/main/java/com/techstore/user/application/port/in/LoginUseCase.java`, `backend/user-service/src/main/java/com/techstore/user/application/port/in/ManageUsersUseCase.java` e `backend/user-service/src/main/java/com/techstore/user/application/port/out/UserRepository.java`; definir operações de registro, autenticação, bootstrap, criação administrativa, atualização de role/active e consulta de último ADMIN.
- [x] T007 Criar migration `backend/user-service/src/main/resources/db/migration/V1__create_users.sql`, entidade JPA `backend/user-service/src/main/java/com/techstore/user/adapter/persistence/UserEntity.java`, `UserJpaRepository.java` e `UserPersistenceAdapter.java`; impor unique email normalizado, role válida e campos obrigatórios.
- [x] T008 Configurar BCrypt cost 12, beans de criptografia e decoder HMAC HS256, validação do segredo de pelo menos 32 bytes e SecurityFilterChain em `backend/user-service/src/main/java/com/techstore/user/adapter/config/SecurityConfig.java`; reservar a emissão de claims do token para `backend/user-service/src/main/java/com/techstore/user/adapter/security/JwtTokenService.java` e documentar `sub`, `roles`, `iat`, `exp` e validade de 24h.
- [x] T009 [P] Adicionar ao Gateway em `backend/api-gateway/src/main/resources/application.yml` o roteamento `/api/auth/**` e `/api/users/**` para `USER_SERVICE_URL`, removendo somente o prefixo `/api` com `StripPrefix=1`; manter autorização dentro do User Service e preservar rotas existentes do Product Service.
- [x] T010 Criar `backend/user-service/src/main/java/com/techstore/user/adapter/http/GlobalExceptionHandler.java`, `backend/user-service/src/main/java/com/techstore/user/adapter/http/ErrorResponse.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/config/OpenApiConfig.java`; garantir erros padronizados, correlation ID e ausência de senha, hash, JWT ou segredo nos logs/respostas.

**Checkpoint**: nenhuma história começa até T001–T010 estarem concluídas; T001 é gate obrigatório de documentação.

## Phase 3: User Story 1 - Registro e Login (Priority: P1)

**Goal**: permitir cadastro público com role USER e autenticação que emite JWT.

**Independent Test**: registrar um e-mail normalizado, confirmar resposta 201 sem hash e role USER, autenticar e receber JWT HS256 de 24h; senha errada, email inexistente e conta inativa retornam a mesma resposta genérica 401; duplicidade retorna 409.

### Tests for User Story 1

- [x] T011 [P] [US1] Escrever testes Mockito de `AuthService` em `backend/user-service/src/test/java/com/techstore/user/application/service/AuthServiceTest.java` para trim/lowercase, registro USER ativo, BCrypt sem persistir senha original, duplicidade, login válido e falha genérica para senha/email/conta inativa.
- [x] T012 [P] [US1] Escrever testes MockMvc de contrato e validação de register/login em `backend/user-service/src/test/java/com/techstore/user/adapter/http/AuthControllerTest.java`, cobrindo campos obrigatórios, e-mail inválido, limites de senha, status HTTP e ausência de passwordHash na resposta.

### Implementation for User Story 1

- [x] T013 [US1] Implementar casos de uso de registro público e autenticação em `backend/user-service/src/main/java/com/techstore/user/application/service/AuthService.java`; normalizar email, forçar role USER, persistir somente BCrypt e igualar mensagem/status para usuário ausente, senha incorreta ou conta inativa.
- [x] T014 [US1] Criar requests/responses em `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthModels.java` e controller em `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthController.java` para `POST /auth/register` e `POST /auth/login`; validar request conforme `specs/003-user-auth/contracts/user-service-api.openapi.yaml` e emitir `accessToken`, `tokenType=Bearer`, `expiresIn=86400` no login.
- [x] T015 [US1] Adicionar teste Testcontainers em `backend/user-service/src/test/java/com/techstore/user/AuthApiIntegrationTest.java` para migrations, persistência, registro/login, e-mail duplicado, BCrypt, conta inativa e resposta genérica nas rotas HTTP internas do User Service.

## Phase 4: User Story 2 - Administração de Usuários (Priority: P1)

**Goal**: bootstrap idempotente do primeiro ADMIN e criação/atualização de usuários apenas por ADMIN autenticado.

**Independent Test**: bootstrap cria ADMIN uma única vez sem sobrescrever usuário; USER ou anônimo recebem 403/401 nas rotas administrativas; ADMIN pode criar conta com role USER/ADMIN e alterar somente role/active; operação não deixa o sistema sem ADMIN ativo.

### Tests for User Story 2

- [x] T016 [P] [US2] Escrever testes unitários de bootstrap e gestão em `backend/user-service/src/test/java/com/techstore/user/application/service/UserManagementServiceTest.java` para variáveis ausentes/parciais, e-mail existente sem overwrite, criação com roles válidas e proteção do último ADMIN ativo.
- [x] T017 [P] [US2] Escrever testes MockMvc de autorização e contrato em `backend/user-service/src/test/java/com/techstore/user/adapter/http/UserControllerTest.java`: POST/PUT exigem ADMIN, usuário USER recebe 403, anônimo recebe 401, role inválida e campos não permitidos são rejeitados.

### Implementation for User Story 2

- [x] T018 [US2] Implementar bootstrap em `backend/user-service/src/main/java/com/techstore/user/adapter/config/InitialAdminBootstrap.java`: ambas `INITIAL_ADMIN_*` ausentes ignoram bootstrap, somente uma definida falha sem revelar valor, e-mail existente não altera dados; criar ADMIN com BCrypt quando ausente.
- [x] T019 [US2] Implementar casos de uso em `backend/user-service/src/main/java/com/techstore/user/application/service/UserManagementService.java` para POST administrativo (email, password, role) e PUT (somente role/active); impedir remoção/desativação da própria role ADMIN e do último ADMIN ativo, retornando conflito apropriado.
- [x] T020 [US2] Criar `backend/user-service/src/main/java/com/techstore/user/adapter/http/UserModels.java` e controller `backend/user-service/src/main/java/com/techstore/user/adapter/http/UserController.java` para `POST /users` e `PUT /users/{id}`; proteger os métodos com role ADMIN e emitir somente UserResponse público sem passwordHash.
- [x] T021 [US2] Criar testes PostgreSQL Testcontainers em `backend/user-service/src/test/java/com/techstore/user/UserManagementIntegrationTest.java` para bootstrap idempotente, unicidade de email, permissões 401/403, criação/atualização por ADMIN e integridade do último ADMIN ativo.

## Phase 5: User Story 3 - Validação de JWT (Priority: P1)

**Goal**: validar tokens emitidos pelo User Service em rotas protegidas e manter compatibilidade com Product Service.

**Independent Test**: JWT válido com `roles=[ADMIN]` acessa operação administrativa; token USER recebe 403; token expirado, adulterado, ausente ou assinado com outra chave recebe 401; Product Service interpreta a role como `ROLE_ADMIN`.

### Tests for User Story 3

- [x] T022 [P] [US3] Escrever testes de JWT em `backend/user-service/src/test/java/com/techstore/user/adapter/security/JwtTokenServiceTest.java` para assinatura HS256, `sub` UUID, `roles` array, `iat`, expiração em 86400 segundos, token expirado e assinatura adulterada.
- [x] T023 [P] [US3] Criar teste de compatibilidade em `backend/product-service/src/test/java/com/techstore/product/adapter/config/UserServiceJwtCompatibilityTest.java` verificando que token conforme contrato (HS256, claim `roles`) é decodificado pelo validador Product Service e gera authority `ROLE_ADMIN`.

### Implementation for User Story 3

- [x] T024 [US3] Implementar `JwtTokenService` em `backend/user-service/src/main/java/com/techstore/user/adapter/security/JwtTokenService.java` com UUID em `sub`, `roles` array, `iat`/`exp`, HS256 e validade fixa de 24h; nunca registrar token/secret.
- [x] T025 [US3] Aplicar regras de resource server e testes de autorização em `backend/user-service/src/main/java/com/techstore/user/adapter/config/SecurityConfig.java` e cobrir os endpoints protegidos com `backend/user-service/src/test/java/com/techstore/user/adapter/security/JwtAuthorizationTest.java`; rotas públicas de auth permanecem abertas e `/users/**` exige ADMIN.

## Phase 6: Polish & Cross-Cutting

**Purpose**: fechar documentação operacional, gateway e gates de qualidade.

- [x] T026 [P] [US3] Criar teste de roteamento do Gateway em `backend/api-gateway/src/test/java/com/techstore/gateway/UserServiceRouteTest.java` validando `/api/auth/**` e `/api/users/**`, preservação do método/header Authorization e remoção de apenas `/api`.
- [x] T027 Atualizar `README.md`, `DEPLOYMENT.md` e `specs/003-user-auth/quickstart.md` com User Service, database lógico isolado, variáveis Railway/Compose, porta 8082 e URLs públicas do Gateway sem incluir secrets.
- [ ] T028 Executar `mvn -f backend/pom.xml verify` e validar cenários de `specs/003-user-auth/quickstart.md`; registrar resultado dos testes Testcontainers e corrigir falhas antes de considerar a feature pronta.

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: T001 é gate documental; T002–T004 dependem de T001.
- **Foundational (Phase 2)**: T005–T010 dependem do módulo inicial; bloqueiam as histórias. T009 pode ser desenvolvido em paralelo aos adapters do User Service depois de T002.
- **US1 (Phase 3)**: depende de T001–T010; MVP funcional de registro/login.
- **US2 (Phase 4)**: depende de domínio, persistência e security fundacionais; recomendado após US1 para reutilizar autenticação/DTOs.
- **US3 (Phase 5)**: depende do emissor JWT de US1 e das rotas protegidas de US2; valida interoperabilidade com Product Service.
- **Polish (Phase 6)**: após as três histórias.

### User story order

`US1 (P1) → US2 (P1) → US3 (P1)` é a ordem recomendada de entrega. As histórias compartilham domínio e JWT; não são completamente independentes apesar de todas terem prioridade P1.

### Parallel opportunities

- T002, T003 e T004 podem ser trabalhadas em paralelo após T001 se os limites de arquivo forem respeitados.
- T011 e T012 são testes separados e podem ser escritos em paralelo.
- T016 e T017 são testes separados e podem ser escritos em paralelo.
- T022 e T023 são testes em módulos diferentes e podem ser escritos em paralelo após o contrato ser congelado.
- T026 pode ser preparado em paralelo a polish documental, após T009.

## Implementation Strategy

### MVP

1. Concluir T001 e todas as tarefas Setup/Fundational.
2. Entregar US1 (registro público, login e JWT) e validar isoladamente.
3. Entregar US2 para bootstrap/admin e provar que nenhum cliente pode atribuir ADMIN a si mesmo.
4. Entregar US3 e provar validação do token pelo Product Service.
5. Rodar integração com PostgreSQL/Testcontainers e quickstart antes do deploy.

### Incremental checkpoints

- Após Phase 2: migrations sobem e o módulo inicia com datasource e secret configurados.
- Após US1: fluxo register/login funciona sem revelar senha, hash ou existência de conta no erro de login.
- Após US2: primeiro ADMIN e operações administrativas ficam utilizáveis com regras de último-admin.
- Após US3: token emitido funciona nos dois serviços e rotas administrativas recusam token inválido/USER.

## Notes

- `specs/003-user-auth/spec.md` está incompleto no workspace e contém clarificações duplicadas. T001 é bloqueador de implementação; esta lista usa as respostas de clarificação já aceitas e não altera a spec automaticamente.
- `[P]` significa que arquivos e pré-requisitos permitem execução paralela; `[US#]` liga a task à história correspondente.
- As tasks de teste antecedem implementação dentro de cada história quando possível; executar os testes e confirmar falha esperada antes de implementar.
