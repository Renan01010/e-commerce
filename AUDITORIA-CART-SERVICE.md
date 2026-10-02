# Auditoria Técnica do Cart Service

## Escopo e estado

Auditoria somente leitura do código presente na branch `004-cart`, alinhada a `origin/master` no momento da análise. Nenhum arquivo foi alterado durante a auditoria; este documento consolida os achados. A análise se baseia no código, não em requisitos presumidos de documentação.

**Conclusão resumida:** o fluxo principal de carrinho está implementado, com autenticação JWT, isolamento por usuário, persistência de itens, consolidação concorrente e consulta ao catálogo. A arquitetura é parcialmente hexagonal. Estoque, limite máximo de quantidade, preço persistido, subtotal e total não estão implementados. A consistência do schema implantado e dos dados reais não pôde ser verificada sem acesso ao banco.

## Estrutura encontrada

```text
backend/cart-service/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/techstore/cart/
    │   │   ├── CartServiceApplication.java
    │   │   ├── domain/
    │   │   ├── application/
    │   │   │   ├── exception/
    │   │   │   ├── model/
    │   │   │   ├── port/out/
    │   │   │   └── service/
    │   │   └── adapter/
    │   │       ├── config/
    │   │       ├── http/
    │   │       ├── persistence/
    │   │       ├── product/
    │   │       └── security/
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    └── test/java/com/techstore/cart/
```

### Arquitetura

- **IMPLEMENTADO:** domínio (`CartItem`), serviços de aplicação, portas de saída `CartStorePort` e `ProductCatalogPort`, e adapters HTTP, persistência, catálogo e segurança.
- **PARCIALMENTE IMPLEMENTADO:** arquitetura hexagonal. Controllers são adapters de entrada, mas chamam serviços concretos diretamente. Não existe pacote `application.port.in` nem interfaces de casos de uso.
- **PARCIALMENTE IMPLEMENTADO:** há `CartItem` no domínio e `CartView` como modelo de aplicação/resposta, mas não há aggregate persistido `Cart`.
- **AUSENTE:** mapper dedicado. As conversões estão em métodos de entidade e controllers.
- **IMPLEMENTADO:** módulo Maven Spring Boot com Web MVC, Data JPA, Security, OAuth2 Resource Server, validação, Actuator, Flyway, PostgreSQL e Springdoc (`backend/cart-service/pom.xml`).

Diagrama de componentes e fluxo:

```mermaid
flowchart TD
    Browser[Cliente] --> Gateway[API Gateway<br/>rota /api/cart/**]
    Gateway -->|encaminha /api/cart/**| CartHTTP[Cart Service<br/>controllers HTTP]
    CartHTTP --> Owner[CartOwnerResolver<br/>sub do JWT como UUID]
    Owner --> UseCases[Serviços de aplicação]
    UseCases --> CartPort[CartStorePort]
    CartPort --> Persistence[CartPersistenceAdapter<br/>JPA + JDBC]
    Persistence --> DB[(PostgreSQL<br/>cart_items)]
    UseCases --> CatalogPort[ProductCatalogPort]
    CatalogPort --> ProductAdapter[ProductCatalogHttpAdapter]
    ProductAdapter -->|GET /api/products/{id}| Product[Product Service]
    User[User Service] -. emite JWT HS256<br/>sub = UUID do usuário .-> Owner
```

## Persistência e banco de dados

Existe uma migration: `backend/cart-service/src/main/resources/db/migration/V1__create_cart_items.sql`.

| Elemento | Definição |
|---|---|
| Tabela | `cart_items` |
| `owner_user_id` | `UUID NOT NULL` |
| `product_id` | `UUID NOT NULL` |
| `quantity` | `INTEGER NOT NULL` |
| Chave primária | `(owner_user_id, product_id)` |
| Constraint | `CHECK (quantity > 0)` |

- **IMPLEMENTADO:** a chave composta permite no máximo uma linha por usuário/produto. A PK também cria o índice único implícito; sua primeira coluna atende consultas por proprietário.
- **IMPLEMENTADO:** JPA usa `@EmbeddedId`; os campos e nomes de coluna correspondem estaticamente à migration. `spring.jpa.hibernate.ddl-auto` está em `validate`.
- **IMPLEMENTADO:** `CartPersistenceAdapter` usa PostgreSQL `INSERT ... ON CONFLICT ... DO UPDATE` para somar quantidades. Há teste de concorrência para esse comportamento.
- **AUSENTE:** FKs para usuário/produto, timestamps, preço, subtotal, total e índices explícitos além da PK. IDs de usuário e produto são referências a outros serviços.
- **PARCIALMENTE IMPLEMENTADO:** quantidade positiva é validada no domínio, aplicação, request e banco; não há limite superior.
- **SEM EVIDÊNCIA ESTÁTICA DE CONFLITO:** só existe a migration V1 e não foi encontrada evidência no histórico local de edição posterior da migration.
- **BLOQUEADO:** o PostgreSQL implantado não foi acessado. Não é possível verificar checksum efetivo do Flyway, schema real, constraints instaladas ou integridade dos dados de produção.

