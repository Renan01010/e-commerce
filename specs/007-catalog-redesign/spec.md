# Especificação da Feature: Redesign Visual do Catálogo

**Branch da Feature**: `007-catalog-redesign`
**Criado em**: 2026-09-28
**Status**: Draft
**Entrada**: Redesign visual e de experiência da tela de catálogo existente do TechStore, preservando o comportamento funcional já implementado.

## Visão Geral

Esta feature evolui a apresentação do catálogo existente para uma experiência de e-commerce
mais moderna, premium e atraente, sem criar uma nova jornada funcional. O visitante continuará
usando a mesma listagem, pesquisa, filtros, ordenação, paginação e navegação para detalhes já
disponíveis.

O redesign deve parecer uma evolução do TechStore atual: minimalista, tecnológico, espaçoso e
orientado à descoberta de produtos, sem assumir a aparência de dashboard administrativo.

### Direção visual

- Fundo off-white e superfícies claras.
- Tipografia escura, legível e com hierarquia evidente.
- Verde-limão como destaque principal, preservando o papel atual de `--green`.
- Bordas sutis, cantos contidos e composição com espaço visual generoso.
- Movimento discreto em hover, carregamento e entrada de conteúdo, sem prejudicar leitura ou
  reduzir a acessibilidade.
- Reutilização das famílias tipográficas, tokens de cor, componentes e padrões de foco já
  presentes no frontend.

### Limites funcionais

O redesign não altera regras de negócio, Product Service, API Gateway, endpoints, contratos,
pesquisa, filtros, ordenação, paginação ou autenticação. Não implementa carrinho, checkout,
pagamentos, pedidos ou administração.

## Cenários de Usuário e Testes

### História de Usuário 1 - Orientar a navegação da loja (Prioridade: P1)

Como visitante do TechStore, quero reconhecer a marca e encontrar rapidamente catálogo,
categorias, conta e carrinho para entender a estrutura da loja sem esforço.

**Por que esta prioridade**: O cabeçalho é a referência persistente da experiência e organiza as
principais entradas sem modificar as funcionalidades existentes.

**Teste independente**: Abrir o catálogo em desktop, tablet e mobile, navegar por teclado e
confirmar que marca, navegação de produtos/categorias, conta e carrinho permanecem identificáveis
e utilizáveis conforme seu estado atual.

**Cenários de Aceitação**:

1. **Dado** o catálogo aberto, **quando** o visitante visualizar o cabeçalho, **então** verá a
   marca TechStore, uma entrada clara para catálogo/produtos, acesso à conta e uma entrada de
   carrinho visualmente reconhecível.
2. **Dado** que o carrinho ainda não está habilitado, **quando** o visitante interagir com sua
   entrada, **então** nenhuma mutação ou checkout será iniciado e o estado indisponível/futuro
   será comunicado de forma discreta.
3. **Dado** que exista quantidade de itens fornecida por uma integração futura, **quando** o
   cabeçalho for exibido, **então** o indicador poderá apresentar essa quantidade sem alterar
   contratos ou inventar dados na versão atual.
4. **Dado** que o visitante use teclado ou leitor de tela, **quando** percorrer o cabeçalho,
   **então** a ordem de foco, os nomes acessíveis e o foco visível permitirão alcançar cada
   entrada sem ambiguidade.

### História de Usuário 2 - Descobrir o catálogo (Prioridade: P1)

Como visitante, quero entender rapidamente o que a loja oferece e iniciar uma pesquisa para
encontrar produtos relevantes.

**Por que esta prioridade**: A primeira área do catálogo deve reduzir o tempo até a descoberta
sem esconder a grade de produtos ou criar uma landing page separada.

**Teste independente**: Abrir a rota principal, confirmar breadcrumb, título, subtítulo, campo
de pesquisa e acesso visual às categorias; iniciar uma pesquisa e verificar que o comportamento
existente permanece igual.

**Cenários de Aceitação**:

1. **Dado** o visitante na rota principal, **quando** a página terminar de carregar, **então** a
   área inicial apresentará breadcrumb, título forte, subtítulo e campo de pesquisa destacado.
2. **Dado** que categorias estejam disponíveis, **quando** o visitante visualizar a área inicial
   ou a navegação, **então** reconhecerá categorias relevantes sem duplicar o filtro existente.
