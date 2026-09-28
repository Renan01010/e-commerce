# Especificação da Feature: Correção Responsiva da UI do Catálogo

**Branch da Feature**: `008-catalog-ui-correction`
**Criado em**: 2026-09-28
**Status**: Draft
**Entrada**: Corrigir e refazer a UI/UX responsiva da tela de catálogo existente do TechStore sem alterar sua funcionalidade ou integração.

## Visão Geral

Esta feature corrige a composição visual do catálogo já implementado. O objetivo é entregar uma
página equilibrada, compacta e realmente responsiva, preservando a identidade tecnológica do
TechStore e todos os comportamentos funcionais definidos em `006-frontend-product-catalog`.

A especificação `007-catalog-redesign` é a referência visual anterior; esta feature concentra-se
nas correções observadas após sua implementação: hero alta, excesso de espaço vertical, busca
distante do título, catálogo abaixo da primeira viewport, sidebar rígida, header incompleto no
mobile e risco de overflow.

### Identidade preservada

- Fundo off-white e superfícies claras.
- Texto escuro e tipografia moderna já utilizada pelo frontend.
- Verde-limão/verde como destaque.
- Bordas discretas, respiro visual e aparência tecnológica premium.
- Composição elegante de e-commerce, sem aparência de dashboard administrativo.

### Autoridade funcional

O catálogo, sua busca, filtros, ordenação, paginação, estados, rotas e integração já existem.
Esta feature não recria esses comportamentos nem altera Product Service, API Gateway, banco,
contratos, autenticação ou regras de negócio.

## Cenários de Usuário e Testes

### História de Usuário 1 - Ver o catálogo sem desperdício de espaço (Prioridade: P1)

Como visitante, quero encontrar rapidamente a busca e os produtos para compreender a loja sem
percorrer uma hero desproporcional.

**Por que esta prioridade**: O catálogo deve ser a experiência principal, não uma introdução que
empurra os produtos para fora da primeira área de visualização.

**Teste independente**: Abrir o catálogo em desktop e confirmar a sequência header, breadcrumb,
hero compacta, busca, categorias e início do catálogo na mesma página.

**Cenários de Aceitação**:

1. **Dado** o visitante na rota do catálogo, **quando** a página carregar, **então** verá header,
   breadcrumb, hero compacta, busca, categorias e o início da área de produtos em uma composição
   contínua.
2. **Dado** um título longo ou uma viewport menor, **quando** o hero for exibido, **então** o
   título ocupará somente uma quantidade razoável de linhas e não criará um vazio vertical
   desnecessário.
3. **Dado** o campo de pesquisa no hero, **quando** o visitante digitar um termo, **então** a
   mesma busca existente será aplicada com os mesmos parâmetros, debounce e resultados.
4. **Dado** o container em uma tela grande, **quando** o catálogo for exibido, **então** seu
   conteúdo permanecerá centralizado em uma largura máxima confortável, sem espalhar controles
   até as bordas da viewport.

### História de Usuário 2 - Navegar em qualquer tamanho de tela (Prioridade: P1)

Como visitante em desktop, tablet ou mobile, quero que header, busca, categorias e catálogo se
reorganizem para que nenhuma ação ou conteúdo seja cortado.

**Por que esta prioridade**: A falha mais evidente atual é tratar telas menores como uma versão
reduzida do desktop, causando perda de navegação e desconforto de uso.

**Teste independente**: Abrir a página em 375, 390, 430, 768, 1024, 1280, 1440 e 1920 pixels,
confirmar ausência de overflow horizontal e verificar as ações essenciais em cada faixa.

**Cenários de Aceitação**:

1. **Dado** uma viewport desktop, **quando** o catálogo for exibido, **então** haverá header
   completo, filtros laterais e grid de produtos aproveitando a largura sem exagero.
2. **Dado** uma viewport tablet, **quando** o catálogo for exibido, **então** tipografia,
   espaçamento, colunas e filtros se adaptarão sem sobrepor a grade ou exigir largura fixa.
