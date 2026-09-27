# Deployment

## Requisitos

- Java 21 e Maven 3.9+ para execução fora de containers.
- Docker Engine e Docker Compose para o ambiente local.
- PostgreSQL 15+ para o Product Service.
- Uma chave JWT HS256 com pelo menos 32 bytes, compartilhada com o emissor de tokens.

## Configuração

Configure as variáveis do ambiente de execução; não reutilize os valores de desenvolvimento:

- `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`
- `TECHSTORE_JWT_SECRET`
- `PRODUCT_SERVICE_URL` no API Gateway
- `CORS_ALLOWED_ORIGINS` com as origens exatas do frontend
- `SERVER_PORT` para cada serviço

Não publique secrets em arquivos versionados, imagens de container ou logs. Em produção, injete-os por um secret manager. Termine TLS no ingress ou load balancer e exponha publicamente apenas o API Gateway; mantenha PostgreSQL e Product Service em uma rede privada.

## Banco e migrações

O Product Service executa Flyway na inicialização e valida o schema com Hibernate (`ddl-auto: validate`). Faça backup do PostgreSQL antes de promover uma nova migração. O banco pertence exclusivamente ao Product Service.

## Serviços e health checks

Construa os módulos com `mvn -f backend/pom.xml clean package`. As imagens Docker usam Java 21 e podem ser construídas pelo `docker compose up --build` para desenvolvimento. Em produção, publique imagens versionadas e configure réplicas e recursos pelo orquestrador adotado.

- API Gateway: `/actuator/health`
- Product Service: `/actuator/health` e `/actuator/metrics`
- OpenAPI: `/swagger-ui.html` no Product Service

O Compose da raiz é destinado ao desenvolvimento local: ele publica portas de desenvolvimento e inicia o Vite. Não o exponha diretamente à internet como deployment de produção.

## Railway (monorepo)

Railpack tentou detectar um único aplicativo na raiz do repositório, mas o TechStore é composto por serviços independentes. O `Dockerfile` raiz agora cria o API Gateway, resolvendo o build inicial da raiz. Para publicar o sistema completo, crie serviços separados no mesmo projeto Railway:

1. **API Gateway**: builder Dockerfile, caminho `Dockerfile` (ou `backend/api-gateway/Dockerfile`), contexto na raiz. Gere um domínio público para este serviço.
2. **Product Service**: builder Dockerfile, caminho `backend/product-service/Dockerfile`, contexto na raiz. Configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` usando as referências do PostgreSQL Railway, além de `TECHSTORE_JWT_SECRET`.
3. **PostgreSQL**: adicione o serviço PostgreSQL gerenciado ao projeto; não exponha a porta do banco publicamente.
4. **Frontend**: builder Dockerfile, caminho `frontend/Dockerfile`, contexto na raiz. Configure `VITE_API_URL` com a URL pública do Gateway terminada em `/api` antes do build.

No Gateway, configure `PRODUCT_SERVICE_URL` como a URL privada do Product Service, usando o domínio e a porta interna desse serviço, e compartilhe o mesmo `TECHSTORE_JWT_SECRET` com o Product Service. Em ambos os serviços Java configure `CORS_ALLOWED_ORIGINS` com a origem pública exata do frontend. Configure health check do Gateway em `/actuator/health` e do frontend em `/health`.

Não defina o root directory como `backend/api-gateway` ou `backend/product-service`: os Dockerfiles Maven precisam do contexto na raiz para copiar o POM pai `backend/pom.xml`. Para publicar somente a interface via Railpack, configure o root directory como `frontend`; para o fluxo Docker documentado acima, selecione explicitamente o Dockerfile `frontend/Dockerfile`.

## Diagnóstico

- Falha de conexão com banco: verifique DNS, porta, credenciais e health check do PostgreSQL.
- Migração inválida: consulte os logs do Product Service e a tabela `flyway_schema_history`; não altere o schema manualmente para contornar a migração.
- 401 em operações administrativas: confira assinatura, expiração e claim `roles` do JWT.
- 403 em operações administrativas: confirme que o token contém `ADMIN`.
- Problemas de CORS: configure a origem do navegador em `CORS_ALLOWED_ORIGINS` no Gateway e no Product Service.
- Use `X-Correlation-ID` para relacionar a requisição nos logs dos serviços.