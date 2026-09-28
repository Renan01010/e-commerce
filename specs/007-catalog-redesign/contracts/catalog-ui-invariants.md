# Contrato de UI: Invariantes do Catálogo

Este é um contrato de apresentação e regressão. Não adiciona endpoints nem substitui o contrato
OpenAPI do Product Service.

## Invariantes funcionais

- A rota `/` continua exibindo o catálogo público.
- A rota `/products/:id` continua abrindo o detalhe correspondente.
- Pesquisa, filtros, ordenação, paginação e mensagens funcionais usam as mesmas chamadas,
  parâmetros e respostas do catálogo existente.
- O card continua navegando para o detalhe e não inicia carrinho.
- A entrada visual de carrinho não faz chamadas nem altera estado enquanto o carrinho estiver fora
  do escopo.

## Invariantes visuais

- Paleta base: off-white, superfícies claras, texto escuro e verde-limão de destaque.
- Foco é sempre visível e não depende de hover.
- Cards mantêm dimensões estáveis quando imagem, título ou metadados variam.
- Estados loading, vazio, sem resultados e erro não ocupam espaço desproporcional nem se confundem.
- A grade adapta colunas e espaçamento para desktop, tablet e mobile.
- Movimento deve respeitar `prefers-reduced-motion`.

## Dados opcionais

Rating, preço anterior e desconto não fazem parte do contrato atual de produto. Só podem aparecer
se forem fornecidos por uma fonte confiável futura; nenhum fallback pode parecer um valor real.