3. **Dado** uma viewport mobile, **quando** a página for exibida, **então** a logo permanecerá
   visível, conta e carrinho continuarão acessíveis, e a navegação desktop será substituída por
   uma entrada de menu adequada.
4. **Dado** qualquer uma das larguras suportadas, **quando** o visitante interagir com a página,
   **então** não haverá scroll horizontal, card fora da viewport, texto cortado ou botão inacessível.

### História de Usuário 3 - Usar filtros sem perder os produtos de vista (Prioridade: P1)

Como visitante, quero acessar os filtros sem que eles ocupem a maior parte da tela, especialmente
no mobile.

**Por que esta prioridade**: Os filtros são importantes para descoberta, mas a sidebar rígida
atual reduz o espaço dos produtos e não oferece uma experiência apropriada para toque.

**Teste independente**: Usar filtros em desktop, tablet e mobile, confirmando que os mesmos
valores, resultados, paginação e ação de limpar continuam funcionando.

**Cenários de Aceitação**:

1. **Dado** desktop, **quando** o catálogo carregar, **então** os filtros aparecerão em uma
   coluna lateral proporcional, sem consumir espaço excessivo.
2. **Dado** tablet, **quando** o visitante quiser filtrar, **então** poderá recolher ou expandir
   os controles sem deslocar ou sobrepor os produtos.
3. **Dado** mobile, **quando** o visitante selecionar “Filtros”, **então** os controles abrirão
   em uma superfície apropriada para toque, com ações claras de aplicar e limpar.
4. **Dado** filtros ativos, **quando** o visitante fechar ou aplicar a superfície de filtros,
   **então** os critérios continuarão visíveis por estado selecionado e os resultados manterão o
   comportamento existente.

### História de Usuário 4 - Comparar produtos em uma grade estável (Prioridade: P1)

Como visitante, quero comparar cards proporcionais e legíveis para decidir qual produto abrir.

**Por que esta prioridade**: Cards inconsistentes ou deformados prejudicam a leitura e a
confiança na vitrine.

**Teste independente**: Renderizar produtos com imagens variadas, títulos longos, estoque zero e
sem imagem em todas as faixas de viewport, confirmando dimensões e navegação.

**Cenários de Aceitação**:

1. **Dado** produtos com imagens de proporções diferentes, **quando** os cards forem exibidos,
   **então** as imagens ocuparão áreas consistentes sem deformação e preservarão sua leitura.
2. **Dado** nomes longos, **quando** os cards forem exibidos, **então** o texto permanecerá
   contido sem estourar o card, empurrar controles ou alterar a altura de forma abrupta.
3. **Dado** uma viewport desktop, tablet ou mobile, **quando** a grade for exibida, **então** a
   quantidade de colunas se adaptará à largura disponível sem overflow.
4. **Dado** um card focado, tocado ou sob hover, **quando** o estado de interação ocorrer,
   **então** haverá feedback visual sem alterar a ordem, dimensões ou acessibilidade do card.

### História de Usuário 5 - Recuperar-se de estados do catálogo (Prioridade: P2)

Como visitante, quero entender loading, erro, catálogo vazio e busca sem resultados sem confundir
um problema de integração com uma falha visual.

**Por que esta prioridade**: Estados incompletos são inevitáveis e precisam ocupar a página de
forma proporcional, com contexto e ação de recuperação.

**Teste independente**: Simular os estados existentes e verificar mensagens, ações, foco, espaço
ocupado e ausência de elementos cortados.

**Cenários de Aceitação**:

1. **Dado** produtos em carregamento, **quando** a resposta estiver pendente, **então** skeletons
   ocuparão dimensões próximas às dos cards sem criar saltos visuais.
2. **Dado** que o catálogo não possa ser carregado, **quando** o erro ocorrer, **então** a
   mensagem “O catálogo não pôde ser carregado” permanecerá funcional e será apresentada em um
   aviso integrado à composição, com nova tentativa.
3. **Dado** nenhum produto ou nenhum resultado de busca, **quando** o estado vazio for exibido,
   **então** haverá mensagem objetiva e ação para limpar/ajustar critérios sem grande área vazia.
