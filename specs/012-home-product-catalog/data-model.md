# Modelo de Dados: Home e Catálogo de Produtos

Este documento descreve dados existentes consumidos e estado transitório de interface. Não há entidade nova persistida no frontend.

## Produto do catálogo

Representa produto ativo retornado pelo Product Service.

| Campo | Uso |
|---|---|
| `id` | Identificador para chave de UI e rota `/products/:id`; nunca renderizado como UUID. |
| `name` | Nome em cards, listagem e detalhe. |
| `description` | Descrição opcional. |
| `price` | Preço atual retornado pelo Product Service; não calcular ou substituir. |
| `brand` | Marca opcional. |
| `sku` | Referência comercial disponível no detalhe quando útil. |
| `categoryId` | Relaciona produto a uma categoria real. |
| `quantity` | Disponibilidade conforme contrato de catálogo (`> 0` disponível; `0` esgotado). Não equivale a reserva/garantia durante adição. |
| `imageUrl` | Imagem remota opcional; requer lazy loading e fallback visual. |
| `createdAt`, `updatedAt` | `createdAt` é usado indiretamente pela ordenação existente `newest`; não inferir badge “Novo”. |

`cost` é campo administrativo e nunca é apresentado. O Product Service não fornece avaliação, preço anterior, desconto, popularidade ou `featured`; a interface não os inventa.

## Categoria

Categoria ativa retornada por `GET /api/categories`.

| Campo | Uso |
|---|---|
| `id` | Filtro de catálogo e chave de UI; não apresentar como texto. |
| `name` | Rótulo visual real. |
| `parentCategoryId` | Hierarquia quando disponível e validável na lista retornada. |
| `displayOrder` | Ordenação visual de categorias. |
| `description`, `isActive`, timestamps | Metadados de resposta; não determinam imagens, promoções ou nomes adicionais. |

O backend retorna também `slug`; o tipo frontend atual não precisa desse campo para esta feature. Não há imagem de categoria no contrato.

## Resultado paginado de produtos

`ProductPage` recebido da API:

- `content`: produtos da página em resposta.
- `totalElements`, `totalPages`, `currentPage`, `pageSize`, `hasMore`: metadados autoritativos da paginação.

## Consulta de produtos recentes da Home

Estado transitório distinto da busca paginada de catálogo:

| Campo lógico | Valor/uso |
|---|---|
| `products` | Até oito produtos retornados na página zero da consulta recente. |
| `sortBy` | `newest`. |
| `sortOrder` | `desc`. |
| `page` | `0`. |
| `pageSize` | `8`. |
| `isLoading` | Indica consulta recente em andamento. |
| `error` | Erro amigável dessa consulta; separado do erro do catálogo e das categorias. |
| `requestVersion` | Controle interno para ignorar resposta obsoleta. |

Esse estado não contém ordenação/ranking editorial e não sobrescreve produtos/filtros/página do catálogo.

## Estado de categorias compartilhadas

As categorias são carregadas uma vez por sessão da SPA e reutilizadas em Home e catálogo após resposta bem-sucedida. Requests concorrentes são deduplicadas. Um erro não é tratado como lista vazia bem-sucedida nem armazenado como cache; retry explícito faz nova chamada. O estado de loading/erro das categorias não substitui o estado dos produtos.

## Consulta do catálogo completo

Estado já existente em `catalogStore`: `searchQuery`, `filters` (`categoryId`, `minPrice`,
`maxPrice`, `inStock`, `brand`), `sortBy`, `sortOrder`, página e metadados. A página começa em zero.
`query` e `categoryId` da URL são a fonte de verdade para busca e categoria: mudanças dos controles
atualizam a URL, parâmetros ausentes limpam o valor correspondente e voltar/avançar restaura os
valores. Os demais filtros, ordenação e paginação continuam no store e não são serializados nesta
feature. Mudanças de critérios reiniciam a página conforme comportamento existente.

## Estado compartilhado do carrinho

`cartStore` permanece a fonte para itens confirmados, `maxItemQuantity`, `total`, `totalAvailable`, status, feedback e `pendingOperations`. Cards usam `addItem` do store; não criam um segundo estado financeiro ou de quantidade.

## Conteúdo comercial editorial

Texto de Hero, benefícios informativos, decoração dos banners e seus destinos gerais são configuração estática frontend. Não representa entidade de catálogo, promoção confirmada, estoque, preço ou campanha persistida. Categoria e produto continuam sempre derivados das respostas atuais.
