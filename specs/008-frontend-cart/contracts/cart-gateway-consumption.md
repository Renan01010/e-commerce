# Contrato de Consumo do Carrinho pelo Frontend

Este documento referencia o contrato autoritativo de `004-cart`; não cria nem substitui endpoints. OpenAPI de origem: [cart-service-api.openapi.yaml](../../004-cart/contracts/cart-service-api.openapi.yaml).

## Base e Autenticação

- Base URL do frontend: `VITE_API_URL`, padrão `/api`, configurada no Axios `apiClient` existente.
- A URL resultante atravessa o API Gateway. O frontend não chama o Cart Service diretamente.
- Requests protegidos reutilizam o interceptor existente para enviar `Authorization: Bearer <token>` de uma sessão válida.
- Nenhum request ou path inclui `userId`; o backend deriva proprietário do `sub` JWT.
- Paths abaixo são relativos à base `/api` do cliente.

## Operações

| Operação | Request | Respostas tratadas pelo frontend |
|---|---|---|
| `GET /cart` | Sem body | `200 CartResponse`; `401` sessão ausente/inválida; `503` dependência de catálogo indisponível. |
| `POST /cart/items` | `{ "productId": "<UUID>", "quantity": 1 }` | `201` nova linha ou `200` consolidada, ambas com `CartResponse`; `400`, `401`, `404`, `503` como erro. |
| `PUT /cart/items/{productId}` | `{ "quantity": 2 }` | `200 CartResponse`; `400`, `401`, `404`, `503` como erro. |
| `DELETE /cart/items/{productId}` | Sem body | `204` sem body; `401` ou `404` como erro. |
| `DELETE /cart` | Sem body | `204` sem body; `401` como erro. |

Quantidade em POST/PUT é um inteiro positivo. Os requests não aceitam outros campos de proprietário.

## Estruturas Relevantes

`CartResponse` contém `items: CartItem[]`. Cada `CartItem` possui `productId`, `quantity`, `available` e `product`. `product` é um resumo atual com `name`, `price`, `brand` e `imageUrl`, ou `null`. Em item indisponível, o contrato prevê `available: false` e `product: null`.

POST/PUT devolvem o carrinho atualizado; usar esse corpo para substituir o estado frontend. DELETE só confirma com 204; atualizar estado local após sucesso, sem tentar interpretar corpo. A descrição completa, erros e schemas são mantidos exclusivamente no OpenAPI de 004-cart.