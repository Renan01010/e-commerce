# Contrato de Consumo do Carrinho pelo Frontend

Este documento descreve o consumo frontend das rotas existentes através do API Gateway. Não cria nem altera contrato/backend. A referência funcional aprovada para a feature são os DTOs/controllers atuais do Cart Service; o OpenAPI versionado em `specs/004-cart/contracts/cart-service-api.openapi.yaml` está desatualizado.

## Operações

| Método e rota | Request | Sucesso | Comportamento frontend |
|---|---|---|---|
| `GET /api/cart` | JWT existente | `200 CartResponse` | Carregar/reconciliar estado e resumo. |
| `POST /api/cart/items` | `{ productId, quantity }` | `201 CartResponse` nova linha; `200 CartResponse` consolidação | Substituir store pela resposta; não somar quantidade localmente. |
| `PUT /api/cart/items/{productId}` | `{ quantity }` | `200 CartResponse` | Substituir store pela resposta. |
| `DELETE /api/cart/items/{productId}` | JWT e UUID no path | `204`, sem corpo | Após confirmação, remover linha local e fazer GET para resumo oficial. |
| `DELETE /api/cart` | JWT existente | `204`, sem corpo | Após confirmação, limpar linhas e fazer GET para resumo oficial. |

## Resposta de Carrinho

`CartResponse` contém `items`, `maxItemQuantity`, `total` e `totalAvailable`.

Cada item contém `productId`, `quantity`, `available`, `product`, `unitPriceSnapshot`, `priceAvailable` e `subtotal`. `product` pode ser nulo; quando existe, inclui `name`, `price`, `brand` e `imageUrl`. Os valores financeiros por linha e agregado são fornecidos pelo serviço e não podem ser reconstruídos no frontend.

## Erros Observados

| Operação | Status documentados/emitidos no Cart Service | Tratamento frontend |
|---|---|---|
| GET | `401`, `503` | Encerrar sessão em `401`; erro recuperável em falha de catálogo/serviço. |
| POST | `400`, `401`, `404`, `409`, `503` | Validar request com orientação; autenticação; produto inexistente/inativo; conflito de limite/estoque; indisponibilidade. |
| PUT | `400`, `401`, `404`, `409`, `503` | Preservar quantidade anterior em erro; conflito mantém detalhes úteis. |
| DELETE item | `401`, `404` | Não marcar remoção como confirmada em erro. |
| DELETE carrinho | `401` | Não marcar limpeza como confirmada em erro. |

`ErrorResponse` pode trazer `status`, `message`, `details`, `correlationId` e `timestamp`. Usar mensagem em linguagem amigável e preservar detalhe acionável quando seguro. `403`, `422` e outros `5xx` não são emitidos pelos controllers/handler atuais identificados; manter fallback genérico caso outra camada os retorne.

## Divergência OpenAPI

O arquivo versionado documenta apenas `items` e campos básicos e não descreve campos financeiros, `maxItemQuantity` ou `409`. O usuário confirmou que os DTOs/controllers atuais são a referência para esta feature. Registrar a diferença; não editar o contrato nem qualquer arquivo backend.