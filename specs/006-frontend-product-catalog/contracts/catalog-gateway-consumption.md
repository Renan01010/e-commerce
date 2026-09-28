# Contrato Consumido: Catálogo via API Gateway

Este documento registra o contrato usado pelo frontend. Ele não cria nem substitui o contrato
OpenAPI do Product Service, que permanece em
`specs/001-foundation-product-catalog/contracts/product-service-api.openapi.yaml`.

## Base

- O frontend usa a base configurada por `VITE_API_URL`, com `/api` como padrão local.
- As requisições públicas passam pelo API Gateway.
- Não é necessário enviar JWT para as operações de leitura do catálogo.
- Se existir uma sessão válida, o cliente HTTP compartilhado pode adicionar `Authorization`;
  a leitura não deve depender desse cabeçalho.

## Operações

### `GET /api/products`

Parâmetros opcionais ou de paginação:

| Parâmetro | Regra de uso |
|---|---|
| `query` | Termo de pesquisa em nome, marca e descrição. |
| `categoryId` | UUID da categoria. |
| `minPrice`, `maxPrice` | Limites inclusivos não negativos. |
| `inStock` | `true` para quantidade maior que zero. |
| `brand` | Marca escolhida pelo visitante. |
| `sortBy` | `relevance`, `price`, `name` ou `newest`. |
| `sortOrder` | `asc` ou `desc`. |
| `page` | Inteiro não negativo, iniciado em zero. |
| `pageSize` | Inteiro entre 1 e 100; padrão do catálogo: 20. |

Sucesso retorna `ProductPage` com `content`, `totalElements`, `totalPages`, `currentPage`,
`pageSize` e `hasMore`. O cliente envia apenas filtros preenchidos e nunca usa `cost` na
apresentação.

### `GET /api/categories`

Retorna uma lista de categorias com `id`, `name`, `parentCategoryId`, `displayOrder` e os
demais metadados do contrato. Falha no carregamento não deve converter UUID em rótulo visível.

### `GET /api/products/{id}`

Retorna o produto ativo identificado pelo UUID, incluindo nome, descrição, preço, marca, SKU,
categoria, quantidade e imagem quando disponíveis. Produto inexistente ou inativo retorna estado
de não encontrado para o visitante.

## Erros

Erros de transporte, `4xx` e `5xx` são convertidos em mensagem segura e compreensível pelo
frontend. Detalhes internos, tokens, credenciais e custo nunca são exibidos. A consulta atual
deve ser preservada quando a recuperação permitir nova tentativa.

## Compatibilidade

Qualquer alteração nesse contrato deve ser feita no Product Service e no OpenAPI da feature
`001-foundation-product-catalog` antes de alterar o frontend. Esta feature não adiciona rotas,
parâmetros ou respostas novas.
