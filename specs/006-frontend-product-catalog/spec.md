# Especificação da Feature: Catálogo de Produtos no Frontend

**Branch da Feature**: `006-frontend-product-catalog`
**Criado em**: 2026-09-28
**Status**: Draft
**Entrada**: Criar a experiência de navegação pública do catálogo de produtos do e-commerce usando os serviços e padrões existentes do TechStore.

## Visão Geral

Esta feature entrega a experiência de descoberta de produtos para visitantes e clientes do
TechStore. O usuário poderá navegar por uma lista paginada, pesquisar, filtrar, ordenar e
abrir os detalhes de um produto disponível no catálogo.

O frontend consumirá os dados existentes através do API Gateway, que permanece como ponto de
entrada da aplicação. O Product Service continua sendo a autoridade para produtos, categorias,
preços e disponibilidade:

`Frontend -> API Gateway -> Product Service -> PostgreSQL`

O catálogo é público e não exige login para consulta. A autenticação já especificada em
`003-user-auth` e o login do frontend em `005-frontend-login` não serão recriados ou alterados.

## Cenários de Usuário e Testes

### História de Usuário 1 - Explorar produtos (Prioridade: P1)

Como visitante do TechStore, quero visualizar produtos em uma listagem clara para descobrir o
que está disponível para compra.

**Por que esta prioridade**: A listagem é a entrada principal da loja e entrega valor mesmo sem
pesquisa ou autenticação.

**Teste independente**: Abrir a página inicial com produtos ativos disponíveis e confirmar que a
listagem, os cards e a paginação exibem dados coerentes do catálogo.

**Cenários de Aceitação**:

1. **Dado** que o catálogo possui produtos ativos, **quando** o visitante abrir a página inicial,
   **então** a aplicação exibe uma lista paginada de produtos e informa a quantidade total de
   resultados quando fornecida pelo serviço.
2. **Dado** um produto na listagem, **quando** o visitante visualizar seu card, **então** o
   card exibe nome, imagem ou substituto visual, preço, categoria ou contexto de categoria e
   indicação de disponibilidade baseada na quantidade do produto.
3. **Dado** que existem mais resultados que cabem na página, **quando** o visitante navegar
   para outra página, **então** a aplicação carrega a página correspondente sem misturar ou
   duplicar produtos da página anterior.
4. **Dado** que um produto está inativo, **quando** o visitante consultar a listagem,
   **então** esse produto não é apresentado.

### História de Usuário 2 - Encontrar e refinar produtos (Prioridade: P1)

Como visitante, quero pesquisar, filtrar e ordenar os produtos para encontrar opções relevantes
com menos esforço.

**Por que esta prioridade**: Catálogos maiores exigem mecanismos de descoberta além da
navegação visual.

**Teste independente**: Aplicar cada critério de pesquisa, filtro e ordenação em um catálogo
controlado e confirmar que os resultados correspondem aos parâmetros escolhidos e que a
paginação volta ao início quando o conjunto de resultados muda.

**Cenários de Aceitação**:

1. **Dado** produtos cujo nome, marca ou descrição contém um termo, **quando** o visitante
   pesquisar por esse termo, **então** a listagem apresenta os resultados retornados pela busca
   do catálogo e indica o contexto da pesquisa.
2. **Dado** que o visitante escolhe uma categoria, faixa de preço, marca ou a opção de somente
   produtos disponíveis, **quando** os resultados forem atualizados, **então** somente produtos
   compatíveis com todos os filtros selecionados são apresentados.
3. **Dado** que o visitante escolhe uma ordenação, **quando** os resultados forem atualizados,
   **então** a ordem exibida corresponde à opção escolhida entre relevância, menor preço, maior
   preço, nome ou produtos mais recentes.
4. **Dado** que o visitante altera pesquisa, filtro ou ordenação enquanto está em outra página,
   **quando** a nova consulta for aplicada, **então** a aplicação retorna à primeira página do
   novo conjunto de resultados.
5. **Dado** que filtros ou pesquisa estão ativos, **quando** o visitante selecionar limpar,
   **então** os critérios são removidos e a listagem volta ao catálogo sem esses refinamentos.

### História de Usuário 3 - Consultar detalhes de um produto (Prioridade: P1)

Como visitante, quero abrir um produto específico para avaliar seus detalhes antes de decidir
se tenho interesse nele.

**Por que esta prioridade**: A listagem permite descoberta, mas a decisão depende de informações
mais completas e de uma navegação clara entre resumo e detalhe.

