# Tasks: Conta do Usuário e Autenticação

**Input**: Design documents from `/specs/015-user-account-authentication/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/account-authentication-api.openapi.yaml

**Tests**: Automatizados são obrigatórios pela spec. Use JUnit/Mockito/MockMvc/Testcontainers no backend e Vitest/Testing Library no frontend; e-mail sempre com adapter fake nos testes.

**Organization**: Fases por história de usuário para permitir validação incremental. Todas as tarefas devem ser executadas sobre as APIs, stores e serviços já existentes.

## Phase 1: Setup

**Purpose**: Preparar configuração compartilhada e integração sem adicionar infraestrutura nova.

- [ ] T001 [P] Documentar nomes e placeholders não secretos para SMTP, `APP_PUBLIC_URL`, URLs internas de introspecção e timeouts em `.env.example`
- [ ] T002 Adicionar dependência Spring Boot Mail e propriedades externas de remetente/URL em `backend/user-service/pom.xml` e `backend/user-service/src/main/resources/application.yml`
- [ ] T003 Configurar variáveis locais `APP_PUBLIC_URL`, `MAIL_*`, `USER_SERVICE_URL` e timeout de introspecção nos serviços correspondentes em `docker-compose.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema e infraestrutura compartilhados que bloqueiam as histórias de conta e revogação.

**⚠️ CRITICAL**: Completar a migration e a validação central de sessões antes de habilitar fluxos novos no frontend.

- [ ] T004 [P] Escrever teste de migration que prova V1 preservada, contas legadas ficam sem nome/não verificadas e `auth_version=0` em `backend/user-service/src/test/java/com/techstore/user/AccountSchemaMigrationIntegrationTest.java`
- [ ] T005 Criar migration Flyway aditiva para `users.name` nullable, `email_verified` false por padrão, `auth_version` iniciado em 0 e tabelas de tokens com hash único, FK, `expires_at`, `used_at` e timestamps em `backend/user-service/src/main/resources/db/migration/V2__extend_user_account_authentication.sql`; preservar V1
- [ ] T006 Atualizar domínio, entidade, mapeamento JPA e persistência para `name` opcional em legado, `email_verified` e `auth_version` em `backend/user-service/src/main/java/com/techstore/user/domain/User.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/persistence/`
- [ ] T007 [P] Estabelecer ports de token, envio de e-mail, introspecção e testes fake em `backend/user-service/src/main/java/com/techstore/user/application/port/out/` e `backend/user-service/src/test/java/com/techstore/user/`
- [ ] T008 Incluir `auth_version` nos JWTs novos sem alterar HS256, `sub`, `roles` nem validade atual de 24 horas em `backend/user-service/src/main/java/com/techstore/user/adapter/security/JwtTokenService.java`
- [ ] T009 [P] Escrever teste de introspecção para token ativo, expirado, revogado, sem versão, usuário inexistente/inativo e resposta mínima sem dados pessoais em `backend/user-service/src/test/java/com/techstore/user/adapter/http/SessionIntrospectionControllerTest.java`
- [ ] T010 Implementar validação central autenticada no User Service: assinatura, expiração, `sub`, conta ativa e versão atual; rejeitar tokens sem `auth_version` em `backend/user-service/src/main/java/com/techstore/user/adapter/http/` e `backend/user-service/src/main/java/com/techstore/user/adapter/config/SecurityConfig.java`
- [ ] T011 Adaptar o validador JWT do Cart Service para consultar a introspecção privada a cada requisição, propagar correlation ID, usar timeout configurável e negar com 503 explícito quando a autoridade estiver indisponível em `backend/cart-service/src/main/java/com/techstore/cart/adapter/security/SecurityConfig.java` e `backend/cart-service/src/main/resources/application.yml`
- [ ] T012 Adaptar a validação JWT do Product Service para consultar a introspecção privada a cada requisição, preservar roles e negar com 503 explícito sem fallback/cache positivo em `backend/product-service/src/main/java/com/techstore/product/adapter/config/SecurityConfig.java` e `backend/product-service/src/main/resources/application.yml`
- [ ] T013 [P] Testar nos serviços Cart e Product os estados ativo, expirado, revogado e timeout/503, sem regressão de autorização por role, em `backend/cart-service/src/test/java/com/techstore/cart/adapter/security/` e `backend/product-service/src/test/java/com/techstore/product/adapter/config/`
- [ ] T014 Documentar contrato de introspecção como endpoint privado não roteado pelo Gateway em `specs/015-user-account-authentication/contracts/account-authentication-api.openapi.yaml` e garantir que `/internal/**` não seja encaminhado em `backend/api-gateway/src/main/resources/application.yml`

