# Quickstart de Validação: Carrinho de Compras

## Pré-requisitos

- Java 21 e Maven instalados.
- Docker e Docker Compose disponíveis para testes Testcontainers e execução local.
- `.env` configurado conforme `.env.example`, com `POSTGRES_PASSWORD` e `TECHSTORE_JWT_SECRET` (mínimo de 32 bytes); não versionar secrets.
- Product Service disponível e um User Service compatível com o JWT do TechStore para emitir token. A branch base `master` não contém o módulo User Service; use um emissor local/deploy já existente e configure o mesmo `TECHSTORE_JWT_SECRET` no emissor, Product Service e Cart Service. Obter também um UUID de produto ativo do catálogo.

## Verificações automatizadas

Na raiz do repositório:

```bash
mvn -f backend/pom.xml -pl cart-service -am verify
```

Resultado esperado: build verde, testes unitários e de segurança aprovados, migrations aplicadas ao PostgreSQL Testcontainers e integração HTTP/Gateway aprovada. O teste de concorrência deve demonstrar uma única linha por usuário/produto e soma exata das quantidades.

## Execução local integrada

Subir PostgreSQL, inicializador do database do carrinho, Product Service, Cart Service e Gateway:

```bash
docker compose up --build -d postgres cart-database-init product-service cart-service api-gateway
```

O Cart Service deve iniciar Flyway no database lógico `techstore_cart_db`; o Gateway deve encaminhar `/api/cart/**` para `CART_SERVICE_URL`, preservando o path.

Defina `TOKEN` com JWT válido e `PRODUCT_ID` com UUID ativo. Consultar inicialmente deve retornar `200` e `items: []`:

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cart
```

Adicionar quantidade 2 deve retornar `201`; repetir com quantidade 3 deve retornar `200` e uma única linha com quantidade 5:

```bash
curl -i -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":2}"
curl -i -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":3}"
```

Consultar deve apresentar `available: true` e resumo atual (`name`, `price`, `brand`, `imageUrl`). Desativar o produto no Product Service e consultar novamente: a linha permanece, com `available: false` e `product: null`.

Atualizar quantidade para 4 deve retornar `200`. Remover item deve retornar `204`; repetir a remoção deve retornar `404`. `DELETE /api/cart` deve retornar `204`, inclusive quando vazio.

## Segurança e falhas

- Sem bearer token, token expirado ou `sub` que não seja UUID: `401`, sem alteração persistida.
- Dois tokens com `sub` distintos: cada um vê somente suas linhas; enviar `userId` no JSON é rejeitado e nunca muda o proprietário.
- Quantidade zero/negativa, UUID inválido ou produto inativo em POST/PUT: `400`/`404` conforme o contrato, sem gravação.
- Product Service com timeout ou indisponível durante validação/leitura: `503`; mutações rejeitadas durante validação não deixam gravação parcial.
- Falhas devem preservar correlation ID e não registrar Authorization, JWT ou secrets.

## Contrato

Ver [OpenAPI](contracts/cart-service-api.openapi.yaml) e [modelo de dados](data-model.md). Health do serviço: `GET /actuator/health` na porta local configurada para o Cart Service.