4. **Dado** qualquer mudança de estado, **quando** o visitante usar teclado, **então** o foco
   visível e a informação acessível permanecerão compreensíveis.

## Casos de Borda

- O container nunca deve usar largura maior que a viewport nem produzir scroll horizontal.
- O hero deve continuar compacto quando o título quebrar em duas ou três linhas.
- Categorias em mobile devem permitir rolagem horizontal ou reorganização sem cortar o último item.
- O menu mobile não pode esconder logo, conta ou carrinho.
- Filtros abertos não podem ficar atrás do header, sobrepor produtos de modo indevido ou impedir
  fechar/aplicar/limpar.
- Imagens com proporções diferentes, ausentes ou inválidas devem usar uma área estável e não
  deformada.
- Produtos com preço zero, quantidade zero e textos longos continuam válidos e legíveis.
- Redução de movimento deve desativar ou reduzir animações não essenciais.
- A página não deve mascarar falhas de API com dados fictícios ou placeholders que pareçam reais.

## Requisitos

### Requisitos Funcionais

- **RF-001**: A correção DEVE preservar as rotas, navegação, busca, filtros, ordenação,
  paginação, detalhes e estados funcionais do catálogo existente.
- **RF-002**: A correção NÃO DEVE alterar Product Service, API Gateway, banco, contratos,
  autenticação, regras de negócio ou chamadas funcionais existentes.
- **RF-003**: A página DEVE usar uma composição contínua de header, breadcrumb, hero compacta,
  busca, categorias e catálogo, sem alturas fixas desnecessárias.
- **RF-004**: A hero DEVE limitar título, descrição e espaçamento para evitar que ocupe a maior
  parte da viewport ou empurre o catálogo excessivamente para baixo.
- **RF-005**: A busca DEVE permanecer integrada à composição da hero e reutilizar a implementação
  existente, incluindo debounce, parâmetros e resultados.
- **RF-006**: O layout DEVE usar container fluido, centralizado, com largura máxima adequada e
  padding lateral responsivo, sem depender de `width: 100vw` ou larguras rígidas incompatíveis.
- **RF-007**: Em desktop, o header DEVE apresentar logo, navegação, conta e carrinho; filtros
  DEVEM aparecer em coluna lateral proporcional e produtos em grid.
- **RF-008**: Em tablet, o layout DEVE reduzir tipografia/espaçamento, adaptar colunas e oferecer
  filtros recolhíveis sem sobreposição.
- **RF-009**: Em mobile, o header DEVE manter logo, conta e carrinho acessíveis e substituir a
  navegação desktop por um menu apropriado.
- **RF-010**: Em mobile, a busca DEVE ocupar praticamente a largura disponível, categorias DEVEM
  permitir rolagem horizontal ou reorganização, e filtros DEVEM abrir por botão em drawer/modal ou
  superfície equivalente.
- **RF-011**: O grid DEVE adaptar a quantidade de colunas entre desktop, tablet e mobile, usando
  o espaço disponível sem provocar overflow horizontal.
- **RF-012**: Os cards DEVEM manter altura consistente, imagens responsivas sem deformação, nome,
  preço, disponibilidade, link/ação existente e feedback de interação.
- **RF-013**: Imagens de produtos DEVEM preservar proporção com `object-fit: contain` ou
  estratégia equivalente adequada para produtos de tecnologia.
- **RF-014**: Filtros móveis DEVEM oferecer ações claras para abrir, fechar, aplicar e limpar,
  mantendo os mesmos critérios e resultados funcionais.
- **RF-015**: Loading, erro, catálogo vazio e busca sem resultados DEVEM manter mensagens e ações
  existentes, mas usar componentes visuais integrados e proporcionais à página.
- **RF-016**: O erro “O catálogo não pôde ser carregado” DEVE continuar reconhecível, acessível,
  recuperável e integrado à área de resultados.
- **RF-017**: A interface DEVE manter foco visível, labels, `aria-label`, `alt`, contraste,
  navegação por teclado e feedback que não dependa apenas de hover.