3. **Dado** que o visitante pesquise pelo campo destacado, **quando** a busca for aplicada,
   **então** os mesmos resultados, parâmetros, debounce e estados da funcionalidade de catálogo
   atual serão preservados.
4. **Dado** que o visitante use uma viewport estreita, **quando** visualizar o hero do catálogo,
   **então** o campo de pesquisa e a ação principal continuarão visíveis sem sobreposição ou
   ocupar toda a primeira tela com conteúdo decorativo.

### História de Usuário 3 - Comparar produtos visualmente (Prioridade: P1)

Como visitante, quero escanear cards de produtos consistentes para comparar opções e abrir um
produto de interesse.

**Por que esta prioridade**: O card é a unidade central de decisão na grade e deve melhorar a
clareza sem mudar os dados ou a navegação que já funcionam.

**Teste independente**: Renderizar produtos com e sem imagem, com estoque e esgotados, abrir um
card e confirmar que os dados e o destino permanecem corretos.

**Cenários de Aceitação**:

1. **Dado** um produto válido, **quando** o card for exibido, **então** a hierarquia priorizará
   imagem ou substituto, categoria/contexto, nome, preço e disponibilidade.
2. **Dado** que avaliação, preço anterior ou desconto estejam presentes em uma fonte já
   disponível, **quando** o card for exibido, **então** esses dados serão apresentados de forma
   secundária e compreensível; quando não estiverem disponíveis, nenhum valor será inventado.
3. **Dado** que o visitante passe o cursor ou foque um card, **quando** o estado de interação
   ocorrer, **então** haverá feedback visual sutil, estável e sem deslocar a grade de maneira
   inesperada.
4. **Dado** que o visitante selecione um card, **quando** a navegação ocorrer, **então** abrirá
   os mesmos detalhes do produto sem iniciar carrinho ou outra operação de compra.
5. **Dado** que a imagem esteja ausente ou falhe, **quando** o card for exibido, **então** um
   substituto visual coerente ocupará o espaço reservado sem quebrar as dimensões da grade.

### História de Usuário 4 - Refinar resultados com pouco atrito (Prioridade: P2)

Como visitante, quero usar filtros e ordenação sem que os controles dominem a tela para manter o
foco nos produtos.

**Por que esta prioridade**: A funcionalidade já existe; o redesign deve reduzir sua carga visual
e tornar seus estados mais fáceis de entender.

**Teste independente**: Aplicar categoria, preço, marca, disponibilidade, busca e ordenação em
desktop e mobile, confirmando que os resultados e a página atual se comportam como antes.

**Cenários de Aceitação**:

1. **Dado** o catálogo carregado, **quando** o visitante visualizar a área de resultados,
   **então** encontrará quantidade de produtos, ordenação, filtros e grade em uma hierarquia
   clara.
2. **Dado** um filtro selecionado, **quando** o controle estiver ativo, **então** seu estado
   selecionado será distinguível por cor, borda, texto ou combinação equivalente sem depender
   apenas de cor.
3. **Dado** que existam filtros ativos, **quando** o visitante selecionar limpar, **então** a
   ação será facilmente localizada e manterá exatamente o comportamento funcional atual.
4. **Dado** um tablet ou mobile, **quando** o visitante abrir os filtros, **então** eles usarão
   espaço contido e continuarão acessíveis sem empurrar a grade para fora da tela.

### História de Usuário 5 - Entender os estados da experiência (Prioridade: P2)

Como visitante, quero que carregamento, erro, catálogo vazio e busca sem resultados tenham uma
apresentação visual clara para saber o que fazer em seguida.

**Por que esta prioridade**: Estados intermediários fazem parte da experiência e não devem criar
grandes áreas vazias ou parecer falhas de layout.

**Teste independente**: Simular cada estado e confirmar que a composição preserva hierarquia,
espaçamento, ação de recuperação e contexto do catálogo.

**Cenários de Aceitação**:

1. **Dado** que produtos estejam carregando, **quando** a resposta ainda não estiver disponível,
   **então** skeletons ou outro feedback consistente ocuparão a área da grade sem saltos bruscos.
2. **Dado** um catálogo vazio ou uma busca sem resultados, **quando** o estado for apresentado,
   **então** haverá mensagem objetiva e ação para limpar ou ajustar critérios sem uma área
   desproporcionalmente vazia.
3. **Dado** um erro de catálogo ou categoria, **quando** a falha for apresentada, **então** o
   aviso seguirá a linguagem visual do redesign, manterá os critérios e oferecerá recuperação.
