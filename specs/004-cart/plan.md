# Plano de Implementação: Carrinho de Compras

**Branch**: `004-cart` | **Data**: 2026-09-27 | **Spec**: [spec.md](spec.md)

**Entrada**: [Spec da feature](spec.md), incluindo a clarificação sobre resumo atual de produto.

## Resumo

Adicionar o Cart Service como microsserviço Spring Boot independente, com PostgreSQL lógico próprio, autenticação JWT local compatível com os serviços existentes e API REST exposta pelo Gateway. O armazenamento mantém apenas UUID do usuário, UUID do produto e quantidade. A validação e o resumo atual do catálogo vêm de uma chamada direta pela rede privada a `PRODUCT_SERVICE_URL/api/products/{id}` (sem passar pelo Gateway); produto removido/inativo continua na resposta do carrinho como indisponível, sem snapshot local.

## Contexto Técnico

**Linguagem/Versão**: Java 21.

**Dependências Primárias**: Spring Boot 3.3.13, Spring Web/RestClient, Spring Security OAuth2 Resource Server, Spring Data JPA, Bean Validation, Flyway, PostgreSQL JDBC, Actuator e springdoc OpenAPI 2.6.0; versões geridas pelo Maven pai quando aplicável.

**Armazenamento**: PostgreSQL, database lógico exclusivo `techstore_cart_db`; schema versionado por Flyway.

**Testes**: JUnit 5, Mockito, Spring Security Test, MockMvc e Testcontainers PostgreSQL 1.20.4.

**Plataforma Alvo**: Container Linux com Java 21; desenvolvimento via Maven e Docker Compose.

**Tipo de Projeto**: Microsserviço REST no monorepo Maven multi-módulo, integrado ao API Gateway.

**Metas de Desempenho**: Não definidas na spec; não se introduz SLA artificial nesta feature.

**Restrições**: Hexagonal; banco lógico isolado; JWT HS256 validado localmente com `TECHSTORE_JWT_SECRET` compartilhado; identidade somente do `sub`; chamadas REST síncronas com timeouts configuráveis e finitos; nenhuma chamada ao banco do Product Service; sem snapshots, estoque, checkout ou eventos.

**Escala/Scope**: Cinco operações HTTP (`GET` carrinho, `POST`/`PUT`/`DELETE` item e `DELETE` carrinho); sem UI. O catálogo atual consulta por UUID, portanto montar o GET implica uma chamada por linha até que medições justifiquem contrato batch.

## Constitution Check

**Gate antes da pesquisa**

| Princípio | Estado | Evidência/condição |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Cart Service possui bounded context e database lógico próprio; catálogo é acessado somente por API REST. |
| II. Arquitetura Hexagonal | PASS | Domínio e casos de uso dependem de portas; adapters encapsulam Spring, JPA, JWT e REST. |
| III. Clean Code/SOLID | PASS | Um agregado lógico simples, regras centralizadas nos casos de uso e sem cópia autoritativa de Product. |
| IV. Test-first | PASS | Regras, autorização, persistência, contrato e integração REST/Gateway terão testes automatizados. |
| V. Contratos de API | PASS | OpenAPI 3.1 versionado em `contracts/cart-service-api.openapi.yaml`; contrato do Product Service permanece inalterado. |
| VI. Segurança por padrão | PASS | JWT validado localmente, proprietário derivado de `sub`, requests sem `userId`, logs sem tokens/secrets. |
| VII. Observabilidade | PASS | Actuator, logs operacionais sanitizados e propagação de correlation ID. |
| VIII. Arquitetura frontend | N/A | Nenhum frontend incluído neste incremento. |
| IX. Reprodutibilidade | PASS | Maven, Flyway, Compose e configuração externalizada; database lógico próprio. |
| X. Spec-driven | PASS | Spec clarificada e artefatos de pesquisa/design produzidos antes de tasks/implementação. |

**Reavaliação pós-design**: PASS. Os artefatos mantêm os limites de contexto e dados aprovados; não há violação constitucional nem biblioteca externa adicional necessária.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/004-cart/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── cart-service-api.openapi.yaml
```

### Código-fonte planejado

```text
backend/
├── pom.xml
├── api-gateway/src/main/resources/application.yml
└── cart-service/
    ├── pom.xml
    ├── Dockerfile
    └── src/
        ├── main/java/com/techstore/cart/
        │   ├── domain/
        │   ├── application/port/in/
        │   ├── application/port/out/
        │   ├── application/service/
        │   └── adapter/
        │       ├── http/
        │       ├── persistence/
        │       ├── product/
        │       ├── security/
        │       └── config/
        ├── main/resources/
        │   ├── application.yml
        │   └── db/migration/
        └── test/java/com/techstore/cart/
            ├── domain/
            ├── application/
            └── adapter/

docker-compose.yml
docker/postgres/init-cart-database.sh
.env.example
```

**Decisão de estrutura**: novo módulo `backend/cart-service`, mantendo controller, portas, domínio e adapters separados. O Gateway encaminha `/api/cart/**` sem remover prefixo, pois o serviço expõe as rotas com `/api/cart`. Compose cria/configura `techstore_cart_db`, sem compartilhar database ou schema com os outros serviços.

## Complexidade

Sem violações constitucionais nem serviços auxiliares além do Cart Service. A restrição de não adicionar endpoint batch no Product Service mantém o escopo, com custo de N chamadas HTTP no GET explicitado como risco operacional.