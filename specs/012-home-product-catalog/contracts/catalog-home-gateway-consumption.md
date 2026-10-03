# Contrato Consumido pela Home via API Gateway

Este documento registra o contrato que o frontend consumirá. Não cria rotas ou altera o contrato OpenAPI ou qualquer serviço backend. A implementação efetiva do Product Service e o cliente existente em `frontend/src/services/apiClient.ts` são a referência para os campos e parâmetros descritos.

## Base e acesso

- Usar a base `VITE_API_URL`, `/api` por padrão.
- Todas as chamadas passam pelo API Gateway e `apiClient`.
- Consultas públicas do catálogo não dependem de uma sessão; o interceptor existente pode anexar autorização se houver sessão válida.
- Não acessar PostgreSQL nem executar consultas de domínio no frontend.

## Produtos recentes da Home

### `GET /api/products`

A seção da Home solicita a primeira página com:

| Parâmetro | Valor |
|---|---|
| `sortBy` | `newest` |
| `sortOrder` | `desc` |
| `page` | `0` |
| `pageSize` | `8` |

Busca vazia e filtros são omitidos pelo `catalogApi`. A resposta `ProductPage` usa `content`,
`totalElements`, `totalPages`, `currentPage`, `pageSize` e `hasMore`. A UI apresenta somente os
produtos em `content`; o total não é exibido como inventário garantido.

O contrato também suporta busca, filtros de categoria/preço/disponibilidade/marca e ordenações
`relevance`, `price`, `name` ou `newest`, com direção `asc` ou `desc`; páginas começam em zero e
`pageSize` permitido é de 1 a 100. A consulta paginada do catálogo preserva esses parâmetros
existentes.

### Produto de resposta

Campos utilizáveis pela UI: `id`, `name`, `description`, `price`, `brand`, `sku`, `categoryId`,
`quantity`, `imageUrl`, `isActive`, `createdAt` e `updatedAt`. `cost` não é dado de apresentação.
Não há `oldPrice`, desconto, avaliação, rótulo de destaque ou ranking no contrato.

## Categorias

### `GET /api/categories`

Retorna categorias ativas com `id`, `name`, `slug`, `description`, `parentCategoryId`,
`displayOrder`, `isActive` e timestamps. A UI usa nomes/IDs reais e `displayOrder`.
Não existe imagem no DTO.

A lista carregada com sucesso é reutilizada em memória pela Home e pelo catálogo durante a
sessão da SPA; requests concorrentes são deduplicadas. Falhas não são cacheadas e retry explícito
consulta o endpoint novamente.

**Divergência conhecida**: `specs/009-category-management/contracts/category-management-api.md`
descreve `search` para essa rota, mas o `CategoryController` implementado não declara parâmetro.
Não enviar `search`. `slug` existe na resposta atual, mas não é necessário nem declarado no tipo
frontend existente.

## Detalhes

### `GET /api/products/{id}`

Usar a operação existente ao abrir `/products/:id`. O Product Service retorna o produto ativo ou
resposta de erro de não encontrado; a UI não converte esse erro em dados simulados.

## Carrinho

O botão no card deve chamar `cartStore.addItem({ productId, quantity: 1 })`. O store existente
encaminha `POST /api/cart/items`, envia sessão conforme cliente compartilhado, valida seu estado
e limite autoritativos, deduplica a mutação pendente e aplica a resposta `CartResponse`.

`GET /api/cart` permanece a inicialização para sessão existente. Itens/quantidades confirmados
do store alimentam o badge; os totais não são recalculados na Home. Status e erros são apresentados
pelo feedback do store, sem expor detalhes HTTP brutos.

## Erros e recuperação

- Produtos e categorias têm estados independentes e ação de nova tentativa.
- Falhas HTTP e de rede usam mensagem amigável, sem corpo técnico, token, stack trace ou UUID.
- Cobrir `400`, `401`, `403`, `404`, `409`, `429` e `5xx`; mensagens específicas só devem alegar
um significado quando esse comportamento é conhecido. Demais status usam fallback seguro.
- `401` do carrinho continua pelo fluxo de sessão existente; erros de catálogo não inventam
resultado vazio nem apagam resultado confirmado por mensagem de sucesso.

## Fora de escopo

Nenhuma rota de CMS, favoritos, avaliação, categoria com imagem, desconto ou preço comparativo é
adicionada; nenhum serviço, endpoint ou contrato backend é alterado.