**Checkpoint**: migration compatível, emissão/versionamento JWT e verificação remota fail-closed prontos; nenhum fluxo frontend novo deve ser habilitado antes desse ponto.

---

## Phase 3: User Story 1 - Criar uma conta (Priority: P1)

**Goal**: Visitante cria conta USER com nome e recebe início do fluxo de confirmação sem promessa falsa de entrega.

**Independent Test**: Cadastrar nome/e-mail/senha válidos, verificar conta persistida não verificada e hash BCrypt, receber instruções pelo fake sender; testar duplicidade, validações, payload privilegiado e falha do SMTP sem apagar a conta.

### Tests for User Story 1

- [ ] T015 [P] [US1] Cobrir cadastro válido, normalização, nome e resposta sem hash/senha em `backend/user-service/src/test/java/com/techstore/user/AuthApiIntegrationTest.java`
- [ ] T016 [P] [US1] Cobrir e-mail duplicado, entradas inválidas, tentativa de definir role/estado e falha de e-mail preservando conta em `backend/user-service/src/test/java/com/techstore/user/adapter/http/AuthControllerTest.java`
- [ ] T017 [P] [US1] Cobrir formulário de cadastro, confirmação de senha, validação, loading, duplicidade e falha de serviço em `frontend/src/pages/__tests__/RegisterPage.test.tsx`

### Implementation for User Story 1

- [ ] T018 [US1] Ampliar cadastro público para aceitar e validar nome de 1–100 caracteres, e-mail normalizado e senha conforme política atual; ignorar/rejeitar campos privilegiados sem quebrar clientes antigos em `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthModels.java` e `backend/user-service/src/main/java/com/techstore/user/application/service/AuthService.java`
- [ ] T019 [US1] Criar conta com role USER e `email_verified=false`, persistir hash BCrypt e resposta compatível sem campos sensíveis em `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthController.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthModels.java`
- [ ] T020 [US1] Criar tokens de verificação de alta entropia com hash persistido, validade de 24 horas e envio via porta SMTP depois de persistir conta/tokens em `backend/user-service/src/main/java/com/techstore/user/application/service/` e `backend/user-service/src/main/java/com/techstore/user/adapter/mail/`
- [ ] T021 [US1] Preservar conta não verificada quando o envio falhar, registrar falha sanitizada e retornar estado que não afirme entrega em `backend/user-service/src/main/java/com/techstore/user/application/service/` e `backend/user-service/src/main/java/com/techstore/user/adapter/http/`
- [ ] T022 [US1] Atualizar schema público de registro e resposta de conta no contrato OpenAPI sem remover campos do contrato de registro/login existente em `specs/015-user-account-authentication/contracts/account-authentication-api.openapi.yaml`
- [ ] T023 [P] [US1] Implementar serviço frontend de cadastro com validação da resposta e mapeamento seguro de erros em `frontend/src/services/authService.ts` e `frontend/src/types/auth.ts`
- [ ] T024 [US1] Implementar formulário e página de criação de conta com nome, e-mail, senha/confirmar senha, estados acessíveis e informação de verificação pendente em `frontend/src/pages/RegisterPage.tsx` e `frontend/src/pages/RegisterPage.css`
- [ ] T025 [US1] Ligar `/register` à nova página e preservar rotas públicas existentes em `frontend/src/App.tsx`

**Checkpoint**: cadastro cria conta sem privilégio, gera início de confirmação e continua recuperável após falha de entrega.

---

## Phase 4: User Story 2 - Confirmar e-mail (Priority: P1)

**Goal**: Usuário confirma e-mail com token temporário de uso único e pode solicitar reenvio sem revelar existência da conta.

**Independent Test**: Confirmar conta criada por US1 com token válido; provar expiração em 24 horas, rejeição de uso/replay e resposta uniforme do reenvio.

### Tests for User Story 2

