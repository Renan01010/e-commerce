# TechStore Product Catalog

Monorepo TechStore com serviços Spring Boot independentes e frontend React. O frontend consome APIs REST através do API Gateway; Product Service e User Service possuem databases PostgreSQL lógicos separados.

```text
React + TypeScript + Vite -> API Gateway -> Product Service -> PostgreSQL (techstore_product_db)
									  \-> User Service    -> PostgreSQL (techstore_user_db)
```

## Desenvolvimento local

Requisitos: Java 21+, Maven 3.9+, Node.js 20+, npm 10+ e Docker Compose.

1. Copie `.env.example` para `.env` e substitua os placeholders por credenciais locais. Configure uma senha PostgreSQL e `TECHSTORE_JWT_SECRET` com pelo menos 32 bytes; `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD` podem permanecer vazias para desativar o bootstrap local.
2. Execute `docker compose up --build` na raiz.
3. Acesse o catálogo em `http://localhost:5173`, o Gateway em `http://localhost:8080`, Product Service em `http://localhost:8081` e User Service em `http://localhost:8082`.

As migrations Flyway são executadas por cada serviço no seu database lógico. O Compose cria `techstore_user_db` separadamente e conecta o User Service a ele; os serviços compartilham a instância física local, não as tabelas nem o database lógico. O frontend usa `VITE_API_URL` e chama somente as rotas `/api/**` do Gateway. Login e cadastro ficam em `/api/auth/**`; administração de usuários e operações administrativas do catálogo exigem JWT com claim `roles` contendo `ADMIN`.

## Testes

- Backend: `mvn -f backend/pom.xml test`
- Integração User Service: `mvn -f backend/pom.xml -pl user-service -am verify` (requer Docker para Testcontainers)
- Integração Product Service: `mvn -f backend/product-service/pom.xml verify` (requer Docker para Testcontainers)
- Frontend: `cd frontend && npm install && npm test`

O modelo de domínio Java não depende de Spring ou JPA. Portas em `application/port` são implementadas pelos adaptadores em `adapter/persistence`; os endpoints e schemas aprovados estão em `specs/001-foundation-product-catalog/contracts/`.