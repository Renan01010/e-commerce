# Pesquisa e Decisões: Carrinho de Compras

**Data**: 2026-09-27 | **Branch**: `004-cart`

## Contexto verificado

- O backend é um monorepo Maven; o pai define Java 21, Spring Boot 3.3.13, Spring Cloud 2023.0.5 e Testcontainers 1.20.4 em `backend/pom.xml`.
- Product Service e User Service já usam Web MVC, JPA, Resource Server, Flyway, PostgreSQL e Actuator. O Product Service usa JWT HS256 com `TECHSTORE_JWT_SECRET` e permite GET público em `/api/**`.
- O path interno do Product Service é `/api/products/{id}`: `ProductController` combina `@RequestMapping("/api/products")` com `@GetMapping("/{id}")`, sem `server.servlet.context-path`. A chamada privada direta é `PRODUCT_SERVICE_URL/api/products/{id}`; o endpoint retorna somente produtos ativos, com `name`, `price`, `brand`, `imageUrl` e `isActive`, e produto inexistente/inativo resulta em 404. Não há endpoint batch.
- O OpenAPI público do Product Service usa `servers.url` terminado em `/api` e o path relativo `/products/{id}`; isso resolve para `/api/products/{id}` pelo Gateway. A rota existente do Gateway preserva o path de produtos, sem `StripPrefix`. Para Cart, FR-005 define `/api/cart`; o OpenAPI do Cart compõe esse mesmo path por `servers.url` `/api` mais `/cart`, e T008 deve encaminhá-lo sem remover o prefixo.
- Compose usa um PostgreSQL compartilhado fisicamente com databases lógicos distintos para serviços; User Service tem inicializador idempotente de database e migrations Flyway.

## Decisões

### Serviço, dependências e arquitetura

**Decisão**: criar o módulo Maven `backend/cart-service`, alinhado ao User/Product Service: Spring Web, Data JPA, Resource Server, Validation, Actuator, Flyway, PostgreSQL e springdoc já usado no backend. Manter domínio/application sem dependência Spring e adapters para HTTP, segurança, Product Service e persistência.

**Rationale**: preserva o bounded context do carrinho e a arquitetura hexagonal obrigatória, sem dependências novas nem acesso a dados de outro serviço.

**Alternativas consideradas**: adicionar o carrinho ao Product Service (rejeitada por misturar bounded contexts e banco); criar módulo frontend ou biblioteca compartilhada (fora do escopo e desnecessário).

### Cliente REST do catálogo

**Decisão**: usar Spring `RestClient` síncrono diretamente contra `PRODUCT_SERVICE_URL/api/products/{id}`, sem passar pelo Gateway. Configurar connection/read timeout finitos e externalizados; traduzir timeout, falha de rede e resposta 5xx para indisponibilidade (503). Resposta 404 do catálogo significa produto ausente/inativo: em GET do carrinho representa `available=false`/`product=null`; em POST/PUT rejeita a mutação como 404. Não encaminhar o JWT do cliente: o GET do catálogo é público e a identidade do usuário não é necessária.

**Rationale**: segue o contrato de catálogo existente e a preferência constitucional por REST quando é necessária resposta imediata. O Product Service continua dono da disponibilidade e dos dados do produto.

**Alternativas consideradas**: consultar o banco do Product Service (proibido); persistir resumo/snapshot (contraria clarificação); adicionar endpoint batch (fora do escopo atual); `WebClient` (introduziria modelo reativo sem benefício estabelecido para este serviço MVC).

**Limitação conhecida**: o contrato só busca por UUID, logo GET carrinho faz uma consulta por linha. Não há meta de desempenho nem limite de itens definido pela spec. Timeouts evitam espera sem limite; observar latência e volume antes de propor batch ou paralelismo.

### Persistência e concorrência

**Decisão**: usar `techstore_cart_db` e tabela Flyway `cart_items`, com `owner_user_id UUID`, `product_id UUID`, `quantity INTEGER`, chave primária composta `(owner_user_id, product_id)` e `CHECK (quantity > 0)`. Não persistir uma linha de Cart vazia: zero itens para o proprietário representa carrinho vazio. Nenhuma FK cruza os databases de User/Product Service.

**Rationale**: o carrinho não tem outros atributos persistidos na spec; uma tabela de linhas é suficiente e a chave composta impõe unicidade por usuário.

**Decisão de concorrência**: adicionar via operação PostgreSQL atômica `INSERT ... ON CONFLICT ... DO UPDATE`, somando a quantidade existente. Isso evita duplicidade e perda de incrementos sem lock distribuído. Atualização substitui a quantidade; remoção/limpeza filtram pelo proprietário.

**Alternativas consideradas**: tabela Cart mais tabela CartItem com UUID de carrinho (rejeitada por adicionar entidade persistente sem atributos próprios); ler/modificar/salvar em JPA sem upsert/lock (rejeitada por race condition nas adições concorrentes).

### Identidade e Gateway

**Decisão**: configurar Resource Server JWT no Cart Service com HS256 e o segredo externalizado existente. Derivar `owner_user_id` exclusivamente do claim `sub`, validando UUID antes de cada caso de uso. Adicionar rota `CART_SERVICE_URL` no Gateway para `/api/cart/**`, preservando a URI. Criar database lógico dedicado no Compose.

**Rationale**: o gateway apenas encaminha; autorização e isolamento devem permanecer no serviço. Repete o padrão atual de chave e deployment sem compartilhar database.

**Alternativas consideradas**: confiar em `userId` enviado pelo cliente ou autenticação somente no Gateway (rejeitadas pela FR-002/FR-015); reutilizar database/schema de outro serviço (rejeitada pela Constituição).

### Testes e observabilidade

**Decisão**: testes unitários para regras e casos de uso; MockMvc/Spring Security para autenticação, validação e contrato; Testcontainers para Flyway, constraints e upsert concorrente; servidor HTTP stub para Product Service e Gateway; Actuator health/metrics, correlation ID e logs sem Authorization/token/secret.

**Rationale**: cobre regras de domínio, fronteiras externas, persistência real e integração do caminho público sem depender de catálogo real nos testes de falha.

**Alternativas consideradas**: somente testes unitários (insuficientes para SQL atômico/migrations); somente smoke manual (não reprodutível).

## Pendências

Nenhuma clarificação bloqueante para planejamento. Valores de timeout devem ser externalizados e receber defaults finitos na implementação; a spec não define SLA. O comportamento fica observável e os defaults podem ser ajustados sem alterar contrato.