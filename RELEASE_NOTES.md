# Release Notes: Foundation Product Catalog

## Incluído

- Product Service com domínios Product e Category, operações REST de leitura e administração, busca textual, filtros, ordenação e paginação.
- Persistência PostgreSQL com Flyway, restrições de integridade e índice GIN para busca full-text.
- API Gateway com roteamento para o catálogo e propagação de `X-Correlation-ID`.
- Autorização JWT para operações administrativas, documentação OpenAPI e logs JSON.
- Frontend React para catálogo, pesquisa, filtros, navegação por categorias e detalhes do produto.
- Docker Compose para desenvolvimento local e testes JUnit 5, Mockito, Testcontainers, Vitest e React Testing Library.

## Contratos

O contrato REST aprovado está em `specs/001-foundation-product-catalog/contracts/product-service-api.openapi.yaml`. Não há alteração incompatível intencional em relação a esse contrato.

## Limitações conhecidas

- A emissão de JWT pertence a um serviço de identidade externo e não está incluída neste incremento.
- “Adicionar ao carrinho” é apenas um placeholder; não há persistência de carrinho.
- O Compose da raiz e o servidor Vite são destinados a desenvolvimento local.
- Os testes Testcontainers exigem Docker; a cobertura JaCoCo deve ser executada em ambiente com Maven.