- [ ] T026 [P] [US2] Cobrir token válido, expirado, usado, superseded, concorrência de consumo e atualização do estado da conta em `backend/user-service/src/test/java/com/techstore/user/EmailVerificationIntegrationTest.java`
- [ ] T027 [P] [US2] Cobrir reenvio genérico para e-mail existente/inexistente, falha do sender e inexistência de token original no banco em `backend/user-service/src/test/java/com/techstore/user/EmailVerificationIntegrationTest.java`
- [ ] T028 [P] [US2] Cobrir apresentação de link, estados sucesso/erro/expirado e reenvio em `frontend/src/pages/__tests__/VerifyEmailPage.test.tsx` e `frontend/src/pages/__tests__/ResendVerificationPage.test.tsx`

### Implementation for User Story 2

- [ ] T029 [US2] Implementar casos de uso para verificar token e emitir novo token invalidando tokens anteriores em `backend/user-service/src/main/java/com/techstore/user/application/service/EmailVerificationService.java`
- [ ] T030 [US2] Criar adapter JPA/repositório com lookup por hash e consumo único transacional em `backend/user-service/src/main/java/com/techstore/user/adapter/persistence/EmailVerificationTokenEntity.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/persistence/EmailVerificationTokenJpaRepository.java`
- [ ] T031 [US2] Expor confirmação e reenvio com validação de request, resposta genérica e tratamento de erros em `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthController.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthModels.java`
- [ ] T032 [US2] Adicionar páginas de confirmação e reenvio com token da query string, feedback seguro e links de retorno/login em `frontend/src/pages/VerifyEmailPage.tsx` e `frontend/src/pages/ResendVerificationPage.tsx`
- [ ] T033 [US2] Implementar operações de verificação/reenvio no serviço frontend e registrar rotas públicas em `frontend/src/services/authService.ts` e `frontend/src/App.tsx`
- [ ] T034 [P] [US2] Documentar caminhos, validade de 24 horas, uso único e respostas genéricas em `specs/015-user-account-authentication/contracts/account-authentication-api.openapi.yaml`

**Checkpoint**: usuário pode confirmar endereço e renovar link; tokens persistidos não revelam valor original.

---

## Phase 5: User Story 3 - Entrar na conta (Priority: P1)

**Goal**: Conta ativa e verificada inicia sessão com JWT existente; credenciais/estado não são enumeráveis.

**Independent Test**: Verificar login bem-sucedido e claims compatíveis; confirmar mesmo erro genérico para conta inexistente, senha errada, inativa e não verificada; sessão frontend permanece em memória.

### Tests for User Story 3

- [ ] T035 [P] [US3] Atualizar testes de API para login de conta verificada, bloqueio de conta não verificada e resposta genérica sem quebra de HS256/`sub`/`roles`/24h em `backend/user-service/src/test/java/com/techstore/user/AuthApiIntegrationTest.java`
- [ ] T036 [P] [US3] Testar formulário existente para erro genérico de não verificado, falha de rede, resposta malformada e nenhuma sessão falsa em `frontend/src/pages/__tests__/LoginPage.test.tsx`

### Implementation for User Story 3

- [ ] T037 [US3] Alterar autenticação para emitir JWT somente a usuário ativo e com e-mail verificado, preservando mensagem idêntica de falha em `backend/user-service/src/main/java/com/techstore/user/application/service/AuthService.java`
- [ ] T038 [US3] Manter login existente e ajustar feedback acessível para conta não verificada com caminho de reenvio sem revelar existência de e-mail em `frontend/src/pages/LoginPage.tsx` e `frontend/src/components/auth/LoginForm.tsx`
- [ ] T039 [US3] Garantir que o store continue mantendo JWT só em memória e que o interceptor continue anexando Bearer às chamadas protegidas em `frontend/src/store/authStore.ts` e `frontend/src/services/apiClient.ts`
- [ ] T040 [P] [US3] Atualizar contrato de login com `auth_version` interno sem expor a claim na resposta e documentar falha genérica para conta não verificada em `specs/015-user-account-authentication/contracts/account-authentication-api.openapi.yaml`

**Checkpoint**: login exige confirmação e os JWTs mantêm compatibilidade com os validadores atuais mais a versão de revogação.

---

## Phase 6: User Story 4 - Sair da conta (Priority: P1)

**Goal**: Usuário remove sua sessão local e retorna à loja sem apagar o carrinho persistido.

