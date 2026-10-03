# Data Model: Consistência Visual e Redesign das Telas Existentes

Esta feature não introduz entidades, persistência ou estado de negócio. A UI continua apresentando os modelos e estados existentes, sem alterar suas formas, fontes de autoridade ou transições.

## Modelos existentes utilizados

| Modelo | Origem | Uso visual | Regra de preservação |
|---|---|---|---|
| `Product` | `frontend/src/types/catalog.ts`, carregado pelo `catalogStore` | Cards, catálogo e detalhe: nome, marca, imagem, preço, disponibilidade e demais campos apresentados atualmente | Renderizar somente dados retornados; não inferir desconto, avaliação ou disponibilidade. |
| `Category` | `frontend/src/types/catalog.ts`, carregado pelo `catalogStore` | Navegação e filtros de categoria no shell e no catálogo | Usar categorias reais; não criar categoria ou alterar filtros/URLs. |
| `ProductPage` | `frontend/src/types/catalog.ts` | Resultados e metadados da paginação | Preservar página atual, contagem e sinalizadores autoritativos do store. |
| `CartItem` e `CartResponse` | `frontend/src/types/cart.ts`, atualizados pelo `cartStore` | Linhas de carrinho, limites, preços, subtotais, total e badge | `product`, preço/subtotal e total podem ser nulos/indisponíveis; não calcular nem presumir valores. |
| `Session`/estado de autenticação | `frontend/src/types/auth.ts` e `authStore` | Acesso às rotas/ações protegidas e estado do Login | Preservar ciclo de sessão, integração e navegação atuais. |

## Estado visual

Estados de loading, erro, vazio, sucesso, indisponibilidade e operação pendente são estados existentes das páginas/stores, não novos dados de domínio. O redesign pode ajustar sua apresentação, mas não sua origem, significado ou transições.

## Rotas e composição

- `/`, `/catalog`, `/products/:id`, `/cart` e fallback continuam dentro de `StoreLayout`.
- `/login` e `/register` continuam layouts standalone.
- Busca/categoria continuam sincronizadas com a URL conforme a implementação atual.
- Nenhum novo store, campo de API, formato de sessão ou estado financeiro será introduzido.
