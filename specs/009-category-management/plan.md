# Plano de Implementação: Gerenciamento de Categorias

**Branch**: `009-category-management` | **Data**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Entrada**: Especificação clarificada em `spec.md`; decisões de slug histórico e PATCH sem body foram incorporadas.

## Resumo

Completar o gerenciamento existente de categorias no Product Service, adicionando slug canônico globalmente único, busca e reativação, além de permitir leitura administrativa das categorias inativas. O plano estende a entidade, o repositório, a API/OpenAPI e a tabela `categories` existentes; preserva leitura pública active-only, JWT ADMIN, relações com produtos e a regra atual que impede desativação enquanto houver produtos vinculados.

A migration V2 será uma migration Java do Flyway para calcular a mesma canonicalização de slug usada em criação e edição. Ela valida todas as linhas e o mapa externo opcional antes de persistir qualquer slug ou constraint. Colisões sem mapa completo causam falha transacional com diagnóstico de `categoryId`, nome e slug candidato; não são resolvidas por sufixos automáticos.

## Contexto Técnico

**Linguagem/Versão**: Java 21; o parent define `maven.compiler.release=21`.

**Dependências Primárias**: Spring Boot 3.3.13, Spring Web MVC, Spring Data JPA, Spring Security Resource Server/JWT, Bean Validation, Flyway, PostgreSQL JDBC, Springdoc 2.6.0, JUnit 5, Mockito e Testcontainers 1.20.4 já usados pelo Product Service.

**Armazenamento**: Database PostgreSQL existente do Product Service; evolução aditiva da tabela `categories`. Nenhum database ou tabela nova.

**Testes**: Maven, JUnit 5/Mockito para domínio/casos de uso, MockMvc para auth/REST/OpenAPI e Testcontainers PostgreSQL para migration, constraints e referências.

**Plataforma Alvo**: Product Service Spring Boot em Java 21, executado localmente ou em container Docker; API pública segue acessível pelo Gateway existente.

**Tipo de Projeto**: Microsserviço Spring Boot existente em monorepo Maven multi-módulo.

**Metas de Desempenho**: A spec não define SLA nem volume alvo. Pesquisa de categoria deve continuar responsiva para o catálogo existente; não introduzir paginação ou estratégia de cache sem requisito/medição.

**Restrições**: Um único Product Service e contrato `/api/categories`; reads públicos retornam active-only; incluir inativas e todas as escritas exigem ADMIN; slugs únicos também em inativas; IDs, nomes, parent, produto e FKs preservados; sem produtos/checkout/estoque novos; mapa de colisões é configuração operacional externa, não dado secreto nem tabela.

**Escala/Âmbito**: Evoluir Category, CategoryService/Repository/adapters, uma migration Flyway, endpoints existentes, endpoint de reativação, configuração externalizada do placeholder, OpenAPI de 001 e testes do Product Service.

## Verificação da Constituição

*Gate antes da pesquisa e reavaliação após o design.*

| Princípio | Estado | Evidência |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Categoria permanece no bounded context Product Service e em seu database. |
| II. Arquitetura Hexagonal | PASS | Regras seguem no domínio/aplicação; JPA, REST, Flyway e segurança são adapters existentes. |
| III. Clean Code/SOLID | PASS | A feature amplia CategoryService/CategoryRepository; não replica serviço ou CRUD. |
| IV. Test-first | PASS | Testes unitários, MockMvc e integração PostgreSQL cobrem novas regras e migration. |
| V. API e contratos | PASS | Atualiza o OpenAPI já versionado de 001 sem contrato concorrente. |
| VI. Segurança por padrão | PASS | Escritas e dados inativos exigem ADMIN; ator vem do JWT e não do request. |
| VII. Observabilidade | PASS | Reutiliza ErrorResponse, correlation ID e logs existentes sem segredos. |
| VIII. Arquitetura frontend | N/A | Escopo desta feature é backend/API/schema/docs; UI administrativa não está solicitada. |
| IX. Infraestrutura e reprodutibilidade | PASS | Flyway, Postgres, Docker Compose e configuração externalizada já existem. |
| X. Spec-driven development | PASS | Spec clarificada precede plano; tasks e implementação ficam para próximas fases. |

**Resultado antes da pesquisa**: PASS; não há serviço ou schema paralelo.

**Resultado após o design**: PASS; a migration é aditiva e aborta antes de alterações se qualquer slug ou mapa não puder garantir unicidade.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/009-category-management/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── category-management-api.md
└── tasks.md             # criado por /speckit.tasks
```

### Código-fonte existente a evoluir

```text
backend/product-service/
├── src/main/java/com/techstore/product/
│   ├── domain/Category.java
│   ├── domain/CategorySlug.java       # canonicalização reutilizada no serviço e migration
│   ├── application/port/CategoryRepository.java
│   ├── application/service/CategoryService.java
│   └── adapter/
│       ├── http/CategoryController.java
│       ├── http/ApiModels.java
│       ├── http/ApiExceptionHandler.java
│       ├── config/SecurityConfig.java
│       └── persistence/
│           ├── CategoryEntity.java
│           ├── CategoryJpaRepository.java
│           └── CategoryPersistenceAdapter.java
├── src/main/java/db/migration/V2__AddCategorySlug.java
├── src/main/resources/application.yml
├── src/main/resources/db/migration/V1__create_catalog_schema.sql
└── src/test/java/com/techstore/product/
    ├── domain/CategorySlugTest.java
    ├── application/service/CategoryServiceTest.java
    ├── CatalogApiIntegrationTest.java
    └── migration/CategorySlugMigrationTest.java

