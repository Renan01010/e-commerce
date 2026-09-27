# Plano de Implementação: User Service e Autenticação

**Branch**: `003-user-auth` | **Data**: 2026-09-27 | **Spec**: [spec.md](spec.md)

**Entrada**: `specs/003-user-auth/spec.md` e decisões de clarificação respondidas durante `/speckit.plan`.

## Resumo

Adicionar um User Service independente ao monorepo para registro, login, bootstrap opcional do primeiro ADMIN e administração de roles. O serviço usa PostgreSQL lógico próprio, BCrypt e JWT HS256; o Gateway encaminha as rotas públicas `/api/auth/**` e administrativas `/api/users/**`, removendo o prefixo público `/api`. Gateway faz roteamento, enquanto User Service e Product Service mantêm a própria autorização. O contrato JWT preserva compatibilidade com o validador existente do Product Service (`sub`, `roles`, `iat`, `exp`, chave compartilhada).

O escopo não inclui frontend de login, refresh/logout, recuperação de senha, verificação de e-mail, MFA ou edição de perfil.

## Contexto Técnico

**Linguagem/Versão**: Java 21 (stack backend atual)

**Dependências Primárias**: Spring Boot 3.3.x, Spring Web, Spring Security Resource Server/JWT, Spring Data JPA, Bean Validation, Flyway, PostgreSQL JDBC, Spring Boot Actuator e springdoc OpenAPI.

**Armazenamento**: PostgreSQL, database lógico exclusivo `techstore_user_db`; Flyway é dono do schema.

**Testes**: JUnit 5, Mockito, Spring Security Test, MockMvc e Testcontainers PostgreSQL.

**Plataforma Alvo**: Container Linux Java 21; execução local via Maven/Docker Compose.

**Tipo de Projeto**: Microsserviço Spring Boot em monorepo Maven multi-módulo, integrado ao API Gateway.

**Metas de Desempenho**: Não definidas na spec; não introduzir meta artificial nesta feature. Medir latência básica do login e hash BCrypt nos testes/perfil local.

**Restrições**: Hexagonal Architecture; banco lógico isolado; senha somente em BCrypt (cost 12), entrada 8–72 caracteres e no máximo 72 bytes UTF-8; role única USER/ADMIN; bootstrap idempotente; segredo HS256 de pelo menos 32 bytes e externalizado; token válido por 24 horas; sem refresh token.

**Escala/Scope**: Cadastro e autenticação de usuários e 4 operações HTTP (`register`, `login`, criação administrativa e atualização administrativa); sem UI.

## Constitution Check

**Gate antes da pesquisa**

| Princípio | Estado | Evidência/condição |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | User Service possui bounded context e database lógico próprios; comunicação com Gateway é REST. |
| II. Arquitetura Hexagonal | PASS | Domínio e casos de uso não dependem de Spring/JPA; adapters encapsulam HTTP, JWT e persistência. |
| III. Clean Code/SOLID | PASS | Caso de uso separa autenticação/gestão; um UserRepository port mantém persistência fora do domínio. |
| IV. Test-first | PASS | Regras, autorização, contrato HTTP e migrações têm suites unitária e de integração planejadas. |
| V. Contratos de API | PASS | OpenAPI público versionado nesta feature; prefixo/reescrita documentados. |
| VI. Segurança por padrão | PASS | BCrypt, JWT HS256, ADMIN explícito, erro de login genérico, sem secrets/senhas nos logs. |
| VII. Observabilidade | PASS | Actuator, logs operacionais sem dados sensíveis e correlation ID propagado pelo Gateway. |
| VIII. Arquitetura frontend | N/A | Nenhum frontend incluído neste incremento. |
| IX. Reprodutibilidade | PASS | Maven, migrations, Compose e variáveis externalizadas; nenhum secret versionado. |
| X. Spec-driven | CONDITIONAL | Decisões foram incorporadas nos artefatos de design, mas o `spec.md` no workspace está truncado e repete clarificações P0. Revisar/completar esse documento antes da geração final de tasks e implementação. |