4. **Dado** que o visitante navegue por teclado, **quando** um estado mudar, **então** o foco e
   a informação anunciada permanecerão compreensíveis sem capturar o teclado.

## Casos de Borda

- Produtos com imagem ausente ou inválida devem manter a mesma proporção visual dos cards.
- Produtos sem avaliação ou preço anterior não devem exibir placeholders que pareçam dados reais.
- O indicador de carrinho não pode sugerir itens quando não houver uma fonte de dados disponível.
- O cabeçalho não pode desaparecer ou ficar ilegível quando o subtítulo for ocultado em mobile.
- Filtros selecionados, erros e busca ativa devem permanecer perceptíveis em contraste e sem
  depender somente de mudança de cor.
- Hover não existe em dispositivos touch; todos os estados essenciais devem funcionar por toque
  e teclado.
- Redução de movimento do sistema deve evitar animações não essenciais ou torná-las discretas.
- Textos longos, nomes de produtos e categorias não podem estourar cards, botões ou controles.
- O redesign não deve criar uma tela vazia quando o catálogo tiver produtos, categorias ou uma
  mensagem de estado disponível.

## Requisitos

### Requisitos Funcionais

- **RF-001**: O redesign DEVE preservar as rotas, navegação e comportamentos funcionais atuais
  do catálogo e dos detalhes de produtos.
- **RF-002**: O redesign DEVE manter o acesso ao catálogo sem autenticação e não pode alterar o
  fluxo de login, cadastro indisponível ou sessão existente.
- **RF-003**: O cabeçalho DEVE apresentar marca TechStore, navegação para catálogo/produtos,
  contexto de categorias, acesso à conta e uma entrada de carrinho coerente com sua
  disponibilidade atual.
- **RF-004**: A entrada de carrinho DEVE permanecer não funcional enquanto o carrinho não estiver
  implementado e não pode criar chamadas, alterações de estado, checkout ou pagamento.
- **RF-005**: O cabeçalho DEVE ser utilizável em desktop, tablet e mobile, com foco visível,
  nomes acessíveis e sem sobreposição.
- **RF-006**: A página de catálogo DEVE apresentar breadcrumb, título, subtítulo, pesquisa
  destacada e possibilidade de reconhecer categorias sem duplicar dados do backend.
- **RF-007**: A pesquisa destacada DEVE reutilizar a busca existente, incluindo parâmetros,
  debounce, paginação, resultados e estados de erro já definidos.
- **RF-008**: A área de catalogação DEVE organizar filtros, quantidade de produtos, ordenação e
  grade em uma hierarquia visual clara e não administrativa.
- **RF-009**: Os filtros existentes DEVEM permanecer disponíveis e funcionais, incluindo
  categoria, preço mínimo/máximo, marca e disponibilidade.
- **RF-010**: Os estados selecionados, limpar filtros, ordenação e paginação DEVEM ser visualmente
  distinguíveis sem alterar suas regras ou contratos.
- **RF-011**: Os cards DEVEM priorizar imagem ou substituto, categoria/contexto, nome, preço e
  disponibilidade, preservando o link existente para detalhes.
- **RF-012**: Avaliação, preço anterior e desconto SOMENTE DEVEM ser exibidos quando dados
  existentes e confiáveis estiverem disponíveis; o frontend não pode inventá-los ou calculá-los
  como regra de negócio.
- **RF-013**: Os cards DEVEM possuir estados de hover, foco e toque com transições suaves que não
  alterem dimensões, ordem ou acessibilidade do conteúdo.
- **RF-014**: Loading, erro, catálogo vazio e busca sem resultados DEVEM compartilhar linguagem
  visual consistente, reduzir espaços vazios excessivos e oferecer ações adequadas.
- **RF-015**: A interface DEVE definir composição específica para desktop, tablet e mobile,
  mantendo a grade, filtros, pesquisa, cabeçalho e mensagens utilizáveis.
- **RF-016**: O redesign DEVE manter foco visível, contraste adequado, navegação por teclado,
  labels, nomes acessíveis e `alt` significativo ou substituto equivalente para imagens.
- **RF-017**: A interface DEVE respeitar preferência de redução de movimento e não depender somente
  de hover para comunicar ações ou estados.