**Independent Test**: Autenticar, preencher stores privados, sair e confirmar limpeza local/retorno público; entrar novamente e recuperar o carrinho pelo Cart Service.

### Tests for User Story 4

- [ ] T041 [P] [US4] Testar logout limpa sessão e estado privado sem chamar endpoint destrutivo de carrinho em `frontend/src/__tests__/App.test.tsx` e `frontend/src/store/__tests__/authStore.test.ts`
- [ ] T042 [P] [US4] Testar header para visitante/autenticado, ação de sair, teclado e redirecionamento público em `frontend/src/__tests__/App.test.tsx`

### Implementation for User Story 4

- [ ] T043 [US4] Implementar ação de logout que limpa auth/catalog/cart stores locais e navega para rota pública sem request de limpeza do backend em `frontend/src/store/authStore.ts`, `frontend/src/store/cartStore.ts` e `frontend/src/App.tsx`
- [ ] T044 [US4] Alterar header para mostrar Entrar/Criar conta a visitantes e nome/avatar, Minha conta e Sair a autenticados, preservando badge do store de carrinho em `frontend/src/App.tsx`

**Checkpoint**: logout encerra o estado no dispositivo e não elimina dados do carrinho no servidor.

---

## Phase 7: User Story 5 - Recuperar e redefinir senha (Priority: P1)

**Goal**: Usuário solicita recuperação sem enumeração, define nova senha com token de uma hora e revoga JWTs anteriores.

**Independent Test**: Recuperar senha com fake mail, aceitar token válido uma vez, rejeitar expirado/reutilizado e comprovar que JWTs anteriores falham em User/Cart/Product e novo login funciona.

### Tests for User Story 5

- [ ] T045 [P] [US5] Testar recuperação genérica para e-mail existente/inexistente, token hash/expiração/uso único e falha do mail sender sem conteúdo sensível em `backend/user-service/src/test/java/com/techstore/user/PasswordResetIntegrationTest.java`
- [ ] T046 [P] [US5] Testar reset incrementa `auth_version` atomicamente com hash BCrypt e invalida tokens concorrentes em `backend/user-service/src/test/java/com/techstore/user/PasswordResetIntegrationTest.java`
- [ ] T047 [P] [US5] Testar login/redefinição, sessão expirada e novas tentativas de token em `frontend/src/pages/__tests__/ForgotPasswordPage.test.tsx` e `frontend/src/pages/__tests__/ResetPasswordPage.test.tsx`

### Implementation for User Story 5

- [ ] T048 [US5] Implementar emissão e persistência hash de token de reset com validade de uma hora e resposta de recuperação uniforme em `backend/user-service/src/main/java/com/techstore/user/application/service/PasswordResetService.java`
- [ ] T049 [US5] Implementar consumo transacional do token, validação da nova senha e atualização BCrypt mais incremento de `auth_version` em `backend/user-service/src/main/java/com/techstore/user/application/service/PasswordResetService.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/persistence/`
- [ ] T050 [US5] Criar adapter JPA de token de reset com lookup por hash, invalidação de tokens anteriores e proteção contra consumo concorrente em `backend/user-service/src/main/java/com/techstore/user/adapter/persistence/PasswordResetTokenEntity.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/persistence/PasswordResetTokenJpaRepository.java`
- [ ] T051 [US5] Expor endpoints forgot/reset e validação dos DTOs conforme o contrato, sem incluir informação da conta na resposta em `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthController.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/http/AuthModels.java`
- [ ] T052 [US5] Criar páginas frontend de esqueci senha e redefinição com confirmação, validação, loading e respostas genéricas em `frontend/src/pages/ForgotPasswordPage.tsx` e `frontend/src/pages/ResetPasswordPage.tsx`
- [ ] T053 [US5] Implementar requests e rotas públicas de recuperação/redifinição sem persistir token ou senha na sessão em `frontend/src/services/authService.ts`, `frontend/src/types/auth.ts` e `frontend/src/App.tsx`
- [ ] T054 [P] [US5] Atualizar contrato OpenAPI com token de reset de uma hora, antienumeração e invalidação de JWTs anteriores em `specs/015-user-account-authentication/contracts/account-authentication-api.openapi.yaml`