specs/001-foundation-product-catalog/contracts/product-service-api.openapi.yaml
docker-compose.yml
```

**Decisão de estrutura**: alterar apenas o serviço e o contrato que já possuem Categoria. `Category` mantém as regras de entidade; `CategoryService` faz validação de nome/slug/status e auditoria; porta/repositório consultam e persistem slug; `CategoryController` mantém as rotas atuais e acrescenta o PATCH de reativação; `ApiModels` atualiza DTOs; a migration Java é descoberta no local padrão `classpath:db/migration`; SecurityConfig mantém reads públicos e protege dados inativos; OpenAPI de 001 permanece fonte única.

## Procedimento Operacional da Migration

1. Manter `V1__create_catalog_schema.sql` inalterada. Antes do primeiro deploy que contém `V2__AddCategorySlug`, fazer backup do database e iniciar o Product Service com `CATEGORY_SLUG_OVERRIDES_JSON` vazio/ausente.
2. A migration lê categorias ativas e inativas, calcula candidatos e identifica todos os grupos colidentes antes de executar DDL/DML. Sem colisões, aplica todos os slugs e `NOT NULL`/unique em uma transação.
3. Com colisões, o Product Service não termina startup; o diagnóstico lista ID, nome e slug candidato. A transação PostgreSQL/Flyway reverte, não existe coluna/constraint parcial e o serviço não fica parcialmente atualizado.
4. Responsável pelo catálogo escolhe explicitamente um slug canônico único para cada ID de todos os grupos reportados. O mapa não contém segredos e é fornecido por configuração de deployment, nunca hardcoded na imagem ou migration compartilhada.
5. Disponibilizar `CATEGORY_SLUG_OVERRIDES_JSON` como configuração externa e mapeá-la em `application.yml` para `spring.flyway.placeholders.categorySlugOverrides` (valor padrão vazio). A migration Java V2 lê `categorySlugOverrides` da configuração Flyway. Reiniciar/reexecutar o Product Service; a migration valida cobertura total, canonicalização, IDs, unicidade global (incluindo inativas) e conflitos dos slugs não substituídos antes de gravar.
6. Se o mapa estiver ausente/incompleto, apontar ID desconhecido, valor inválido ou ainda colidir, abortar novamente sem escrita e reportar os conflitos restantes. Nunca adicionar sufixos nem alterar nome/ID/hierarquia/produto.
7. Após sucesso, conferir contagem de categorias, slugs, constraints, `parent_category_id` e `products.category_id`; retirar `CATEGORY_SLUG_OVERRIDES_JSON` da configuração externa. Flyway registra V2 aplicada e os slugs ficam persistidos.

## Sequência de Implementação

1. Formalizar a canonicalização de slug no domínio, incluindo transliteração, separadores, limites e validação de slug explícito.
2. Evoluir porta/repositório para verificar unicidade global de slug e nome segundo a regra atual; assegurar consulta/ordenação de categorias ativas/inativas e busca por nome/descrição/slug.
3. Implementar V2 Java como preflight e backfill transacional; ler mapa Flyway externalizado e garantir falha antes da primeira escrita quando existir colisão sem resolução explícita.
4. Evoluir entidade e adapter da tabela `categories` com slug obrigatório/único sem alterar chaves ou relações existentes; validar Hibernate contra schema migrado.
5. Evoluir CategoryService para create/update/search/visibilidade/reativação, preservando auditoria, pai ativo na criação, soft delete e `409` se produtos impedirem desativação.
6. Evoluir CategoryController/ApiModels com busca e includeInactive condicional ADMIN, resposta slug e `PATCH /api/categories/{id}/activation` sem body; preservar rotas e payloads compatíveis.
7. Atualizar segurança para proteger leituras administrativas de inativas, mantendo listagem pública active-only; mapear 400/401/403/404/409 no ErrorResponse existente.
8. Atualizar Springdoc e `specs/001-foundation-product-catalog/contracts/product-service-api.openapi.yaml`; acrescentar testes de domínio, serviço, controller/security, migration e integração do catálogo.
9. Executar os gates Maven e o quickstart, incluindo cenário de colisão, rollback e reaplicação com mapa explícito.

## Riscos e Controles

- **Colisão em dados de produção**: startup do Product Service falha até fornecer mapa completo. Mitigar com backup, relatório diagnóstico e configuração temporária aprovada por ID.
- **Mapa incompleto ou conflitante**: migration revalida o conjunto inteiro antes de gravar e falha sem persistência parcial.
- **Slug de inativo reutilizado**: constraint/consulta de unicidade considera todas as linhas, não apenas ativas.
- **Leak de categorias inativas por GET público**: `includeInactive=true` exige ADMIN; default público mantém active-only.
- **Referências existentes**: migration não altera UUID, nome, parent nem FK de produto; testar com dados pré-carregados no PostgreSQL Testcontainers.
- **Regressão do catálogo público**: preservar rota/DTO existente e cobrir listagem ativa, pesquisa e acesso público em `CatalogApiIntegrationTest`.

## Rastreamento de Complexidade

Nenhuma violação constitucional nem dependência ou serviço novo. A migration Java e o mapa operacional são necessários para detectar e resolver slugs históricos sem sufixos silenciosos nem tabela de resolução permanente.
