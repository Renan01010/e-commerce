---
description: "Tarefas de implementação da feature Login do Cliente"
---

# Tasks: Login do Cliente

**Input**: Documentos de design em `specs/005-frontend-login/`.

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contrato OpenAPI](contracts/auth-login-api.openapi.yaml).

**Testes**: Incluídos conforme a Constituição TechStore: regras de interação/autenticação do frontend recebem testes unitários e de componente antes da implementação.

**Organização**: As tarefas são agrupadas pelas histórias da spec para implementação incremental; o estado de sessão compartilhado é fundação para todas.

**Formato**: `- [ ] Tnnn [P?] [USn?] descrição com caminho concreto`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar dependência de ícones e asset local aprovado para o hero visual.

- [X] T001 Adicionar `lucide-react` às dependências de `frontend/package.json` e atualizar/criar `frontend/package-lock.json` com a instalação reproduzível.
- [X] T002 [P] Adicionar uma fotografia local licenciada/aprovada para a área hero em `frontend/src/assets/login-hero.webp`; usar o protótipo anexado como direção visual, não como imagem de runtime.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Definir contrato tipado, estado em memória e serviço HTTP compartilhado que bloqueiam as histórias.

**Checkpoint**: Nenhuma tela envia credenciais ou usa token antes desta fase.

- [X] T003 [P] Escrever testes do ciclo de sessão em `frontend/src/store/__tests__/authStore.test.ts`, cobrindo sessão ausente, armazenamento em memória, expiração, limpeza e ausência de persistência após reinicialização do store.
- [X] T004 [P] Escrever testes do contrato de login em `frontend/src/services/__tests__/authService.test.ts`, verificando `POST /auth/login`, request `{ email, password }`, resposta válida, resposta sem token e mapeamento seguro de 401/rede/5xx.
- [X] T005 [P] Escrever testes do interceptor em `frontend/src/services/__tests__/apiClient.test.ts`, cobrindo envio de bearer apenas para sessão válida e ausência do header sem sessão ou com token expirado.
- [X] T006 Criar tipos em `frontend/src/types/auth.ts` para `LoginCredentials`, resposta (`accessToken` string não vazia, `tokenType` `Bearer`, `expiresIn` inteiro positivo) e `AuthSession` (`accessToken`, `tokenType`, `expiresAt` derivado de `expiresIn`).
- [X] T007 Implementar `frontend/src/store/authStore.ts` com Zustand em memória, mantendo `AuthSession` somente na vida do SPA, sem `persist` middleware nem `localStorage`/`sessionStorage`.
- [X] T008 Atualizar `frontend/src/services/apiClient.ts` para expor o cliente Axios compartilhado e adicionar `Authorization: Bearer <accessToken>` somente quando o store tem sessão ainda não expirada; não enviar token expirado.
- [X] T009 Criar `frontend/src/services/authService.ts` usando o cliente compartilhado para `POST /auth/login`; validar a presença/utilidade da resposta, mapear 401 para mensagem genérica e falha sem resposta/timeout/5xx para mensagem de indisponibilidade sem expor detalhes do backend.

---

## Phase 3: User Story 1 - Entrar na conta (Priority: P1)

**Goal**: Permitir login real com e-mail e senha, estabelecer sessão volátil e navegar para a Home.

**Independent Test**: Com `authService` substituído por resposta válida no teste de componente, preencher e-mail/senha, enviar uma vez, confirmar `AuthSession` em memória e navegação para `/`; o quickstart valida depois o endpoint real.

### Tests for User Story 1

- [X] T010 [P] [US1] Escrever `frontend/src/pages/__tests__/LoginPage.test.tsx` para login válido: chama serviço, grava token e expiração no store e redireciona para `/`.

### Implementation for User Story 1

- [X] T011 [US1] Criar `frontend/src/pages/LoginPage.tsx` com a composição da página e `frontend/src/components/auth/LoginForm.tsx` com campos rotulados de e-mail/senha e submissão ao serviço, sem autenticação mockada.
- [X] T012 [US1] Registrar `/login` em `frontend/src/App.tsx` e conectar o sucesso do formulário ao store compartilhado e à navegação existente para `/`.

**Checkpoint**: Um login real bem-sucedido estabelece sessão em memória e chega à Home sem modificar o catálogo.

---

## Phase 4: User Story 2 - Corrigir entrada e entender falhas (Priority: P1)

**Goal**: Validar campos, informar carregamento/erros e oferecer controle de senha acessível em desktop e mobile.

**Independent Test**: Com chamadas de serviço controladas no teste de componente, confirmar bloqueio de campos vazios/e-mail inválido, loading sem submissão duplicada, senha alternável, mensagens genéricas para 401/rede e layout utilizável em largura móvel.

### Tests for User Story 2

- [X] T013 [P] [US2] Escrever `frontend/src/components/auth/__tests__/LoginForm.test.tsx` para campos obrigatórios, formato básico de e-mail, foco/labels, indicador de loading, botão desabilitado e mensagens genéricas para falha de autenticação/rede.
- [X] T014 [P] [US2] Escrever `frontend/src/components/auth/__tests__/PasswordField.test.tsx` para senha inicialmente oculta, alternância visível/oculta por teclado e mouse, nome acessível do controle e preservação do valor.

### Implementation for User Story 2

