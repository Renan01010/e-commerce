# Modelo de Dados: Catálogo de Produtos no Frontend

Este documento descreve os dados consumidos e o estado transitório da experiência. Nenhuma
entidade nova é persistida pelo frontend.

## Produto

Representa um item ativo fornecido pelo Product Service.

| Campo | Uso no frontend |
|---|---|
| `id` | Identifica o produto e compõe a rota de detalhes. |
| `name` | Título do card e da página de detalhes. |
| `description` | Resumo do card e descrição completa quando disponível. |
| `price` | Preço atual exibido em BRL; não é recalculado. |
| `brand` | Metadado de identificação e filtro. |
| `sku` | Referência apresentada no detalhe quando disponível. |
| `categoryId` | Relaciona o produto à categoria retornada pelo serviço. |
| `quantity` | Deriva disponibilidade: maior que zero disponível; zero esgotado. |
| `imageUrl` | Imagem opcional; ausência exige substituto visual. |
| `isActive` | Produtos inativos não entram na listagem; detalhe inativo é não encontrado. |
| `createdAt`, `updatedAt` | Permitem a ordenação existente por produto mais recente; não são editados. |

`cost`, `createdBy` e `updatedBy` não são dados de apresentação do cliente e não devem ser
expostos no catálogo.

## Categoria

Representa o agrupamento usado no filtro e no contexto de navegação.

| Campo | Uso no frontend |
|---|---|
| `id` | Valor do filtro e referência do produto. |
| `name` | Rótulo visível ao visitante. |
| `parentCategoryId` | Permite apresentar a hierarquia quando válida. |
| `description`, `displayOrder`, `isActive` | Metadados de apresentação e seleção conforme resposta. |

## Página de produtos

Resposta paginada do Product Service:

- `content`: produtos da página atual.
- `totalElements`: total de produtos compatíveis.
- `totalPages`: total de páginas.
- `currentPage`: página atual, iniciada em zero no contrato.
- `pageSize`: tamanho efetivo da página.
- `hasMore`: indica se existe próxima página.

## Consulta do catálogo

Estado transitório composto por `query`, `categoryId`, `minPrice`, `maxPrice`, `inStock`,
`brand`, `sortBy`, `sortOrder`, `page` e `pageSize`.

Alterar busca, filtro ou ordenação reinicia `page` em zero. Limpar filtros remove somente os
critérios de refinamento e mantém o catálogo no estado padrão.

## Estados de apresentação

- **Carregando**: consulta em andamento; não deve confundir dados antigos com a resposta atual.
- **Sucesso com resultados**: lista/cards ou detalhe do produto retornado.
- **Vazio**: consulta válida sem produtos, com orientação para ajustar critérios.
- **Erro recuperável**: falha de Gateway/serviço, mensagem segura e nova tentativa/retorno.
- **Não encontrado**: detalhe sem produto ativo para o identificador solicitado.

## Relações e autoridade

O frontend relaciona `Product.categoryId` a `Category.id` somente para exibição e filtro. O
Product Service é a autoridade; o frontend não persiste, altera ou sincroniza esses dados.