**Reavaliação pós-design**: sem violação arquitetural encontrada. O gate X permanece condicional à limpeza editorial da especificação original; este plano não a altera.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/003-user-auth/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── user-service-api.openapi.yaml
```

### Código-fonte planejado

```text
backend/
├── pom.xml                              # declarar módulo user-service
├── api-gateway/
│   └── src/main/resources/application.yml # rotas /api/auth/** e /api/users/**
└── user-service/
    ├── pom.xml
    ├── Dockerfile
    └── src/
        ├── main/java/com/techstore/user/
        │   ├── domain/                   # User, Role e regras sem frameworks
        │   ├── application/port/in/      # casos de uso
        │   ├── application/port/out/     # UserRepository
        │   ├── application/service/      # registro, login, administração
        │   └── adapter/
        │       ├── http/                 # controllers, DTOs, erros, validação
        │       ├── persistence/          # entidades Spring Data/JPA e adapter
        │       ├── security/             # BCrypt e emissão/validação JWT
        │       └── config/               # Security, datasource, OpenAPI
        ├── main/resources/
        │   ├── application.yml
        │   └── db/migration/
        └── test/java/com/techstore/user/
            ├── domain/
            ├── application/
            ├── adapter/http/
            └── adapter/persistence/

docker-compose.yml                       # User Service e DB lógico de desenvolvimento
.env.example                              # nomes das variáveis sem valores secretos reais
```

**Decisão de estrutura**: novo módulo backend independente, sem alterações no frontend. O API Gateway mantém somente roteamento; cada service valida role localmente. O User Service pode compartilhar a instância física PostgreSQL, mas recebe database e credenciais logicamente próprios.

## Sequência de implementação planejada

1. Criar módulo Maven, configuração externa, Dockerfile e database lógico local separado; configurar Flyway e migration `users`.
2. Implementar domínio puro, portas, adapter JPA e bootstrap idempotente do ADMIN.
3. Implementar casos de uso de registro público, login e CRUD administrativo limitado; usar BCrypt e regras para último ADMIN ativo.
4. Implementar JWT HS256 e security filter chain; garantir compatibilidade de algoritmo, claim `roles` e secret com Product Service.
5. Expor controllers/DTOs e tratamento de erros conforme OpenAPI; aplicar validação explícita dos nomes de path/query, se houver.
6. Adicionar rotas Gateway com `StripPrefix=1`, destino `USER_SERVICE_URL`, correlation ID e CORS; manter autorização no User Service.
7. Cobrir unit tests, MVC/security tests, Testcontainers/migrations e teste Gateway-to-User-Service; validar `quickstart.md`.
8. Revisar a spec truncada/duplicada, gerar tasks via `/speckit.tasks` e só então iniciar implementação.

## Riscos e controles

- **Compatibilidade JWT**: teste de contrato deve validar token emitido pelo User Service no Product Service existente, inclusive role `ADMIN` mapeada para `ROLE_ADMIN`.
- **Bootstrap concorrente**: constraint única por email e operação idempotente em múltiplas réplicas; falha não pode sobrescrever usuário existente.
- **Segredo**: mesmo `TECHSTORE_JWT_SECRET` em User Service e Product Service; nunca registrar valor. Rotação coordenada é operação futura/documentada.
- **BCrypt**: validar limite UTF-8 de 72 bytes para não aceitar senhas que o encoder trunca.
- **Último administrador**: serializar ou proteger atualização concorrente para impedir que duas operações removam simultaneamente o último ADMIN ativo.
- **Spec**: arquivo de entrada termina no meio do JSON de cadastro e repete clarificações P0. Corrigir a fonte antes do gate de tasks/implementação.

## Complexidade

Sem violações constitucionais nem serviços extras fora do escopo aprovado. A criação do User Service é necessária para manter autenticação e dados de usuário fora do Product Service.