**Checkpoint**: redefinição atualiza credencial e invalida imediatamente sessões anteriores nos três serviços consumidores.

---

## Phase 8: User Story 8 - Reutilizar o carrinho da conta (Priority: P1)

**Goal**: Carrinho continua pertencendo ao usuário autenticado e não cruza entre contas ou logout/login.

**Independent Test**: Usuário A adiciona item, faz logout e login novamente e recupera-o; usuário B não vê nem altera o carrinho de A.

### Tests for User Story 8

- [ ] T055 [P] [US8] Testar isolamento do Cart Service entre subjects e negação de JWT revogado na camada de segurança em `backend/cart-service/src/test/java/com/techstore/cart/adapter/security/CartSecurityIntegrationTest.java`
- [ ] T056 [P] [US8] Testar recuperação de carrinho ao trocar sessão e badge baseado somente no cart store confirmado em `frontend/src/store/__tests__/cartStore.test.ts` e `frontend/src/__tests__/App.test.tsx`

### Implementation for User Story 8

- [ ] T057 [US8] Integrar mudança/logout de sessão com limpeza de dados privados locais e carregamento do carrinho da nova identidade, sem alterar contratos de cart API em `frontend/src/store/cartStore.ts` e `frontend/src/App.tsx`
- [ ] T058 [US8] Confirmar que Cart Service deriva owner do JWT validado e que introspecção não altera operações nem persistência do carrinho em `backend/cart-service/src/main/java/com/techstore/cart/adapter/security/CartOwnerResolver.java` e `backend/cart-service/src/main/java/com/techstore/cart/adapter/security/SecurityConfig.java`

**Checkpoint**: dados de carrinho sobrevivem a logout/login da mesma conta e continuam isolados por subject.

---

## Phase 9: User Story 9 - Navegar e acessar recursos conforme a sessão (Priority: P1)

**Goal**: Visitantes mantêm acesso às rotas públicas; recursos de conta exigem autenticação frontend e backend.

**Independent Test**: Navegar nas rotas públicas sem sessão, acessar rota privada anonimamente e com sessão expirada, e chamar diretamente endpoints sem JWT; nenhum conteúdo privado é retornado.

### Tests for User Story 9

- [ ] T059 [P] [US9] Testar acesso/redirecionamento de rotas públicas e privadas com sessão ausente, válida e expirada em `frontend/src/__tests__/App.test.tsx`
- [ ] T060 [P] [US9] Testar endpoints de conta sem token, token inválido, token expirado e token revogado retornam negação sem dados em `backend/user-service/src/test/java/com/techstore/user/AccountSecurityIntegrationTest.java`

### Implementation for User Story 9

- [ ] T061 [US9] Criar guard de rota de conta com retorno após login seguro, sem renderização temporária de dados privados e limpeza de sessão expirada em `frontend/src/components/auth/RequireAuth.tsx` e `frontend/src/App.tsx`
- [ ] T062 [US9] Restringir todos os endpoints `/users/me` no backend ao subject autenticado e manter `/auth/**` público somente nos fluxos previstos em `backend/user-service/src/main/java/com/techstore/user/adapter/config/SecurityConfig.java`

**Checkpoint**: frontend e backend protegem contas; Home, catálogo, detalhe, login e cadastro continuam públicos.

---

## Phase 10: User Story 6 - Consultar e editar perfil (Priority: P2)

**Goal**: Usuário autenticado visualiza perfil permitido e edita somente o nome.

**Independent Test**: Consultar e atualizar o próprio perfil; confirmar e-mail/verificação não mudam e requests de visitante ou de campos não permitidos falham.

### Tests for User Story 6

- [ ] T063 [P] [US6] Testar leitura/edição de perfil, validação de nome, dados legados sem nome e ausência de campos sensíveis em `backend/user-service/src/test/java/com/techstore/user/adapter/http/MyProfileControllerTest.java`
- [ ] T064 [P] [US6] Testar apresentação e edição do perfil, loading/erro/sucesso e ausência de controles para e-mail/role em `frontend/src/pages/__tests__/AccountPage.test.tsx`

### Implementation for User Story 6