**Teste independente**: Selecionar um card, confirmar a abertura do detalhe correspondente,
validar seus dados e retornar ao catálogo preservando uma navegação compreensível.

**Cenários de Aceitação**:

1. **Dado** um produto exibido na listagem, **quando** o visitante selecionar seu card,
   **então** a aplicação abrirá a página de detalhes do mesmo produto.
2. **Dado** um produto ativo encontrado pelo identificador, **quando** a página de detalhes
   carregar, **então** ela exibirá nome, descrição completa quando disponível, preço, imagem ou
   substituto, marca ou referência, categoria e disponibilidade/quantidade conforme o catálogo.
3. **Dado** que o produto pertence a uma categoria, **quando** o visitante visualizar o
   detalhe, **então** poderá reconhecer a categoria e navegar de volta ao catálogo filtrado por
   essa categoria.
4. **Dado** que o visitante está no detalhe, **quando** selecionar voltar ao catálogo,
   **então** retornará a uma rota navegável do catálogo sem iniciar carrinho ou checkout.
5. **Dado** um identificador inexistente, inválido ou de produto inativo, **quando** o visitante
   abrir a rota de detalhe, **então** verá uma mensagem de produto não encontrado e uma ação
   para voltar ao catálogo.

### História de Usuário 4 - Entender os estados do catálogo (Prioridade: P2)

Como visitante, quero receber feedback claro enquanto o catálogo carrega, quando não há
resultados ou quando ocorre uma falha para saber o que fazer em seguida.

**Por que esta prioridade**: Feedback previsível evita que uma tela vazia seja confundida com
falha e mantém a jornada recuperável.

**Teste independente**: Simular carregamento, resposta vazia, falha de rede e falha do serviço
para confirmar que cada estado é distinguível e possui uma ação adequada quando possível.

**Cenários de Aceitação**:

1. **Dado** que uma consulta está em andamento, **quando** o visitante aguardar a resposta,
   **então** a aplicação exibirá um estado de carregamento e não apresentará resultados parciais
   como se fossem a resposta final.
2. **Dado** que a consulta terminou sem produtos, **quando** a aplicação apresentar o resultado,
   **então** exibirá um estado vazio que informa a ausência de resultados e sugere ajustar ou
   limpar pesquisa e filtros.
3. **Dado** que o catálogo ou as categorias não podem ser carregados, **quando** a falha for
   identificada, **então** a aplicação exibirá uma mensagem compreensível, sem detalhes
   internos, e permitirá nova tentativa ou retorno a uma área disponível.
4. **Dado** que o detalhe falhou ao carregar, **quando** a aplicação apresentar o erro,
   **então** o visitante não verá dados inventados ou de outro produto e poderá voltar ao
   catálogo.

## Casos de Borda

- Uma imagem ausente, inválida ou que falha ao carregar não pode quebrar o card ou o detalhe;
  deve existir uma apresentação alternativa identificável.
- Um produto com quantidade igual a zero deve ser apresentado como indisponível, sem ser
  confundido com produto inexistente.
- Um preço igual a zero deve ser exibido como valor válido, e não como ausência de preço.
- Uma faixa de preço inválida ou com mínimo maior que máximo deve ser rejeitada ou corrigida
  antes de apresentar resultados enganosos.
- Uma pesquisa sem correspondências deve usar o estado vazio, não o estado de erro.
- Uma resposta de erro não pode apagar silenciosamente os critérios que o visitante escolheu.
- A ausência temporária de categorias não deve expor IDs como se fossem nomes amigáveis; a
  aplicação deve sinalizar o problema ou usar um contexto neutro.
- Cliques repetidos no mesmo card durante o carregamento não devem criar navegações concorrentes
  que exibam detalhes de produtos diferentes.
- Em telas estreitas, filtros, busca, ordenação, cards, mensagens e paginação devem continuar
  utilizáveis sem sobreposição ou corte.

## Requisitos

### Requisitos Funcionais

- **RF-001**: O frontend DEVE disponibilizar uma rota de catálogo acessível sem autenticação e
  uma rota de detalhes para um produto identificado pelo catálogo.
- **RF-002**: O frontend DEVE consultar os produtos ativos através do API Gateway, sem acessar
  diretamente o banco de dados ou criar uma fonte local de verdade para o catálogo.
