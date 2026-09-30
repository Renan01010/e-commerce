# Modelo de Dados: Carrinho de Compras

## Agregado Cart

**Identidade**: UUID do usuário autenticado, extraído exclusivamente do `sub` do JWT validado. O cliente nunca fornece o proprietário.

**Persistência**: o agregado é materializado implicitamente pelas linhas em `cart_items`. Não existe registro persistido para carrinho vazio; ausência de linhas equivale a `items: []`.

**Regras**:

- Um usuário só consulta ou modifica linhas cujo `owner_user_id` corresponde ao próprio `sub`.
- Cada par usuário/produto tem no máximo uma linha.
- `quantity` é inteiro positivo; POST soma à linha existente e PUT substitui.
- Produto não é entidade local nem fonte de verdade do Cart Service.

## CartItem persistido

| Campo | Tipo PostgreSQL | Regras |
|---|---|---|
| `owner_user_id` | `UUID` | Não nulo; parte da chave composta; valor do `sub` validado. |
| `product_id` | `UUID` | Não nulo; parte da chave composta; não possui FK entre serviços. |
| `quantity` | `INTEGER` | Não nulo e maior que zero (`CHECK`). |

**Chave primária**: `(owner_user_id, product_id)`. Essa chave garante unicidade da linha e permite upsert atômico para adições repetidas/concor­rentes. A tabela é criada e evoluída por Flyway no database lógico `techstore_cart_db`.

## ProductSummary de resposta

Representação transitória, consultada por REST e nunca persistida:

| Campo | Tipo lógico | Origem |
|---|---|---|
| `name` | string | Product Service |
| `price` | decimal | Product Service |
| `brand` | string ou null | Product Service |
| `imageUrl` | URI string ou null | Product Service |

O endpoint existente retorna somente produto ativo. Se responder 404 para uma linha persistida, a representação do carrinho contém `available=false` e `product=null`; a linha continua removível. Falhas de rede, timeout ou 5xx não são interpretadas como produto ausente: leitura/mutação dependente do catálogo responde 503.

## CartItem de API

| Campo | Tipo | Regra |
|---|---|---|
| `productId` | UUID | Identificador persistido do produto. |
| `quantity` | inteiro | Positivo; valor persistido. |
| `available` | boolean | `true` se Product Service respondeu produto ativo; `false` caso contrário. |
| `product` | ProductSummary ou null | Resumo atual quando disponível; nunca snapshot local. |

## Transições

| Operação | Estado persistido |
|---|---|
| GET sem linhas | Nenhuma alteração; retorna lista vazia. |
| POST para produto ativo ausente | Insere linha com quantidade solicitada. |
| POST para produto ativo existente | Upsert atômico soma a quantidade. |
| POST/PUT para produto inválido/inativo | Rejeita antes de persistir. |
| PUT para linha própria existente e produto ativo | Substitui a quantidade. |
| DELETE item | Remove somente a chave usuário/produto; linha ausente retorna 404. |
| DELETE carrinho | Remove todas as linhas do proprietário; repetição permanece 204. |
| Produto desativado após inclusão | Mantém linha; leitura marca indisponível até remoção. |