- [ ] T065 [US6] Implementar casos de uso de leitura e atualização somente do nome autenticado em `backend/user-service/src/main/java/com/techstore/user/application/service/MyProfileService.java` e `backend/user-service/src/main/java/com/techstore/user/domain/User.java`
- [ ] T066 [US6] Adicionar `GET/PUT /users/me` com identidade obtida do subject JWT e DTO sem senha/hash/versão interna em `backend/user-service/src/main/java/com/techstore/user/adapter/http/MyProfileController.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/http/UserModels.java`
- [ ] T067 [US6] Implementar cliente de perfil e tipos sem dados sensíveis em `frontend/src/services/accountService.ts` e `frontend/src/types/account.ts`
- [ ] T068 [US6] Criar tela “Minha conta” e edição de nome com estados de dados legados, loading, erro e sucesso em `frontend/src/pages/AccountPage.tsx` e `frontend/src/pages/AccountPage.css`
- [ ] T069 [US6] Ligar rota privada `/account` e navegação “Minha conta” ao componente protegido em `frontend/src/App.tsx`

**Checkpoint**: perfil mostra dados próprios e só permite atualizar nome.

---

## Phase 11: User Story 7 - Alterar senha autenticado (Priority: P2)

**Goal**: Usuário autenticado troca senha após validar a atual, sem armazenar senha em texto puro.

**Independent Test**: Alterar com credencial atual válida, rejeitar incorreta e confirmar que nova senha autentica e tokens anteriores deixam de autorizar.

### Tests for User Story 7

- [ ] T070 [P] [US7] Testar alteração autenticada, senha atual errada, política da nova senha e que a senha anterior deixa de autenticar após sucesso em `backend/user-service/src/test/java/com/techstore/user/ChangePasswordIntegrationTest.java`
- [ ] T071 [P] [US7] Testar formulário de segurança, confirmações divergentes, loading e feedback sem expor valores de senha em `frontend/src/pages/__tests__/AccountSecurityPage.test.tsx`

### Implementation for User Story 7

- [ ] T072 [US7] Implementar caso de uso para validar senha atual e atualizar somente o hash BCrypt; manter revogação imediata de JWTs vinculada ao fluxo de redefinição em `backend/user-service/src/main/java/com/techstore/user/application/service/ChangePasswordService.java`
- [ ] T073 [US7] Expor `PUT /users/me/password` usando o subject do JWT e respostas sem dados de credencial em `backend/user-service/src/main/java/com/techstore/user/adapter/http/MyProfileController.java` e `backend/user-service/src/main/java/com/techstore/user/adapter/http/UserModels.java`
- [ ] T074 [US7] Implementar chamada de alteração de senha e limpeza da sessão local para exigir autenticação renovada em `frontend/src/services/accountService.ts` e `frontend/src/pages/AccountSecurityPage.tsx`
- [ ] T075 [US7] Criar rota `/account/security` e navegação entre perfil e segurança em `frontend/src/App.tsx` e `frontend/src/pages/AccountPage.tsx`

**Checkpoint**: alteração autenticada usa verificação da senha atual e força novo login por revogação de sessões anteriores.

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Documentação operacional, teste integrado de ponta a ponta e verificação de segurança/transversal.