- **RF-018**: A interface DEVE respeitar redução de movimento e não deve usar posicionamento
  absoluto ou alturas fixas para estruturar header, hero, filtros ou catálogo.
- **RF-019**: A implementação DEVE reutilizar `App.tsx`, `CatalogPage`, `ProductCard`, estado,
  cliente HTTP, tokens visuais e componentes existentes sempre que possível.
- **RF-020**: A página DEVE ser validada visualmente nas larguras 375, 390, 430, 768, 1024, 1280,
  1440 e 1920 pixels, sem scroll horizontal, corte, sobreposição ou controles fora da viewport.
- **RF-021**: A feature NÃO DEVE implementar checkout, pagamento, pedidos, administração,
  avaliação, desconto, preço anterior, reserva de estoque ou novas regras de negócio.

### Entidades e Dados de Apresentação

- **Produto**: usa os mesmos campos do catálogo funcional; imagem, nome, preço e quantidade não
  são alterados nem recalculados pela UI.
- **Categoria**: usa categorias já carregadas para chips, filtro e contexto, sem criar taxonomia.
- **Consulta do catálogo**: permanece no estado existente; a UI apenas abre, fecha e apresenta
  seus filtros sem duplicar regras.
- **Estado visual**: loading, erro, vazio, sem resultados e sucesso são apresentações do estado
  funcional atual, sem persistência nova.
- **Superfície de filtros**: estado transitório de aberto/fechado/aplicado/limpo; não representa
  uma nova entidade nem modifica o contrato do catálogo.

## Critérios de Sucesso

### Resultados Mensuráveis

- **CS-001**: Em todas as oito larguras de teste definidas, a página apresenta `scrollWidth`
  igual à largura útil da viewport e nenhum elemento essencial ultrapassa seu container.
- **CS-002**: Em desktop, tablet e mobile, a hero ocupa somente a altura necessária para seu
  conteúdo e o início do catálogo aparece sem um vazio vertical desproporcional.
- **CS-003**: 100% das consultas de busca, filtro, ordenação, paginação e detalhes mantêm os
  mesmos parâmetros, resultados e destinos do catálogo anterior.
- **CS-004**: Em mobile, logo, conta, carrinho, menu, busca e botão de filtros permanecem
  alcançáveis por toque e teclado, sem elementos cortados.
- **CS-005**: Cards com imagens, sem imagens, títulos longos e estoque zero mantêm área de mídia,
  altura e link utilizáveis em todas as faixas de viewport.
- **CS-006**: Loading, erro, vazio e sem resultados são visualmente distinguíveis e cada estado
  possui mensagem e ação compatíveis com seu contexto.
- **CS-007**: Todos os controles essenciais têm foco visível, nome acessível e operação por
  teclado, sem depender exclusivamente de hover.
- **CS-008**: Nenhuma chamada ou estado novo altera carrinho, checkout, pagamento, pedido,
  estoque, backend, contratos ou regras de negócio.

## Premissas

- `006-frontend-product-catalog` é a autoridade funcional do catálogo; `007-catalog-redesign` é
  uma referência visual anterior que será corrigida, não recriada do zero.
- O modelo atual não possui rating, preço anterior ou desconto confiáveis; esses dados não serão
  inventados para preencher a UI.
- O carrinho continua fora do escopo e qualquer indicador visual deve permanecer futuro/inativo
  sem contador fictício ou mutação.
- A implementação reutilizará as fontes, cores, ícones, componentes e dependências já existentes;
  novas bibliotecas só seriam consideradas se um bloqueio real aparecer no plano.
- A validação será feita com dados reais quando os serviços estiverem disponíveis e com mocks nos
  testes automatizados, sem mascarar falhas de integração.

## Fora do Escopo

- Recriar o catálogo, sua integração HTTP, store, endpoints ou contratos.
- Alterar Product Service, API Gateway, banco, autenticação, regras de negócio ou persistência.
- Criar checkout, pagamentos, pedidos, administração, avaliações, descontos ou favoritos.
- Adicionar dados fictícios, imagens fictícias ou placeholders que mascarem falhas da API.
- Alterar a página de login além do necessário para não quebrar estilos compartilhados.