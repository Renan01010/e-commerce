# Quickstart de Validação: Feature 013

## Pré-requisitos

- Node.js/npm compatíveis com o projeto.
- Dependências frontend instaladas em `frontend/`.
- Testes automatizados usam mocks; Gateway, Product Service, Cart Service e User Service não são necessários para a suíte.

## Validação automatizada

Na pasta `frontend/`:

```powershell
npm test
npm run build
```

Esperado: testes frontend passam e o build TypeScript/Vite termina sem erros.

## Cenários manuais e responsivos

1. Abrir `/`, `/catalog`, `/products/:id`, `/cart`, `/login`, `/register` e uma rota inexistente; confirmar identidade dark/tech, sem header compartilhado no Login e no estado de cadastro indisponível.
2. Navegar entre as rotas da loja e confirmar que o header é único/compartilhado, a busca mantém o comportamento por rota, as categorias são reais e o badge permanece condicionado ao estado confirmado do carrinho.
3. No catálogo, validar a aparência dos controles sem alterar busca, categoria, faixa de preço, marca, disponibilidade, ordenação, paginação, estados de loading/erro/vazio ou links de produto.
4. No detalhe, validar dados da resposta real, imagem/fallback, indisponibilidade, estado de carregamento/erro e a ação de carrinho existente.
5. No Login, testar campos, foco/teclado, validação, erro e loading; em `/register`, confirmar apenas a mensagem de indisponibilidade e retorno ao Login.
6. No Carrinho, validar estado autenticado/visitante, vazio, erro/retry, loading, itens, limites, operações, mensagens e valores indisponíveis/confirmados; verificar remoção e limpeza sem regressão.
7. Inspecionar cada rota em 1280 px, 1024 px, 768 px e 375 px: sem overflow horizontal do documento, controles utilizáveis, grid/colunas adaptados e ações principais visíveis.
8. Navegar por teclado em header, filtros, formulário, detalhe e carrinho; verificar foco visível, labels, contraste, semântica e anúncios dos estados.

## Resultado da validação desta implementação

- As rotas Home, Catálogo, Detalhe, Login, Cadastro indisponível e Carrinho foram conferidas nos viewports de 1280 px, 1024 px, 768 px e 375 px; não foi observado overflow horizontal.
- A inspeção visual confirmou que a Home preserva a referência da Feature 012 e que Catálogo e Login usam o padrão dark/tech compartilhado.
- O Gateway/Product Service não estava disponível no ambiente de inspeção visual; por isso, as páginas exibiram estados de falha/vazio. Dados reais foram cobertos nos testes automatizados com mocks.

## Referências

- [Especificação](spec.md)
- [Plano](plan.md)
- [Pesquisa e decisões](research.md)
- [Modelo de dados](data-model.md)
- [Contrato de preservação da UI](contracts/frontend-ui-preservation.md)
- [Contrato atual de consumo do Carrinho](../008-frontend-cart/contracts/cart-gateway-consumption.md)
