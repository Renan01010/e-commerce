# Contrato de Preservação da UI — Feature 013

Este documento registra o contrato de interação frontend a preservar durante o redesign. Não cria ou altera endpoint, DTO, regra de negócio ou contrato de serviço.

## Rotas e layout

| Rota | Composição | Comportamento a preservar |
|---|---|---|
| `/` | `StoreLayout` + Home | Conteúdo e busca da Home; header não duplica sua busca; ações e navegação existentes. |
| `/catalog` | `StoreLayout` + `CatalogPage` | Busca, filtros, categoria, ordenação, paginação, estados e navegação de produto existentes. |
| `/products/:id` | `StoreLayout` + detalhe | Produto real, estados de carregamento/erro/indisponibilidade e integração de carrinho atuais. |
| `/cart` | `StoreLayout` + carrinho | Sessão, linhas, quantidades, limites, valores confirmados, remoção, limpeza, mensagens e vazio atuais. |
| `/login` | `LoginPage` standalone | Campos, validação, submissão, loading/erro/sucesso, sessão e redirecionamento atuais; sem header da loja. |
| `/register` | `RegisterUnavailablePage` standalone | Estado indisponível atual e navegação de retorno; sem formulário, novo fluxo ou header da loja. |
| Fallback | `StoreLayout` | Mensagem e navegação para o catálogo atuais. |

## Autoridade e interfaces de serviço

- Catálogo e produto continuam carregados por `catalogStore` e clientes existentes via API Gateway.
- Sessão permanece em `authStore`; autenticação e token não são redesenhados em sua implementação.
- Carrinho e badge continuam derivados do `cartStore` e das respostas confirmadas do Cart Service.
- Nenhum campo enviado, método, path, status esperado ou schema de serviço é alterado.
- A UI não calcula ou inventa preço, subtotal, total, estoque, disponibilidade ou limite de quantidade.

## Mudanças permitidas

Somente hierarquia visual, cores/tokens, tipografia, superfícies, bordas, espaçamento, responsividade, estados de foco e aparência de loading/erro/vazio, semântica acessível adicional e CSS necessário para telas aprovadas. O resultado deve preservar a identidade escura da Home e manter as telas de autenticação standalone.