- [X] T015 [US2] Completar `frontend/src/components/auth/LoginForm.tsx` com validação de presença/formato antes do request, bloqueio de double-submit, estado de carregamento, preservação do e-mail e mensagens de erro seguras sem credenciais ou detalhes do backend.
- [X] T016 [US2] Criar `frontend/src/components/auth/PasswordField.tsx` com controle acessível de mostrar/ocultar senha usando ícones de `lucide-react`, sem limpar ou alterar o valor digitado.
- [X] T017 [US2] Criar `frontend/src/pages/LoginPage.css` para composição desktop dividida e mobile em uma coluna, usando navy/azul somente no login e mantendo formulário, loading e erros visíveis sem sobreposição de 320 px a 1440 px.

**Checkpoint**: Erros locais e remotos não criam sessão; nenhum fluxo exibe se a conta existe, senha, token ou detalhes internos.

---

## Phase 5: User Story 3 - Ir para criação de conta (Priority: P2)

**Goal**: Disponibilizar navegação clara para a futura tela de cadastro, sem implementar cadastro.

**Independent Test**: Abrir a página de login no router de teste, navegar por teclado até o link e ativá-lo; confirmar a navegação para `/register`, o estado informativo não funcional e a ausência de request de cadastro ou alteração da sessão.

### Tests for User Story 3

- [X] T018 [P] [US3] Escrever `frontend/src/components/auth/__tests__/CreateAccountLink.test.tsx` para texto acessível, navegação por teclado até `/register`, exibição do placeholder e ausência de request de cadastro/alteração de sessão.

### Implementation for User Story 3

- [X] T019 [US3] Criar `frontend/src/components/auth/CreateAccountLink.tsx` e `frontend/src/pages/RegisterUnavailablePage.tsx`, registrar `/register` em `frontend/src/App.tsx` e mostrar “Cadastro indisponível no momento” sem formulário, serviço ou mutação de sessão.

**Checkpoint**: Visitantes encontram a navegação de cadastro, mas nenhuma autenticação/cadastro fictício é executado.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validar acessibilidade visual, segurança da sessão e integração real antes da entrega.

- [X] T020 [P] Revisar testes de `frontend/src/pages/__tests__/LoginPage.test.tsx` e `frontend/src/components/auth/__tests__/LoginForm.test.tsx` para ordem de tabulação, associação de labels, foco visível e anúncio de loading/erro.
- [X] T021 Revisar `frontend/src/services/authService.ts` e `frontend/src/services/apiClient.ts` para confirmar que senha/token não são logados, não há persistência de token e headers bearer não são enviados após expiração.
- [ ] T022 Executar `npm test` e `npm run build` em `frontend/`, corrigir falhas e registrar resultado em `specs/005-frontend-login/quickstart.md`.
- [ ] T023 Executar os cenários reais do Gateway/User Service em `specs/005-frontend-login/quickstart.md` com credenciais de teste válidas/inválidas e verificar desktop/mobile; atualizar o guia somente se comandos/rotas reais divergirem do contrato aprovado.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências; asset e pacote podem ser preparados em paralelo.
- **Foundational (Phase 2)**: depende do Setup e bloqueia as três histórias.
- **User Story 1 (Phase 3)**: depende da fundação; entrega o caminho de login válido.
- **User Story 2 (Phase 4)**: depende da estrutura da página/formulário de US1, mas seus testes podem ser escritos após Foundation; aperfeiçoa os estados de validação/erro e responsividade.
- **User Story 3 (Phase 5)**: depende da página de login de US1 para integrar o link, mas seu componente/teste é independente e não depende de auth service.
- **Polish (Phase 6)**: depende das histórias implementadas; T023 ainda requer Gateway/User Service reais.

### Dependency Graph

```text
Setup T001-T002
  -> Foundation T003-T009
       -> US1 T010-T012
       -> US2 T013-T017 (usa LoginForm criado em US1)
       -> US3 T018-T019 (link integrado à LoginPage de US1)
  -> Polish T020-T023
```

### Parallel Opportunities

- Setup: T001 e T002 atuam em arquivos/assets diferentes e podem ocorrer em paralelo.
- Foundation: T003-T005 são testes em arquivos separados; após definição dos tipos, store, Axios client e auth service são arquivos distintos, respeitando as dependências T006 → T007 → T008 → T009.
- US1: T010 deve ser escrito primeiro; T011 implementa página/formulário; T012 conecta rota e redirecionamento.
- US2: T013 e T014 são testes independentes e podem ser escritos em paralelo; T016 e T017 alteram arquivos diferentes após o contrato dos componentes estar definido.
- US3: T018 e T019 são específicos do link; a integração da página deve ocorrer após US1.

## Parallel Example: Foundation

```text
Em paralelo, antes da implementação:
T003 authStore.test.ts
T004 authService.test.ts
T005 apiClient.test.ts

Depois dos testes:
T006 types/auth.ts
T007 authStore.ts
T008 apiClient.ts (após authStore)
T009 authService.ts (após apiClient)
```

## Implementation Strategy

### MVP

1. Completar Setup e Foundation.
2. Implementar US1: login real, sessão em memória e redirecionamento para `/`.
3. Completar US2 antes de disponibilizar a tela, pois estados de erro, validação e acessibilidade são requisitos P1.
4. Validar o MVP com `npm test`/`npm run build` e credenciais reais via Gateway; US3 pode ser entregue em seguida como P2.

### Incremental Delivery

1. Setup + Foundation: dependência visual, tipos, store, cliente HTTP e serviço de auth cobertos por testes.
2. US1: credenciais válidas estabelecem sessão e redirecionam.
3. US2: validações, loading, feedback, visibilidade de senha e responsividade.
4. US3: link para `/register` com placeholder não funcional e sem fluxo fictício de cadastro.
5. Polish: acessibilidade, ausência de persistência/logs sensíveis e quickstart com backend real.