- **RF-003**: A listagem DEVE consumir e respeitar a paginação retornada pelo serviço, incluindo
  página atual, total de resultados, total de páginas e indicação de mais resultados quando
  disponíveis.
- **RF-004**: Cada card DEVE apresentar, quando fornecidos pelo catálogo, nome, imagem,
  categoria, preço e estado de disponibilidade baseado na quantidade; não deve apresentar o
  custo interno do produto.
- **RF-005**: A aplicação DEVE obter e apresentar categorias usando o recurso de categorias
  existente, respeitando a relação pai/filho quando ela for fornecida.
- **RF-006**: O visitante DEVE poder pesquisar por um termo que será encaminhado ao mecanismo de
  busca existente para nome, marca e descrição.
- **RF-007**: O visitante DEVE poder filtrar por categoria, preço mínimo, preço máximo, marca e
  disponibilidade em estoque.
- **RF-008**: A aplicação DEVE combinar filtros selecionados de forma cumulativa e encaminhar
  somente os filtros preenchidos, sem inventar critérios não suportados pelo contrato existente.
- **RF-009**: O visitante DEVE poder ordenar por relevância, preço, nome e data de inclusão,
  escolhendo a direção quando aplicável ao contrato existente.
- **RF-010**: A alteração de pesquisa, filtro ou ordenação DEVE reiniciar a navegação na primeira
  página dos resultados correspondentes.
- **RF-011**: A aplicação DEVE oferecer uma ação para limpar pesquisa e filtros e retornar aos
  resultados sem refinamentos.
- **RF-012**: Cada item da listagem DEVE oferecer navegação para os detalhes do produto sem
  alterar o estado do carrinho ou iniciar uma operação de compra.
- **RF-013**: A página de detalhes DEVE apresentar, quando disponíveis, nome, descrição, preço,
  imagem, marca, referência, categoria e quantidade/estado de disponibilidade do produto
  retornado pelo serviço.
- **RF-014**: A aplicação DEVE permitir voltar do detalhe para o catálogo e oferecer contexto
  de categoria navegável quando o produto possuir categoria válida.
- **RF-015**: Durante qualquer consulta de listagem, categorias ou detalhes, a aplicação DEVE
  apresentar um estado de carregamento identificável e impedir que dados antigos sejam
  confundidos com a resposta atual.
- **RF-016**: Quando uma consulta válida não retornar produtos, a aplicação DEVE apresentar um
  estado vazio distinto de erro, com orientação para ajustar ou limpar os critérios.
- **RF-017**: Quando o Gateway ou Product Service falhar, a aplicação DEVE apresentar feedback
  seguro e compreensível, preservar os critérios selecionados quando possível e oferecer uma
  forma de nova tentativa ou retorno.
- **RF-018**: A aplicação DEVE tratar produto inexistente, inválido ou inativo no detalhe como
  estado de produto não encontrado, sem exibir dados de outro produto.
- **RF-019**: A apresentação DEVE formatar preços de forma consistente para o público brasileiro
  e manter a precisão do valor recebido, sem recalcular preço no frontend.
- **RF-020**: A experiência DEVE ser utilizável por teclado e tecnologias assistivas, com nomes
  compreensíveis para busca, filtros, ordenação, cards, estados e paginação.
- **RF-021**: A experiência DEVE permanecer utilizável em larguras desktop e mobile, mantendo
  ações, conteúdo e mensagens visíveis sem sobreposição ou corte.
- **RF-022**: O catálogo DEVE reutilizar os padrões visuais, de navegação e de sessão já
  existentes no frontend, sem alterar o comportamento definido para login, cadastro indisponível
  ou autenticação.
- **RF-023**: Esta feature DEVE manter o API Gateway como ponto de entrada entre o frontend e o
  Product Service e não deve criar ou alterar endpoints de produtos e categorias já existentes.
- **RF-024**: A feature NÃO DEVE habilitar adicionar ao carrinho, alterar/remover itens, checkout,
  pagamento, pedidos, administração de produtos/categorias ou reserva de estoque.

### Contrato de Dados Consumido

O catálogo reutiliza os recursos públicos existentes:

| Recurso | Uso na experiência |
| --- | --- |
| `GET /api/products` | Listagem paginada, pesquisa, filtros e ordenação |
| `GET /api/categories` | Nomes e hierarquia para seleção e contexto de categoria |
| `GET /api/products/{id}` | Detalhes do produto ativo selecionado |