- **RF-018**: A implementação DEVE reutilizar componentes, rotas, estado, cliente HTTP e tokens
  visuais existentes sempre que possível, limitando alterações a apresentação e experiência.
- **RF-019**: Nenhuma alteração desta feature DEVE modificar Product Service, API Gateway,
  endpoints, contratos, regras de busca/filtros, persistência ou modelo de produto.
- **RF-020**: A feature NÃO DEVE implementar checkout, pagamento, pedidos, administração,
  reserva de estoque ou operações de carrinho.

### Entidades e Dados de Apresentação

- **Produto**: usa os mesmos campos já retornados pelo catálogo. Nome, imagem, categoria, preço
  e quantidade permanecem autoritativos; avaliação, preço anterior e desconto são opcionais e
  só aparecem se já existirem em dados confiáveis.
- **Categoria**: usa categorias já carregadas para filtro, navegação e contexto visual; não cria
  uma taxonomia nova.
- **Estado visual**: loading, erro, vazio, sem resultados e sucesso são estados de apresentação
  derivados do estado funcional existente, sem persistência nova.
- **Indicador de carrinho**: elemento visual futuro, sem quantidade inventada e sem mutação nesta
  feature; quando não houver integração disponível, deve indicar estado indisponível de forma
  discreta.

## Critérios de Sucesso

### Resultados Mensuráveis

- **CS-001**: Em testes com as mesmas respostas do catálogo atual, 100% das jornadas de pesquisa,
  filtro, ordenação, paginação e navegação para detalhes preservam seus resultados e destinos.
- **CS-002**: Em viewports de 320 px a 1440 px, 100% dos elementos essenciais do cabeçalho,
  pesquisa, filtros, cards, mensagens e paginação permanecem visíveis, utilizáveis e sem
  sobreposição.
- **CS-003**: Em uma inspeção com produtos contendo os campos disponíveis, 100% dos cards exibem
  nome e preço e exibem imagem, categoria e disponibilidade quando esses dados existem.
- **CS-004**: Em testes de teclado, todos os controles do cabeçalho e catálogo são alcançáveis,
  possuem foco visível e podem ser acionados sem mouse.
- **CS-005**: Loading, erro, catálogo vazio e busca sem resultados apresentam quatro estados
  visualmente distinguíveis, cada um com mensagem e ação compatíveis com seu contexto.
- **CS-006**: O tempo percebido de descoberta não aumenta por chamadas adicionais: o redesign
  utiliza as mesmas requisições e parâmetros funcionais da tela existente.
- **CS-007**: Nenhum cenário do redesign cria ou altera carrinho, checkout, pagamento, pedido,
  estoque, Product Service, API Gateway ou contratos.

## Premissas

- O catálogo funcional da feature `006-frontend-product-catalog` permanece a fonte de verdade
  para rotas, dados, estados, busca, filtros, ordenação e paginação.
- A identidade visual atual em `frontend/src/styles.css` é a base do redesign: off-white,
  verde-limão, tipografia escura, fontes existentes e foco verde profundo.
- `ProductCard`, `CatalogPage`, `App.tsx`, `catalogStore` e `apiClient` serão reutilizados; a
  separação entre apresentação, estado e comunicação deve permanecer.
- O modelo atual não possui avaliação, preço anterior ou desconto. Esses elementos ficam
  condicionais e não serão simulados para preencher o layout.
- O carrinho ainda não possui uma experiência frontend funcional. O header pode reservar um
  ponto de entrada visual, mas não pode prometer uma ação entregue.
- O redesign não exige novas imagens ou dependências externas; ativos existentes ou substitutos
  coerentes devem preservar desempenho e licenciamento do projeto.
- A qualidade visual será validada em desktop, tablet e mobile, além de testes automatizados de
  comportamento e acessibilidade já usados pelo frontend.

## Fora do Escopo

- Alteração de Product Service, API Gateway, endpoints, contratos, banco, modelo ou regras de
  negócio.
- Nova implementação de busca, filtros, ordenação, paginação, autenticação ou sessão.
- Adicionar, editar, remover ou consultar itens do carrinho.
- Checkout, pagamentos, pedidos, descontos como regra de negócio, reserva de estoque e
  administração.
- Criar avaliações, favoritos, comparação ou recomendações personalizadas.
- Inventar avaliações, preços anteriores, descontos, quantidade de itens no carrinho ou qualquer
  dado que não exista em uma fonte confiável.