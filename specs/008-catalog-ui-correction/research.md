# Pesquisa: Correção Responsiva da UI do Catálogo

## Decisões

### 1. Manter a autoridade funcional existente

**Decisão**: não alterar `catalogStore`, `catalogApi`, tipos, rotas de dados ou contratos; a UI
somente reorganiza controles e apresenta o mesmo estado.

**Racional**: o catálogo já está implementado e `006-frontend-product-catalog` define a
integração aprovada. A correção é visual e uma nova fonte de dados criaria regressão.

**Alternativas consideradas**: recriar a página com store/API próprios ou criar endpoints de
layout. Rejeitadas por duplicação e violação explícita do escopo.

### 2. Usar composição responsiva por faixa

**Decisão**: definir comportamentos distintos para mobile pequeno, mobile, tablet, desktop e
telas grandes, usando container fluido, grid adaptável e padding relativo.

**Racional**: reduzir somente fontes do desktop não resolve menu, filtros, hero ou overflow.
Faixas explícitas permitem testar decisões de interação, não apenas escala.

**Alternativas consideradas**: um único layout com `transform: scale`, `width: 100vw` ou uma
quantidade fixa de colunas. Rejeitadas por causar overflow e perda de legibilidade.

### 3. Filtros como drawer/superfície móvel

**Decisão**: sidebar proporcional no desktop, controle recolhível no tablet e botão que abre
superfície de filtros no mobile. Os valores continuam no store atual.

**Racional**: o mobile precisa priorizar produtos; um drawer permite aplicar/limpar/fechar sem
perder os critérios nem ocupar a viewport inteira permanentemente.

**Alternativas consideradas**: manter sidebar visível ou remover filtros do mobile. Rejeitadas
por reduzir espaço ou retirar uma funcionalidade existente.

### 4. Validar overflow como contrato visual

**Decisão**: medir `scrollWidth` contra a largura útil em cada viewport e inspecionar foco,
texto, cards, header, filtros e estados.

**Racional**: overflow pode surgir de combinações específicas de textos e controles que testes de
renderização não capturam. A matriz pedida transforma o problema visual em critério verificável.

### 5. Preservar dados reais e estados atuais

**Decisão**: não adicionar rating, desconto, preço anterior, imagens fictícias ou dados de
integração para preencher a UI; manter mensagens e ações de erro existentes.

**Racional**: a UI deve revelar o estado real do catálogo e não mascarar indisponibilidade da API.

## Conclusão

Não há decisão bloqueante. A correção pode avançar como refatoração de composição, responsividade
e acessibilidade, com regressão funcional contínua.
