# Implementation Plan: Conta do Usuário e Autenticação

**Branch**: `015-user-account-authentication` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

**Input**: Especificação clarificada em `specs/015-user-account-authentication/spec.md`.

## Summary

Completar a conta TechStore evoluindo o User Service, cadastro/login e estado de sessão frontend já existentes. Acrescentar confirmação de e-mail, recuperação e alteração de senha, perfil, navegação e rotas protegidas sem introduzir um novo serviço de usuários nem alterar regras de catálogo ou carrinho.

Para cumprir a decisão de revogação imediata de todas as sessões após redefinição, manter um contador de versão de autenticação por usuário no User Service, incluí-lo em JWTs novos e validar a versão atual centralmente para cada requisição autenticada recebida pelo User, Cart e Product Services. A validação remota deve falhar fechada, permanecer em rede privada fora das rotas públicas do Gateway e retornar somente estado ativo/inativo. Este mecanismo mantém a emissão HS256 e as claims `sub` e `roles`, mas introduz uma chamada síncrona de validação por requisição protegida.

Adicionar migration Flyway aditiva para nome, verificação e versão de autenticação, mais tabelas de tokens temporários de confirmação e redefinição. Configurar um adapter SMTP através de variáveis de ambiente, com falhas de entrega sem apagar a conta criada e adapter falso para testes. Construir as telas de cadastro, confirmação, recuperação, redefinição, conta, perfil e segurança sobre React/TypeScript, Router, Zustand e cliente Axios existentes.

## Technical Context

**Language/Version**: Java 21; TypeScript 5.7.2; React 18.3.1.

**Primary Dependencies**: Spring Boot 3.3.13, Spring Web, Data JPA, Security, OAuth2 Resource Server/JWT, Validation, Flyway, PostgreSQL JDBC e springdoc OpenAPI; adicionar Spring Boot Mail para SMTP. Frontend existente: Vite 5.4, React Router 6.28, Zustand 5, Axios 1.7, Vitest 2.1 e Testing Library.

**Storage**: PostgreSQL 15+, database lógico próprio do User Service (`techstore_user_db`); migrations Flyway versionadas. Nenhum serviço compartilha tabelas ou acessa o database lógico do User Service diretamente.

**Testing**: JUnit 5, Mockito, Spring Security Test, MockMvc e Testcontainers PostgreSQL; Vitest, jsdom e React Testing Library. Validação final deve incluir suites backend/frontend e builds Maven/npm.

**Target Platform**: Containers Linux Java 21 no Railway; frontend SPA em browsers desktop/mobile. Gateway é a única entrada externa publicada; User Service fica privado para introspecção.

**Project Type**: Monorepo de microsserviços Spring Boot com frontend React/TypeScript e API Gateway.

**Performance Goals**: A especificação não define SLO ou carga-alvo. A introspecção síncrona adiciona uma ida ao User Service por requisição protegida; usar timeouts curtos configuráveis, propagar correlation ID e medir latência/erros. Não cachear validação positiva, pois isso violaria a revogação imediata. Falha de autoridade de autenticação deve negar a operação e retornar indisponibilidade, nunca aceitar o JWT sem validação central.

**Constraints**:

- Preservar emissão JWT HS256, segredo compartilhado, claim `sub`, claim `roles` e duração atual de 24 horas; incluir claim de versão de autenticação nos tokens novos.
- Cadastro público cria somente `USER`; role administrativa e bootstrap existente não podem ser alterados por payload público.
- Exigir e-mail verificado para login. Contas preexistentes sem prova de confirmação serão não verificadas e precisarão completar o fluxo antes de novo login.
- Senhas continuam BCrypt com a política existente (8–72 caracteres); nunca armazenar senha ou valor original de token temporário.
- Tokens temporários: confirmação por 24 horas; redefinição por 1 hora; uso único.
- A redefinição incrementa a versão de autenticação do usuário e invalida tokens anteriores em User/Cart/Product. O Cart e o Product Service recebem mudanças somente para consultar a revogação; regras de catálogo, carrinho e APIs de negócio ficam inalteradas.
- Envio é feito por SMTP/provider externo configurável; não criar servidor próprio, fila ou infraestrutura de e-mail. A falha de entrega não remove a conta nem pode ser apresentada como e-mail entregue. Links usam a origem pública da aplicação fornecida por configuração de ambiente.
- Respostas de login e recuperação/reenvio não enumeram contas. Logs não incluem tokens, senhas, JWTs, credenciais nem conteúdo de mensagens.
- Migrations já aplicadas não são editadas. Nenhum nome fictício será gerado para preencher contas legadas.
- Preservar sessão frontend em memória e o store/serviço existente do carrinho; logout não apaga carrinho.
- Sem checkout, pagamento, pedidos, shipping, tracking, reserva de estoque, newsletter ou alterações às regras de catálogo/carrinho.