## Domínio e regras de negócio

| Regra | Estado | Comportamento observado |
|---|---|---|
| Quantidade mínima positiva | **IMPLEMENTADO** | `@Positive`, validação no serviço/domínio e `CHECK (quantity > 0)`. |
| Quantidade máxima | **AUSENTE** | Não há limite de negócio ou constraint superior; uma soma pode exceder o intervalo de `INTEGER`. |
| Produto ativo ao adicionar | **IMPLEMENTADO** | Consulta o catálogo antes de persistir uma linha nova. |
| Produto ativo ao atualizar | **IMPLEMENTADO** | Uma linha associada a produto inativo não pode ter quantidade alterada. |
| Produto inativo já no carrinho | **IMPLEMENTADO** | Leitura retorna `available: false` e `product: null`; o item não é removido automaticamente. |
| Estoque disponível | **AUSENTE** | A resposta do produto inclui `quantity`, mas o adapter não usa esse campo nem valida estoque. |
| Produto repetido | **IMPLEMENTADO** | Novo POST soma à quantidade existente; a PK composta mantém uma linha. |
| Atualização de quantidade | **IMPLEMENTADO** | PUT substitui a quantidade existente, em vez de somá-la. |
| Remover item | **IMPLEMENTADO** | Restrito ao proprietário; item ausente resulta em 404. Não precisa consultar o catálogo. |
| Limpar carrinho | **IMPLEMENTADO** | Remove todas as linhas do proprietário; carrinho já vazio continua sendo sucesso. |
| Carrinho vazio | **IMPLEMENTADO** | GET retorna `items: []`. |
| Preço | **PARCIALMENTE IMPLEMENTADO** | Preço atual do Product Service é apresentado; não é gravado como snapshot. |
| Subtotal por linha | **AUSENTE** | Não existe campo nem cálculo. |
| Total do carrinho | **AUSENTE** | Não existe campo nem caso de uso de cálculo. |

O preço apresentado é o preço atual do catálogo no momento da leitura, não necessariamente o preço vigente quando o item entrou no carrinho.

## Casos de uso

| Caso de uso | Estado | Dependências e cobertura observada |
|---|---|---|
| `GetCartService` | **IMPLEMENTADO** | Lê persistência e monta a visão. Depende do catálogo para carrinhos com itens. Teste dedicado. |
| `AddCartItemService` | **IMPLEMENTADO** | Valida quantidade/produto ativo e soma quantidade da linha existente. Depende de persistência e catálogo. Testes para produto ativo, inativo e catálogo indisponível. |
| `SetCartItemQuantityService` | **IMPLEMENTADO** | Valida quantidade, propriedade da linha e disponibilidade. Depende de persistência e catálogo. Teste dedicado. |
| `RemoveCartItemService` | **IMPLEMENTADO** | Remove item do proprietário; item inexistente gera exceção 404. Teste dedicado. |
| `ClearCartService` | **IMPLEMENTADO** | Remove todas as linhas do proprietário. Não depende do catálogo. Teste dedicado. |
| `CartViewAssembler` | **IMPLEMENTADO** | Adiciona resumo atual ou indica produto indisponível. Testa sucesso, inativo e falha do catálogo. |
| `CalculateCartTotal` | **AUSENTE** | Não há classe, serviço ou cálculo de total. |

**Risco de integração/performance:** a montagem do carrinho consulta Product Service individualmente para cada item. A leitura pode gerar uma chamada HTTP por linha; add e update também montam a visão antes da escrita. Se qualquer consulta falhar, a operação pode falhar com 503.

## API REST

As rotas de negócio exigem JWT válido. `CartOwnerResolver` exige `JwtAuthenticationToken` e interpreta o claim `sub` como UUID. A segurança não exige role específica nessas operações: exige autenticação e, depois, usa o proprietário do token para isolar os dados.

