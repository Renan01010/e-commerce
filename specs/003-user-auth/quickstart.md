# Quickstart: User Service e Autenticação

**Feature**: `003-user-auth`

## Pré-requisitos

- Java 21+ e Maven 3.9+.
- Docker ativo para Testcontainers e PostgreSQL local.
- Porta livre para Gateway (8080), Product Service (8081), User Service (8082) e PostgreSQL (5432).

## Configuração local

Copie `.env.example` para `.env` e defina localmente, sem versionar credenciais reais ou segredos:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/techstore_user_db
SPRING_DATASOURCE_USERNAME=techstore
SPRING_DATASOURCE_PASSWORD=<senha-local>
TECHSTORE_JWT_SECRET=<segredo-local-com-no-minimo-32-bytes>
USER_SERVICE_URL=http://localhost:8082
INITIAL_ADMIN_EMAIL=
INITIAL_ADMIN_PASSWORD=
```

O bootstrap é opcional: deixe ambas as variáveis `INITIAL_ADMIN_*` vazias para iniciar sem criar o ADMIN inicial. Definir apenas uma deve impedir a inicialização. Para testar bootstrap localmente, preencha ambas com valores de desenvolvimento, sem os commitar. O `docker compose up --build` cria idempotentemente `techstore_user_db` além de `techstore_product_db`; ambos usam a mesma instância PostgreSQL local, mas cada serviço recebe apenas o seu database lógico.

## Build e testes

Na raiz do monorepo:

```powershell
mvn -f backend/pom.xml -pl user-service -am test
mvn -f backend/pom.xml -pl user-service -am verify
```

`verify` executa os testes de integração com PostgreSQL via Testcontainers e requer Docker. Testes unitários podem ser executados sem iniciar o banco externo.

## Fluxo de validação HTTP

1. Execute `docker compose up --build`; o serviço `user-database-init` cria `techstore_user_db` somente se ausente. O User Service aplica migrations Flyway no próprio database.
2. Inicie User Service em `8082`, Gateway em `8080` e Product Service em `8081`. Configure no Gateway `USER_SERVICE_URL=http://localhost:8082` para execução local.
3. Registre usuário público:

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{"email":"user@example.test","password":"SenhaForte123"}
```

Esperado: `201 Created`, role `USER`, sem campos de senha/hash na resposta.

4. Faça login:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{"email":"user@example.test","password":"SenhaForte123"}
```

Esperado: `200 OK`, `accessToken`, `tokenType: Bearer` e `expiresIn: 86400`. O JWT contém `sub` UUID, `roles`, `iat` e `exp` e é assinado HS256.

5. Use o token ADMIN emitido pelo bootstrap para `POST /api/users` e `PUT /api/users/{id}`. Token inválido/ausente retorna 401; role USER retorna 403.
6. Confirme que credenciais inválidas, e-mail inexistente e conta inativa produzem resposta genérica equivalente; e-mail duplicado retorna 409.
7. Envie o JWT ADMIN a um endpoint administrativo do Product Service através do Gateway; o Product Service deve aceitá-lo usando o mesmo `TECHSTORE_JWT_SECRET` e a claim `roles`.

## Verificações de segurança

- Conferir `flyway_schema_history` no database `techstore_user_db` e verificar que a tabela de usuários existe apenas nesse database.
- Confirmar que resposta de registro/login, erros e logs não contêm senha, passwordHash, JWT completo ou segredo.
- Confirmar que `/api/auth/**` é encaminhado ao User Service removendo `/api`; as rotas `/api/products/**` permanecem no Product Service.
- Testar bootstrap com ambas as variáveis ausentes, ambas presentes, apenas uma presente e e-mail já existente.