**Scale/Scope**: User Service (domínio, aplicação, portas, adapters HTTP/JPA/security/mail e configuração), Cart e Product adapters de segurança e cliente de introspecção, regras de rota do API Gateway quando necessário, frontend (rotas, formulários, estado/header/conta), migration/configuração e documentação de deployment. Nenhum database lógico ou microsserviço novo.

## Constitution Check

| Principle | Status | Evidence |
|---|---|---|
| I. Domain-Driven Microservices | PASS | User continua proprietário dos dados de conta; Cart/Product só consomem uma resposta restrita de validação de sessão e não compartilham database. |
| II. Hexagonal Architecture | PASS | Regras de conta/tokens ficam no domínio/aplicação; SMTP, persistência, HTTP e JWT são adapters/ports. |
| III. Clean Code and SOLID | PASS | Reutilizar casos de uso e adapters existentes; acrescentar portas focadas para e-mail, tokens e validação de sessão, sem criar serviço de conta paralelo. |
| IV. Test-First Quality | PASS | Cobrir regras de domínio, casos de uso, migration/API/security, revogação interserviços, UI e integração com fake email adapter. |
| V. API and Contract Discipline | PASS | Estender contrato OpenAPI do User Service sem mudar endpoints públicos existentes; documentar a introspecção como contrato interno não roteado pelo Gateway. |
| VI. Security by Default | PASS | BCrypt, token hashes, e-mail verificado, respostas antienumeração, autorização backend, introspecção fail-closed e segredos por ambiente. |
| VII. Observability | PASS | Preservar correlationId, adicionar métricas/logs sanitizados para envio, validação e indisponibilidade de introspecção. |
| VIII. Frontend Architecture | PASS | UI usa rotas/componentes/stores/clientes separados; autorização definitiva permanece no backend. |
| IX. Infrastructure and Reproducibility | PASS | Configuração SMTP/URLs por ambiente; User Service permanece privado e SMTP precisa estar acessível pela rede Railway. Sem nova infraestrutura gerenciada. |
| X. Spec-Driven Development | PASS | Spec clarificada precede este plano; tarefas, implementação, análise e convergência seguem as fases seguintes. |

**Gate antes da pesquisa**: PASS. A decisão de revogação imediata foi explicitamente autorizada e registrada na spec.

**Gate após o design**: PASS com risco operacional registrado. Introspecção síncrona é necessária para cumprir a revogação sem aceitar tokens antigos nos consumidores. Indisponibilidade do User Service bloqueia operações protegidas em Cart/Product; clientes devem falhar fechados, ter timeout curto e erro `503` explícito.

## Project Structure

### Documentation (this feature)

```text
specs/015-user-account-authentication/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── account-authentication-api.openapi.yaml
└── tasks.md                 # criado por /speckit.tasks
```

### Source Code (repository root)

