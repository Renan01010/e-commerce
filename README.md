# TechStore Product Catalog

Catálogo de produtos em arquitetura hexagonal: React/TypeScript consome a API REST pelo API Gateway; o Product Service é dono do PostgreSQL.

```text
React + TypeScript + Vite -> API Gateway -> Product Service -> PostgreSQL
```

## Desenvolvimento local

Requisitos: Java 21+, Maven 3.9+, Node.js 20+, npm 10+ e Docker Compose.

1. Copie `.env.example` para `.env` e defina uma senha local para o PostgreSQL e uma chave JWT de pelo menos 32 bytes.
2. Execute `docker compose up --build` na raiz.
3. Acesse o catálogo em `http://localhost:5173`, o Gateway em `http://localhost:8080` e a documentação OpenAPI em `http://localhost:8081/swagger-ui.html`.

As migrações Flyway criam o schema automaticamente. O frontend usa `VITE_API_URL` e chama somente as rotas `/api/**` do Gateway. Operações administrativas exigem JWT com claim `roles` contendo `ADMIN`; a emissão de tokens pertence ao serviço de identidade e não faz parte desta feature.

## Testes

- Backend: `mvn -f backend/pom.xml test`
- Integração PostgreSQL: `mvn -f backend/product-service/pom.xml verify` (requer Docker para Testcontainers)
- Frontend: `cd frontend && npm install && npm test`

O modelo de domínio Java não depende de Spring ou JPA. Portas em `application/port` são implementadas pelos adaptadores em `adapter/persistence`; os endpoints e schemas aprovados estão em `specs/001-foundation-product-catalog/contracts/`.