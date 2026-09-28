# Pesquisa: Redesign Visual do Catálogo

## Decisões

### 1. Evoluir a linguagem visual existente

**Decisão**: preservar off-white, verde-limão, tipografia `DM Sans`/`Space Grotesk`/`DM Mono`,
bordas sutis, foco verde profundo e o tom tecnológico já presente em `styles.css`.

**Racional**: a feature pede evolução do TechStore, não uma troca de identidade. Reutilizar tokens
reduz regressão visual e mantém login/catálogo reconhecíveis como o mesmo produto.

**Alternativas consideradas**: tema escuro, paleta nova ou layout de dashboard. Rejeitadas por
contradizerem a direção solicitada e a identidade existente.

### 2. Refatorar composição sem duplicar lógica

**Decisão**: alterar composição de `App.tsx`, `CatalogPage.tsx`, `ProductCard.tsx` e `styles.css`,
sem mover busca, filtros, paginação ou chamadas para novos componentes de estado.

**Racional**: `catalogStore` e `catalogApi` já implementam o comportamento funcional aprovado.
Separar novamente a lógica aumentaria risco de divergência e não agrega valor visual.

**Alternativas consideradas**: criar uma nova página paralela ou um segundo store. Rejeitadas por
duplicarem funcionalidades e quebrarem a restrição de reutilização.

### 3. Tratar conteúdo opcional como ausência real

**Decisão**: avaliação, preço anterior e desconto só serão renderizados se já existirem em dados
confiáveis; o redesign não altera tipos nem calcula regras comerciais.

**Racional**: o modelo atual não possui esses campos. Reservar espaços ou inventar valores
prejudicaria a confiança do usuário e criaria uma regra fora do Product Service.

### 4. Usar affordances futuras sem ativar carrinho

**Decisão**: o header pode apresentar conta e uma entrada visual de carrinho indisponível/futura,
sem contador inventado, chamadas ou mutações.

**Racional**: a solicitação pede a estrutura de navegação, mas o carrinho está fora do escopo.
O estado visual deve comunicar o limite sem parecer uma ação quebrada.

### 5. Validar por comportamento e viewport

**Decisão**: manter testes automatizados de componentes/páginas e complementar com validação
manual em 320 px, tablet e desktop, incluindo teclado e redução de movimento.

**Racional**: testes protegem busca, filtros e navegação; proporção, ritmo visual e ausência de
overflow precisam ser conferidos no navegador.

## Conclusão

Não há decisão técnica bloqueante. A implementação pode avançar como refatoração visual incremental,
com regressão funcional após cada grupo de componentes.
