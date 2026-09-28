# Modelo de Estado Frontend: Carrinho

O frontend não cria modelo persistido nem uma fonte autoritativa do carrinho. Os tipos abaixo representam a resposta transitória do Cart Service, em memória, conforme `specs/004-cart/contracts/cart-service-api.openapi.yaml`.

## CartResponse

| Campo | Tipo | Regra |
|---|---|---|
| `items` | `CartItem[]` | Lista retornada para a sessão autenticada; vazia quando o carrinho não contém linhas. |

## CartItem

| Campo | Tipo | Regra |
|---|---|---|
| `productId` | UUID/string | Identificador do produto; mantém-se disponível mesmo se o produto estiver indisponível. |
| `quantity` | inteiro positivo | Quantidade confirmada pelo Cart Service. |
| `available` | boolean | Estado enviado pelo Cart Service; não deve ser inferido da quantidade de estoque no catálogo. |
| `product` | `ProductSummary  null` | Resumo atual do Product Service ou nulo para produto inexistente/inativo. |
| `product` | `ProductSummary ou null` | Resumo atual do Product Service ou nulo para produto inexistente/inativo. |

## ProductSummary

| Campo | Tipo | Regra |
|---|---|---|
| `name` | string | Nome atual fornecido pelo backend. |
| `price` | número decimal | Informação atual do produto; não representa subtotal nem total do carrinho. |
| `brand` | string ou null | Marca atual, quando fornecida. |
| `imageUrl` | URI string ou null | Imagem atual, quando fornecida. |

## Estado da Interface

O estado compartilhado mantém a lista confirmada da API e metadados de apresentação. Não persiste em `localStorage`, `sessionStorage` ou banco local.

| Estado lógico | Conteúdo esperado | Transição principal |
|---|---|---|
| `unauthenticated` | Sem linhas visíveis; orientação e link para `/login` | Sessão ausente/expirada ou resposta 401. |
| `idle` | Sessão válida; carrinho ainda não carregado | Inicializa GET quando cabeçalho/página precisar dos dados. |
| `loading` | GET inicial em andamento, deduplicado na sessão | Sucesso vai para `loaded`; erro mantém estado recuperável. |
| `loaded` | `CartResponse.items` confirmado | POST/PUT substituem pelo response; DELETE atualiza depois de 204. |
| `mutating` | Últimas linhas confirmadas mais indicador da ação pendente | Sucesso aplica response/204; falha mantém linhas anteriores. |
| `error` | Mensagem segura e ação de recuperação aplicável | Retry ou operação posterior tenta novamente. |

A implementação pode representar essas fases em campos Zustand equivalentes; não precisa criar classes de domínio ou uma máquina de estados externa.

## Derivações

- **Unidades no badge**: soma de `quantity` para todas as linhas, incluindo indisponíveis.
- **Estado vazio**: `items.length === 0` após resposta bem-sucedida.
- **Linha indisponível**: `available === false` e `product === null`; apresentar UUID/identificação suficiente, indicação de indisponibilidade e remoção; não ler nome, preço, marca ou imagem e não oferecer PUT.
- **Linha disponível**: apresentar resumo atual não nulo; campos `brand` e `imageUrl` podem ser nulos.
- **Quantidade decrementável**: somente inteiro resultante maior ou igual a um; a remoção é operação distinta.

## Ciclo de Vida da Sessão e Sincronização

1. Sessão ausente: não requisitar GET autenticado; não manter linhas de outra sessão.
2. Sessão válida: carregar uma vez sob demanda; chamadas simultâneas para a mesma sessão compartilham a carga em andamento.
3. Mudança, expiração ou limpeza de sessão: invalidar request pendente e remover as linhas anteriores.
4. POST/PUT: aceitar o `CartResponse` recebido como estado compartilhado confirmado.
5. DELETE de linha: somente após 204 remover localmente o `productId` confirmado; resposta sem corpo.
6. DELETE do carrinho: somente após 204 substituir a lista por vazia; resposta sem corpo.
7. Erro de rede/404/503: manter o estado confirmado anterior e exibir erro contextual. 401 limpa a sessão e os dados do carrinho.

Não existe entidade financeira de resumo, total, frete, desconto, checkout ou snapshot de preço nesta feature.