| Método e caminho | Request | Response/status | Validações e erros |
|---|---|---|---|
| `GET /api/cart` | Sem body | `200` com `{ "items": [...] }`; `401` sem JWT; `503` se falhar consulta ao catálogo | Cada linha é enriquecida com produto atual; produto inativo fica indisponível. |
| `DELETE /api/cart` | Sem body | `204`; `401` sem JWT | Limpa todas as linhas do proprietário. |
| `POST /api/cart/items` | `{ "productId": UUID, "quantity": inteiro }` | `201` nova linha; `200` linha consolidada; também `400`, `401`, `404` produto ausente/inativo, `503` catálogo | `productId` obrigatório; quantidade positiva; campos JSON desconhecidos são rejeitados. |
| `PUT /api/cart/items/{productId}` | `{ "quantity": inteiro }` | `200` com carrinho atualizado; também `400`, `401`, `404` item/produto ausente ou inativo, `503` catálogo | UUID no path e quantidade positiva; substitui a quantidade. |
| `DELETE /api/cart/items/{productId}` | Sem body | `204`; `401` sem JWT; `404` item inexistente para o proprietário | Não consulta Product Service. |

As linhas de resposta contêm `productId`, `quantity`, `available` e, quando disponível, `product` com `name`, `price`, `brand` e `imageUrl`. Não contêm subtotal nem total. O `CartExceptionHandler` mapeia erros de entrada a 400, ausência a 404, indisponibilidade do catálogo a 503 e proprietário inválido a 401; exceções inesperadas de persistência não têm mapeamento específico.

Também há endpoints/framework configurados: Actuator expõe `health`, `metrics` e `info`; GET de health/metrics e rotas Springdoc/Swagger aparecem como permitidos no SecurityConfig. `info` não consta nos matchers públicos e cai na regra geral de autenticação.

## Segurança e integrações

- **JWT:** `TECHSTORE_JWT_SECRET` é obrigatório, precisa ter ao menos 32 bytes e é usado com HS256.
- **User Service:** não existe chamada HTTP. O User Service emite JWT com `sub = user.id().toString()` e roles; o Cart usa o `sub` e não usa role para autorizar as operações de carrinho.
- **Product Service:** `PRODUCT_SERVICE_URL`, padrão `http://localhost:8081`; connect timeout padrão `1s`; read timeout `2s`. O adapter chama `GET /api/products/{id}` e lê `name`, `price`, `brand`, `imageUrl` e `isActive`. Não usa o estoque retornado.
- **Erros do catálogo:** resposta 404 é tratada como produto ausente/inativo; demais falhas do cliente REST são convertidas em indisponibilidade e expostas como 503.
- **Gateway:** a rota usa `CART_SERVICE_URL`, padrão `http://localhost:8083`, com `Path=/api/cart/**`; o prefixo não é removido.
- **CORS:** não há configuração CORS própria no Cart Service. O CORS externo é configurado no Gateway.
- **Variáveis de ambiente:** `PORT`/`SERVER_PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `TECHSTORE_JWT_SECRET`, `PRODUCT_SERVICE_URL`, `PRODUCT_SERVICE_CONNECT_TIMEOUT` e `PRODUCT_SERVICE_READ_TIMEOUT`.
- **Container:** `backend/cart-service/Dockerfile` compila com Maven/Java 21, pula testes durante a criação da imagem, executa em Java 21 JRE e expõe `8083`.

## Testes presentes

Há 17 classes de teste:

- Domínio: `CartItemTest`.
- Aplicação: `AddCartItemServiceTest`, `SetCartItemQuantityServiceTest`, `GetCartServiceTest`, `RemoveCartItemServiceTest`, `ClearCartServiceTest`, `CartViewAssemblerTest`.
- HTTP: `CartControllerTest`, `CartQueryControllerTest`, `CartItemControllerTest`, `CartExceptionHandlerTest`, `CartOpenApiContractTest`.
- Integrações/adapters: `CartPersistenceAdapterTest`, `CartConcurrentAddIntegrationTest`, `ProductCatalogHttpAdapterTest`, `CartSecurityIntegrationTest`, `CartObservabilityTest`.

Cobrem autenticação, validação básica, status HTTP, isolamento por proprietário, upsert concorrente, persistência, resposta do catálogo, produto ativo/inativo e falhas/timeout. Testes de PostgreSQL usam Testcontainers e dependem de Docker. **Os testes não foram executados nesta auditoria.**

## Classificação final

- **IMPLEMENTADO:** CRUD de linhas do carrinho, associação por proprietário, autenticação JWT, quantidade positiva, consolidação atômica, consulta ao catálogo e mapeamento de erros de catálogo.
- **PARCIALMENTE IMPLEMENTADO:** arquitetura hexagonal, modelo de domínio agregado, dados de preço e visão do carrinho.
- **AUSENTE:** limite máximo de quantidade, verificação de estoque, snapshots de preço, subtotais, total e FKs entre microsserviços.
- **BLOQUEADO:** verificação do banco real de produção, checksum registrado do Flyway e integridade dos dados implantados.
- **INCONSISTÊNCIA ESTÁTICA NÃO IDENTIFICADA:** a migration V1, entidade e chave composta parecem corresponder no código revisado.