- [ ] T076 Atualizar documentação de variáveis SMTP/`APP_PUBLIC_URL`, rede privada e rollout Railway com sessões legadas em `.env.example` e `DEPLOYMENT.md`
- [ ] T077 Atualizar configurações de CORS/rotas públicas somente se os novos endpoints externos exigirem encaminhamento; manter introspecção fora do Gateway em `backend/api-gateway/src/main/resources/application.yml`
- [ ] T078 Adicionar/atualizar métricas e logs sanitizados para envio de e-mail, introspecção, falhas e correlation ID sem tokens em `backend/user-service/src/main/java/com/techstore/user/adapter/` e adapters de segurança Cart/Product
- [ ] T079 [P] Verificar contrato OpenAPI, modelo e rotas contra schema/implementação em `specs/015-user-account-authentication/contracts/account-authentication-api.openapi.yaml`, `specs/015-user-account-authentication/data-model.md` e `backend/user-service/src/test/java/com/techstore/user/`
- [ ] T080 Executar testes e verify backend, testes/build frontend e os cenários de fumaça de `specs/015-user-account-authentication/quickstart.md`
- [ ] T081 Registrar limites/necessidade de rate limiting para login, recuperação e reenvio e evidência operacional/latência de introspecção em `specs/015-user-account-authentication/quickstart.md` e `DEPLOYMENT.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências de feature; dependências/configuração compartilhada.
- **Foundational (Phase 2)**: Depende de Setup e bloqueia todas as histórias que emitem/validam JWT, usam migrations ou exigem revogação central.
- **User Stories (Phase 3+)**: Começam após Foundation; respeitam dependências entre histórias descritas abaixo.
- **Polish (Phase 12)**: Depende das histórias selecionadas e da validação integrada.

### User Story Dependencies

- **US1 (P1)**: Depois da Foundation; cria identidade e envia início de verificação.
- **US2 (P1)**: Depende de US1 para cadastro/token e habilita contas para login.
- **US3 (P1)**: Depende de US1 e US2; login elegível exige conta verificada.
- **US4 (P1)**: Depende de US3; limpa estado frontend sem apagar carrinho servidor.
- **US5 (P1)**: Depende da Foundation e do envio de e-mail; usa `auth_version` para revogar sessões antigas.
- **US8 (P1)**: Depende de US3 e US4; integra troca de identidade ao estado existente de carrinho.
- **US9 (P1)**: Depende de US3; protege rotas de conta e endpoints em backend. As telas de perfil são adicionadas por US6/US7.
- **US6 (P2)**: Depende de US3 e US9; depende de guard e sessão válida para perfil.
- **US7 (P2)**: Depende de US3, US5/Foundation (versão de revogação) e US9.

### Within Each User Story

- Executar testes automatizados definidos antes/ao lado da implementação e provar comportamento de aceitação por história.
- Alterar domínio/persistência antes do adapter HTTP que depende deles.
- Criar endpoints antes de integrar telas frontend.
- Não habilitar UI que afirme sucesso de envio se o serviço indicar falha.
- Completar checkpoints antes de iniciar dependentes.

### Parallel Opportunities

- **Setup**: T001 pode ser feito em paralelo a T002; T003 depende da definição das propriedades em T001/T002.
- **Foundation**: T004 e T007 podem avançar em paralelo; T005 depende do teste T004 e T006 depende da migration; T008 depende do modelo persistido; T009 deve falhar antes da implementação T010; T011/T012 podem avançar em paralelo após T010 e o contrato interno; T013 testa as adaptações dos dois consumidores.
- **US1**: T015–T017 são testes em arquivos independentes; após contratos base, T023 pode avançar separado do backend.
- **US2**: testes do backend e páginas frontend têm arquivos separados; endpoint e UI podem avançar em paralelo após contrato definido.
- **US3**: testes backend/frontend são independentes; trabalho frontend pode avançar após interface existente do serviço.
- **US5**: T045–T047 são testes de superfícies separadas; UI pode avançar em paralelo aos casos de uso após contrato.
- **US8**: teste Cart e teste frontend ficam em arquivos independentes.
- **US6/US7**: testes frontend/backend independentes; após endpoints estáveis, UI e refinamento OpenAPI podem avançar em paralelo.

### Parallel Example: Foundation JWT Validation

```text
Após T010 e definição do contrato privado:
T011 Cart JWT validation adapter
T012 Product JWT validation adapter
Em paralelo, depois das duas implementações:
T013 Cart/Product security integration tests
```

### Implementation Strategy

#### Incremento mínimo utilizável

O mínimo para uma conta utilizável é **US1 + US2 + US3** após a Foundation: registrar, confirmar e autenticar. US1 isolada não permite login por decisão de segurança e não representa uma jornada completa.

#### Entrega incremental

1. Setup + Foundation: migration, estado de autenticação e revogação global preparados.
2. US1 + US2 + US3: cadastro, verificação e entrada completos; validar em separado antes de publicar.
3. US4 + US8: logout e continuidade/isolamento do carrinho.
4. US5: recuperação e redefinição, com revogação imediata validada nos três serviços.
5. US9 + US6 + US7: proteção de rotas, perfil e segurança da conta.
6. Polish: configuração Railway/SMTP, observabilidade, testes full e quickstart.

#### Parallel Team Strategy

Após a Foundation, dividir trabalho por domínio: User Service/e-mail, frontend de conta e adapters de validação Cart/Product. Manter a integração dos consumidores e o rollout coordenados antes de habilitar tokens com `auth_version`; não executar implantação parcial em produção.
