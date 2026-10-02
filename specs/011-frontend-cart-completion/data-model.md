# Modelo de Dados e Estado Frontend: Carrinho

O frontend não cria entidade persistida do carrinho nem uma fonte financeira autoritativa. O modelo em memória espelha a resposta confirmada dos DTOs/controllers atuais do Cart Service. O OpenAPI versionado está desatualizado; consulte [contrato de consumo](contracts/cart-gateway-consumption.md).

## CartResponse

| Campo | Tipo | Uso e regra |
|---|---|---|
| `items` | `CartItem[]` | Linhas atuais; lista vazia representa carrinho vazio confirmado. |
| `maxItemQuantity` | inteiro positivo | Limite retornado para orientar controles; backend continua autoridade. |
| `total` | decimal ou `null` | Total oficial; mostrar somente quando disponível. Nunca recomputar no cliente. |
| `totalAvailable` | boolean | Indica se `total` pode ser apresentado. |

## CartItem

| Campo | Tipo | Uso e regra |
|---|---|---|
| `productId` | UUID/string | Chave de mutação; não exibir como informação comercial. |
| `quantity` | inteiro positivo | Quantidade confirmada e contribuição ao badge de unidades. |
| `available` | boolean | Disponibilidade conforme serviço; não inferir do estoque de catálogo. |
| `product` | `ProductSummary` ou `null` | Resumo atual; pode ser nulo se produto indisponível. |
| `unitPriceSnapshot` | decimal ou `null` | Preço por unidade associado à linha; não substituir por `product.price`. |
| `priceAvailable` | boolean | Estado de validade/disponibilidade do snapshot. |
| `subtotal` | decimal ou `null` | Subtotal da linha calculado e fornecido pelo serviço. |

## ProductSummary

| Campo | Tipo | Uso e regra |
|---|---|---|
| `name` | string | Nome para identificação visual quando há resumo. |
| `price` | decimal ou `null` | Preço atual de catálogo, distinto do snapshot financeiro da linha. |
| `brand` | string ou `null` | Marca opcional. |
| `imageUrl` | string ou `null` | Imagem opcional; falha/ausência usa placeholder acessível. |

## Estado Compartilhado

- **Sessão**: JWT permanece no estado de autenticação existente. Troca, expiração ou encerramento limpa itens e invalida requisições anteriores.
- **Carrinho carregado**: substituído integralmente após GET, POST ou PUT confirmados.
- **Mutação pendente**: serialização atual impede concorrência de mutações; os controles mostram loading e ficam desabilitados.
- **Remoção confirmada**: após `204`, remover a linha afetada sem recomputar total; marcar resumo como desconhecido até GET posterior.
- **Limpeza confirmada**: após `204`, linhas vazias e badge zero; GET posterior carrega o total oficial do carrinho vazio.
- **Erro de resumo**: se GET posterior a DELETE falhar, manter linhas já mutadas, definir resumo não disponível e habilitar retry que executa somente GET.
- **Erro de mutation**: manter linhas e valores da última resposta confirmada; `401` também encerra a sessão pelo mecanismo existente.

## Derivações Permitidas na UI

- Produtos distintos: quantidade de elementos em `items`.
- Unidades no badge/resumo: soma de `quantity`, incluindo itens indisponíveis.
- Estado vazio: `items.length === 0` após sucesso confirmado/GET.
- Estado de preço: dependente dos indicadores/valores retornados; campos nulos nunca são formatados como `R$ 0,00`.

## Cálculos Proibidos

- Total do carrinho pela soma de subtotais.
- Subtotal pela multiplicação de preço exibido e quantidade.
- Substituição de snapshot desconhecido pelo preço atual de catálogo.
- Disponibilidade real por leitura de estoque no catálogo.
- Limite de quantidade distinto de `maxItemQuantity` informado.