```text
backend/
├── user-service/
│   └── src/
│       ├── main/java/com/techstore/user/
│       │   ├── domain/                       # conta, política e ciclo de tokens
│       │   ├── application/port/in/          # cadastro, verificação, recuperação, perfil, senha
│       │   ├── application/port/out/         # repositório, email sender, issuer/clock
│       │   └── adapter/
│       │       ├── http/                     # endpoints públicos e introspecção interna
│       │       ├── persistence/              # adapters e entidades JPA
│       │       ├── security/                 # BCrypt, JWT e validação de auth version
│       │       └── mail/                     # adapter SMTP
│       ├── main/resources/db/migration/      # nova migration versionada; preservar V1
│       └── test/                             # domínio, aplicação, contrato, integração
├── cart-service/
│   └── src/main/java/com/techstore/cart/adapter/security/ # validação remota fail-closed
├── product-service/
│   └── src/main/java/com/techstore/product/adapter/config/ # validação remota fail-closed
└── api-gateway/
    └── src/main/resources/application.yml    # somente se novas rotas públicas precisarem de forwarding

frontend/src/
├── App.tsx                                    # rotas públicas/privadas e header autenticado
├── components/auth/                           # formulários reutilizáveis e estados de auth
├── pages/                                     # register, verify, forgot/reset, account/profile/security
├── services/                                  # auth/profile service via apiClient existente
├── store/                                     # limpar sessão/stores dependentes no logout
└── types/

.env.example                                   # somente nomes/placeholders de ambiente
DEPLOYMENT.md                                  # env SMTP, introspecção privada e rollout Railway
docker-compose.yml                             # URLs internas de desenvolvimento
```

**Structure Decision**: Evoluir o monorepo e layouts hexagonais existentes. Nenhum pacote compartilhado novo é presumido; contratos entre serviços são implementados em adapters locais para manter os serviços desacoplados.

## Sequência de implementação planejada

1. Confirmar contrato JWT atual e configuração de deployment; adicionar migration Flyway sem editar V1: nome opcional para contas legadas, `email_verified` false por padrão, `auth_version` inicial e tabelas/índices de tokens temporários.
2. Evoluir domínio/ports/use cases de conta, cadastro com nome, verificação/resend, perfil, atualização de senha, recuperação/reset e versão de autenticação. Garantir transações para consumo único, rotação de tokens e atualização do hash/versão após reset de recuperação.
3. Implementar SMTP adapter atrás de porta, templates de confirmação/reset, configuração externa e resposta que não afirme envio quando o provedor falha; teste via fake adapter. Não criar fila/outbox/servidor SMTP.
4. Estender controladores/DTOs e contrato OpenAPI: reutilizar register/login existentes, adicionar apenas endpoints ausentes e `/users/me`; adicionar endpoint interno mínimo de introspecção sem expor rota pelo Gateway nem retornar dados da conta.
5. Emitir JWT existente com claim `auth_version`. O User Service compara a versão e estado ativo em cada autenticação protegida; redefinição de senha incrementa a versão. Endpoint de introspecção verifica assinatura/expiração/claims e consulta versão/estado persistidos.
6. Integrar Cart e Product Service por cliente HTTP interno para introspecção em cada request JWT autenticada; manter validação local de assinatura/claims/roles, timeout curto configurável, `503` em indisponibilidade e nenhuma aceitação por fallback/cache positivo.
7. Criar UI e fluxos frontend em cima do `apiClient` e stores existentes: cadastro, login condicionado à verificação, verificar/re-enviar, recuperação/redefinição, conta/perfil/segurança, rotas protegidas, header guest/authenticated e logout que limpa stores dependentes sem apagar carrinho backend.
8. Atualizar Railway/Compose/env docs para SMTP, URLs privadas de introspecção e dependências de inicialização/health; documentar rollout coordenado, a necessidade de novos logins para sessões antigas sem `auth_version` e indisponibilidade fail-closed.
9. Adicionar testes unitários, API/security, migration Testcontainers, contract, integração de introspecção (resposta ativa/revogada/expirada/timeout), testes frontend e smoke de Gateway. Executar suites backend/frontend e builds.

## Complexity Tracking

| Violation / tradeoff | Why needed | Simpler alternative rejected because |
|---|---|---|
| Introspecção síncrona User Service em todas as requisições JWT protegidas de Cart/Product | A decisão aprovada exige revogação imediata de todas as sessões após redefinição, inclusive em serviços que validam JWT localmente. | JWT stateless local não conhece mudanças de senha; cache positiva atrasa revogação; expiração reduz apenas a janela; Redis/blacklist introduziria nova infraestrutura. |
| Dependência de disponibilidade User → Cart/Product | Validação deve falhar fechada para não autorizar JWT revogado. | Aceitar token em falha tornaria a promessa de revogação falsa e insegura. |