Os parâmetros de listagem são `query`, `categoryId`, `minPrice`, `maxPrice`, `inStock`, `brand`,
`sortBy`, `sortOrder`, `page` e `pageSize`, conforme o contrato do Product Service. A resposta
de listagem fornece `content`, `totalElements`, `totalPages`, `currentPage`, `pageSize` e
`hasMore`. O catálogo não deve expor `cost` ao cliente final, mesmo que o serviço o conheça.

### Entidades Principais

- **Produto**: item ativo do catálogo, identificado por UUID, com nome, descrição, preço, marca,
  referência, categoria, imagem, quantidade e datas de atualização.
- **Categoria**: agrupamento do catálogo com nome e possível categoria pai, usado para filtrar e
  contextualizar produtos.
- **Consulta do catálogo**: combinação transitória de pesquisa, filtros, ordenação e página
  escolhida pelo visitante; não é uma nova entidade persistida.
- **Estado de disponibilidade**: apresentação derivada da quantidade retornada pelo Product
  Service; quantidade maior que zero significa disponível e quantidade igual a zero significa
  esgotado/indisponível.

## Critérios de Sucesso

### Resultados Mensuráveis

- **CS-001**: Em uma sessão sem autenticação, 100% das consultas de listagem, categorias e
  detalhes válidas são iniciadas pela rota pública do Gateway e exibem os dados retornados pelo
  Product Service sem acesso direto ao banco.
- **CS-002**: Em cenários de teste com produtos ativos, 100% dos cards exibem nome e preço, e
  exibem imagem, categoria e disponibilidade quando esses dados estão disponíveis na resposta.
- **CS-003**: 100% das combinações suportadas de pesquisa, filtros e ordenação produzem uma nova
  consulta com os critérios escolhidos, sem manter resultados de uma página anterior como se
  fossem atuais.
- **CS-004**: 100% dos produtos selecionados a partir da listagem abrem o detalhe do mesmo
  identificador, e 100% dos identificadores inexistentes/inativos exibem um estado seguro de
  não encontrado.
- **CS-005**: Todos os estados de carregamento, vazio e erro possuem uma apresentação distinta;
  falhas de rede ou serviço oferecem nova tentativa ou retorno em até uma ação do visitante.
- **CS-006**: Em testes de viewport entre 320 px e 1440 px, nenhuma ação essencial do catálogo
  ou detalhe fica oculta, cortada ou sobreposta.
- **CS-007**: Todos os controles interativos do catálogo e do detalhe podem ser alcançados e
  acionados por teclado, com nome acessível e feedback textual para estados de erro e vazio.
- **CS-008**: Nenhuma ação da feature cria ou altera carrinho, pedido, pagamento, estoque ou
  dados administrativos do catálogo.

## Premissas

- O Product Service e os contratos descritos em `001-foundation-product-catalog` permanecem
  disponíveis e são a fonte de verdade para produtos, categorias, preços e quantidade.
- O API Gateway já encaminha `/api/products/**` e `/api/categories/**` ao Product Service; esta
  feature não modifica esse roteamento.
- As consultas públicas não exigem JWT. Quando existir sessão de login, ela continua sendo
  gerenciada pelo mecanismo definido em `005-frontend-login`, sem tornar o catálogo dependente de
  autenticação.
- A moeda de apresentação permanece o Real brasileiro, conforme o padrão visual atual do
  frontend e o valor monetário retornado pelo serviço.
- A imagem de um produto é opcional; quando ausente ou indisponível, o frontend usa um estado
  visual alternativo sem fabricar uma imagem do produto.
- A disponibilidade apresentada nesta feature é informativa e deriva de `quantity`; não reserva
  nem garante estoque para uma compra futura.
- A especificação descreve comportamento do produto. Decisões sobre componentes, gerenciamento
  de estado, cache, debounce, testes e detalhes de implementação serão definidas no plano e nas
  tarefas subsequentes, respeitando os padrões existentes.

## Fora do Escopo

- Criar, alterar ou duplicar endpoints, contratos, regras de negócio ou persistência do Product
  Service e do API Gateway.
- Cadastro, login, logout, renovação ou persistência de sessão de usuário.
- Adição, alteração, remoção ou consulta de itens do carrinho.
- Checkout, pedidos, pagamentos, faturamento, descontos e notificações.
- Reserva, dedução, sincronização ou validação transacional de estoque.
- Administração de produtos ou categorias.
- Recomendação personalizada, avaliação, favoritos e comparação de produtos.