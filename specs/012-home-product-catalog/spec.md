# Especificação da Feature: Home e Catálogo de Produtos

**Branch da Feature**: `012-home-product-catalog`
**Criado em**: 2026-10-03
**Status**: Draft
**Entrada**: Implementar uma Home responsiva para o TechStore que consuma os produtos e as categorias existentes pelo API Gateway e reutilize o fluxo existente de carrinho, sem alterações backend.

## Visão Geral

A Home será a principal entrada da loja e combinará identidade visual tecnológica escura, navegação por categorias, conteúdo comercial configurado no frontend e produtos reais do catálogo. O visitante poderá seguir para o catálogo completo, pesquisar, filtrar, ordenar, paginar e abrir detalhes, sem perder o fluxo existente de autenticação e carrinho.

O frontend é exclusivamente consumidor das APIs existentes:

`React -> estado/store e API client existentes -> API Gateway -> Product Service / Cart Service`

O Product Service continua sendo a fonte dos dados apresentados no catálogo. O Cart Service continua sendo a fonte de verdade de quantidade, preço, subtotal, total, disponibilidade e limites no carrinho. Esta feature não altera backend, contratos, infraestrutura nem regras de domínio.

## Referência Visual

A referência principal para composição desktop é `references/TechStore_ Tecnologia sem limites.png`. O caminho `references/home-desktop-reference.png` indicado inicialmente não existe na feature com esse nome; a imagem fornecida está no primeiro caminho.

Antes de implementar tarefas de layout da Home, consultar a imagem para orientar a composição do header em duas faixas, Hero tecnológico amplo com CTA e benefícios, navegação visual em cards horizontais de categoria, grade de produtos e banners secundários. Usar a referência para hierarquia, proporção, espaçamento, paleta dark com acentos azul/roxo e verde/lima, bordas, densidade e linguagem visual, adaptando-a à arquitetura existente e a telas menores.

Não copiar da imagem texto comercial, percentuais ou outras condições promocionais, avaliações, badges de venda/novidade, produtos, imagens de produto, preços, categorias ou dados de disponibilidade. Conteúdo de catálogo e categoria vem das APIs verificadas; texto editorial/benefícios devem seguir a especificação e não representar promoções ou capacidades backend inexistentes.

## Contrato Atual Verificado e Divergências

Os endpoints e campos abaixo foram verificados na implementação disponível e nos clientes já existentes no frontend. Nenhuma rota ou campo novo deve ser presumido:

- `GET /api/products` aceita `query`, `categoryId`, `minPrice`, `maxPrice`, `inStock`, `brand`, `sortBy`, `sortOrder`, `page` e `pageSize`. A ordenação aceita `relevance`, `price`, `name` e `newest`, com `asc` ou `desc`; a página começa em zero e o tamanho máximo é 100. A resposta paginada fornece `content`, `totalElements`, `totalPages`, `currentPage`, `pageSize` e `hasMore`.
- `GET /api/products/{id}` retorna um produto ativo. Os dados consumíveis incluem `id`, `name`, `description`, `price`, `brand`, `sku`, `categoryId`, `quantity`, `imageUrl`, `isActive`, `createdAt` e `updatedAt`. `cost` é informação administrativa e não pode ser apresentada como dado comercial.
- `GET /api/categories` retorna categorias ativas com `id`, `name`, `slug`, `description`, `parentCategoryId`, `displayOrder`, `isActive`, `createdAt` e `updatedAt`. Não há campo de imagem de categoria; o frontend pode usar representação visual genérica sem inventar categorias. A navegação deve usar categorias retornadas por esta API, ordenadas por `displayOrder`, nunca uma lista fictícia como fonte de verdade.
- O Product Service não fornece preço anterior, desconto, avaliação, indicador de produto destacado, rótulo promocional nem imagem de categoria no contrato de leitura verificado. Esses dados não podem ser inventados ou inferidos.
- O cliente HTTP compartilhado usa `VITE_API_URL` (padrão `/api`) e pode anexar a sessão existente. A leitura pública do catálogo não deve depender de autenticação.
- As operações existentes de carrinho no cliente compartilhado são `GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/{productId}`, `DELETE /api/cart/items/{productId}` e `DELETE /api/cart`. A adição usa a ação/store compartilhada existente; a resposta do serviço substitui o estado confirmado e a resposta/erro do backend determina o resultado. Remoções retornam `204` sem corpo.

**Divergência documentada**: `specs/009-category-management/contracts/category-management-api.md` descreve um parâmetro `search` em `GET /api/categories`, porém o `CategoryController` atual não recebe parâmetros nessa rota. A Home não deve enviar esse parâmetro nem depender de busca de categorias no servidor. O `CategoryResponse` atual também inclui `slug`, ausente da interface TypeScript atual; esta feature não precisa do campo e não deve ampliar o contrato do frontend sem necessidade.

**Escopo da especificação**: seguir os DTOs/controllers atuais e os clientes frontend existentes, sem modificar ou sincronizar documentação OpenAPI/backend como parte desta feature. Os documentos [006-frontend-product-catalog](../006-frontend-product-catalog/spec.md), [011-frontend-cart-completion](../011-frontend-cart-completion/spec.md) e seus documentos de consumo registram capacidades já existentes que devem ser reutilizadas.

## Cenários de Usuário e Testes

### História de Usuário 1 - Descobrir a loja pela Home (Prioridade: P1)

Como visitante, quero abrir uma Home visualmente atrativa e responsiva para conhecer a TechStore, suas categorias e produtos reais e seguir para uma compra.

**Por que esta prioridade**: A Home é a porta de entrada principal e precisa entregar descoberta de produtos sem exigir autenticação nem inventar dados comerciais.

**Teste independente**: Renderizar a rota inicial com respostas mockadas de categorias e produtos, verificar as seções da Home, os dados recebidos, os links de navegação e os estados de carregamento, vazio e falha.

**Cenários de Aceitação**:

1. **Dado** que a aplicação seja aberta na rota inicial, **quando** a Home for apresentada, **então** ela exibirá identidade visual TechStore predominantemente escura, destaque visual em verde/lima, CTA para o catálogo e conteúdo comercial configurado no frontend, sem depender de CMS.
2. **Dado** que existam categorias ativas, **quando** a Home carregar, **então** a navegação de categorias usará seus nomes e IDs reais, respeitará `displayOrder` e permitirá abrir o catálogo filtrado pela categoria selecionada.
3. **Dado** que a resposta de categoria não contenha imagens, **quando** as categorias forem apresentadas, **então** cada uma terá representação visual genérica apropriada sem imagem, nome ou ID fictício.
4. **Dado** que existam produtos no catálogo, **quando** a Home carregar, **então** a seção “Produtos em destaque” mostrará até oito produtos reais mais recentes, consultados com `sortBy=newest` e `sortOrder=desc`, sem alegar popularidade ou destaque editorial fornecido pelo serviço.
5. **Dado** um produto exibido na Home, **quando** o visitante abrir seus detalhes, **então** verá a rota de detalhe existente para o mesmo produto.
6. **Dado** que o visitante selecione um CTA de banner secundário, **quando** navegar, **então** chegará ao catálogo com filtro correspondente somente quando houver categoria real compatível; não será apresentada como verdadeira nenhuma oferta, desconto ou campanha comercial fictícia.
7. **Dado** que o visitante abra o menu em uma tela pequena, **quando** navegar ou selecionar um destino, **então** poderá acessar a navegação, conta e carrinho por controles utilizáveis por toque e teclado.

### História de Usuário 2 - Encontrar produtos no catálogo completo (Prioridade: P1)

Como visitante, quero pesquisar, filtrar, ordenar e paginar o catálogo completo para encontrar produtos reais e abrir os detalhes que me interessam.

**Por que esta prioridade**: A Home promove descoberta, mas o catálogo completo existente é necessário para dar acesso navegável ao inventário de produtos oferecido pela API.

**Teste independente**: Usar mocks do cliente de catálogo para verificar consultas e resultados ao pesquisar, filtrar por categoria, ordenar e mudar de página; verificar também abertura do detalhe e os estados vazio e erro.

**Cenários de Aceitação**:

1. **Dado** que o visitante escolha “Ver catálogo” ou submeta uma busca, **quando** a navegação ocorrer, **então** a aplicação apresentará o catálogo completo em rota navegável e preservará a busca submetida na URL.
2. **Dado** que o visitante digite um termo de busca, **quando** o debounce definido no plano futuro expirar ou a busca for submetida, **então** a aplicação enviará `query` ao endpoint existente, sem disparar requisições redundantes por cada tecla.
3. **Dado** que o visitante escolha uma categoria real, marca, faixa de preço ou filtro de disponibilidade, **quando** os resultados forem atualizados, **então** a API existente receberá apenas os filtros suportados e a paginação será reiniciada na primeira página.
4. **Dado** que o visitante selecione uma ordenação ou direção suportada, **quando** os resultados forem atualizados, **então** serão enviados valores permitidos pelo Product Service e a paginação será reiniciada na primeira página.
5. **Dado** que existam páginas subsequentes, **quando** o visitante paginar, **então** o conteúdo apresentado corresponderá à página retornada pelo serviço, sem duplicação ou mistura com resultados anteriores.
6. **Dado** um produto retornado pela API, **quando** o card for apresentado, **então** poderá incluir imagem ou substituto visual, nome, marca, preço e disponibilidade conforme os dados recebidos; UUIDs não serão exibidos.
7. **Dado** que `imageUrl` esteja ausente ou falhe, **quando** o card for apresentado, **então** um substituto visual acessível será exibido sem quebrar a navegação.
8. **Dado** que o visitante abra um produto, **quando** a rota de detalhe carregar, **então** a página existente apresentará os dados retornados pelo serviço ou seu estado de carregamento, erro ou não encontrado.

### História de Usuário 3 - Adicionar um produto ao carrinho (Prioridade: P1)

Como cliente autenticado, quero adicionar um produto pelo fluxo existente e receber confirmação coerente com o carrinho, sem divergência de quantidade ou valor.

**Por que esta prioridade**: A ação comercial primária da Home só é confiável quando reutiliza a integração compartilhada e mantém o badge sincronizado com o estado confirmado.

**Teste independente**: Mockar o store/API de carrinho, acionar “Adicionar ao carrinho” em um card e verificar estado pendente, chamada única, aplicação da resposta confirmada, feedback e erro.

**Cenários de Aceitação**:

1. **Dado** um produto apresentado na Home ou catálogo, **quando** o visitante acionar “Adicionar ao carrinho”, **então** a ação usará a operação e o store existentes, sem implementar regra de preço, quantidade, estoque ou autorização no componente.
2. **Dado** que uma adição esteja pendente, **quando** o botão for acionado novamente, **então** a interface impedirá POSTs duplicados e comunicará visualmente o estado de carregamento.
3. **Dado** que o serviço confirme uma adição, **quando** a resposta for aplicada, **então** o badge do cabeçalho refletirá o estado confirmado compartilhado e o visitante receberá feedback acessível.
4. **Dado** que o serviço rejeite a adição, **quando** a resposta de erro for recebida, **então** a interface exibirá orientação amigável, preservará o estado confirmado anterior e não apresentará sucesso ou valores calculados localmente.
5. **Dado** que o visitante não esteja autenticado, **quando** visualizar a ação de adicionar, **então** receberá orientação e acesso ao login e nenhuma mutação de carrinho será enviada.
6. **Dado** que a sessão expire durante a operação, **quando** o serviço rejeitar a adição por autenticação, **então** o fluxo existente de sessão/login será respeitado sem persistir JWT em `localStorage` por esta feature.

### História de Usuário 4 - Compreender carregamento, vazio e falhas (Prioridade: P1)

Como visitante, quero reconhecer carregamentos, resultados vazios e falhas de serviço e ter um próximo passo claro, sem confundir uma tela vazia com uma falha.

**Por que esta prioridade**: A Home depende de APIs e a descoberta deve continuar compreensível e recuperável durante ausência de conteúdo ou indisponibilidade.

**Teste independente**: Simular carregamento de produtos/categorias, resposta sem resultados e respostas HTTP/rede com erro, verificando mensagens e ações de recuperação.

**Cenários de Aceitação**:

1. **Dado** que produtos ou categorias estejam carregando, **quando** a Home for exibida, **então** haverá skeleton ou estado visual equivalente sem tela completamente vazia.
2. **Dado** que categorias falhem ao carregar, **quando** a Home for apresentada, **então** a falha terá mensagem segura e uma opção de tentar novamente, sem substituir dados ausentes por categorias inventadas.
3. **Dado** que o catálogo não retorne produtos, **quando** a listagem for apresentada, **então** mostrará mensagem amigável de nenhum produto encontrado e ação para remover refinamentos ou retornar ao catálogo.
4. **Dado** que uma requisição de catálogo falhe, **quando** a falha for apresentada, **então** a mensagem será compreensível, permitirá tentar novamente e não exporá stack trace, UUID interno ou detalhe técnico desnecessário.
5. **Dado** que o Gateway ou Product Service esteja indisponível, **quando** a requisição falhar por rede ou erro de servidor, **então** a interface indicará indisponibilidade temporária sem apresentar dados fictícios como sucesso.
6. **Dado** um status HTTP `400`, `401`, `403`, `404`, `409`, `429` ou `5xx`, **quando** a API retornar a falha, **então** a camada de consumo apresentará mensagem segura e adequada; status não previstos no contrato atual usarão fallback amigável, sem afirmar que o Product Service os emite.

### História de Usuário 5 - Navegar com conforto em diferentes telas (Prioridade: P2)

Como visitante, quero utilizar Home, navegação, busca, catálogo e carrinho em desktop, tablet e celular, com teclado e tecnologias assistivas.

**Por que esta prioridade**: A experiência precisa preservar sua intenção visual sem excluir dispositivos menores ou modos de navegação acessíveis.

**Teste independente**: Verificar layout e comportamento crítico em larguras representativas de desktop, tablet e mobile, além de foco por teclado, rótulos e nomes acessíveis.

**Cenários de Aceitação**:

1. **Dado** viewport desktop, **quando** a Home for exibida, **então** o Hero terá destaque amplo, navegação completa e grade com múltiplos cards.
2. **Dado** viewport tablet, **quando** a Home e o catálogo forem exibidos, **então** o grid e a navegação se adaptarão sem corte ou rolagem horizontal da página.
3. **Dado** viewport mobile, **quando** a Home e o catálogo forem exibidos, **então** o menu poderá ser aberto, as categorias terão navegação horizontal ou equivalente, o Hero e a busca se adaptarão e os cards terão tamanho e ações adequados ao toque.
4. **Dado** que o visitante navegue por teclado, **quando** focar links, campos, filtros e botões, **então** encontrará foco visível e poderá operar as ações sem depender de hover.
5. **Dado** que imagens ou estados sejam apresentados, **quando** um leitor de tela os anunciar, **então** imagens terão texto alternativo apropriado, campos terão labels e estados dinâmicos usarão semântica acessível.
6. **Dado** um estado de erro, disponibilidade ou carregamento, **quando** for comunicado, **então** não dependerá somente de cor para ser entendido.

### Edge Cases

- A consulta de produtos retorna lista vazia, páginas zero ou resultado vazio após um filtro.
- A consulta de categorias falha enquanto produtos continuam disponíveis, ou categorias retornam sem hierarquia/sem imagens.
- Uma categoria é selecionada e removida/desativada antes de a consulta de produto ser concluída.
- Uma busca nova ou uma troca rápida de filtros/página torna obsoleta uma resposta em andamento.
- Um produto não possui `imageUrl` ou uma imagem remota não pode ser carregada.
- Um produto é removido/inativado ou tem disponibilidade alterada entre leitura do catálogo e tentativa de adição; a resposta do Cart Service decide o resultado.
- Uma operação de adição falha com `401`, `403`, `404`, `409`, `429`, `5xx` ou erro de rede; o estado confirmado do carrinho não é substituído por uma previsão local.
- O cliente de categorias recebe um campo `slug` que a interface frontend tipada atual não declara; a Home continua usando somente campos conhecidos necessários.
- Não há categoria real adequada para um banner secundário; seu CTA deverá abrir o catálogo sem aplicar filtro fictício.

## Requisitos

### Requisitos Funcionais

- **FR-001**: A rota inicial `/` MUST apresentar a Home TechStore como principal entrada pública; o catálogo completo MUST ser acessível em `/catalog`, preservando as rotas existentes `/products/:id`, `/login` e `/cart`.
- **FR-002**: A Home MUST seguir a direção visual descrita pelo usuário: tema predominantemente escuro, contraste elevado, estética tecnológica, tipografia limpa, cards arredondados e destaques em verde/lima com acentos azul/roxo.
- **FR-003**: O conteúdo de Hero e banners MUST ser configurável no frontend, sem CMS, sem alegar promoções/descontos reais ausentes da API e sem dados de produto fictícios.
- **FR-003a**: Benefícios MUST ser apresentados somente como informações institucionais e não podem afirmar prazos/abrangência de entrega, condições específicas de parcelamento, proteção/garantias de compra ou disponibilidade de suporte que não estejam confirmados por uma fonte existente; não reproduzir claims comerciais da imagem de referência.
- **FR-004**: A navegação de categorias MUST ser derivada exclusivamente de categorias retornadas por `GET /api/categories`; nomes e IDs fictícios não podem servir de fonte de verdade.
- **FR-005**: A UI MUST ordenar categorias pelo `displayOrder` retornado e MUST filtrar o catálogo usando `categoryId` real, sem enviar o parâmetro `search` não suportado por `GET /api/categories`.
- **FR-006**: A seção “Produtos em destaque” MUST exibir até oito produtos reais mais recentes carregados por `GET /api/products` na primeira página com `sortBy=newest` e `sortOrder=desc`; o rótulo da seção não deve implicar que a API forneça um campo de destaque, popularidade ou classificação editorial.
- **FR-007**: Cards MUST apresentar apenas informações disponíveis no produto retornado, incluindo nome, marca, preço, imagem ou substituto visual e indicador de disponibilidade conforme os campos de catálogo.
- **FR-008**: A interface MUST NOT inventar preço anterior, desconto, avaliação, estoque, ranking, badge “Novo”, “Oferta” ou “Mais vendido”; custo administrativo MUST NOT ser apresentado como preço comercial.
- **FR-009**: A Home MUST permitir acesso ao catálogo completo em `/catalog`, reutilizando capacidades existentes de pesquisa, filtros suportados, ordenação, paginação, carregamento, vazio, erro e detalhe.
- **FR-010**: A busca do cabeçalho MUST encaminhar o termo ao catálogo pelo parâmetro `query`, sincronizar mudanças de busca do catálogo com a URL sem chamadas por tecla desnecessárias e preservar o comportamento de busca existente.
- **FR-011**: A seleção de categoria MUST atualizar o catálogo e a URL com seu `categoryId`; `query` e `categoryId` presentes na URL são a fonte de verdade desses dois critérios, e a ausência de qualquer parâmetro limpa o critério correspondente do store. Navegação voltar/avançar MUST restaurar esses dois valores. Alterações de consulta, filtros ou ordenação MUST reiniciar a paginação na primeira página; demais filtros, ordenação e página não precisam ser serializados nesta feature.
- **FR-012**: A seleção de um card MUST abrir os detalhes do mesmo produto pela rota de detalhe existente e MUST comunicar carregamento, falha ou produto não encontrado.
- **FR-013**: Os cards MUST fornecer a clientes autenticados uma ação “Adicionar ao carrinho” que utilize o store e o cliente compartilhados existentes, sem regra de domínio ou cálculo financeiro em componentes; visitantes não autenticados devem receber acesso ao login sem tentativa de mutação.
- **FR-014**: Uma adição pendente MUST impedir duplicação acidental da mesma ação, mostrar estado de carregamento e tratar a resposta/erro do Cart Service.
- **FR-015**: Após sucesso de carrinho, o badge MUST representar o estado confirmado já compartilhado pelo Cart Service; erro MUST preservar o último estado confirmado e apresentar feedback acessível.
- **FR-016**: O cabeçalho MUST preservar identidade TechStore, busca, navegação responsiva, conta e carrinho com contador existente; favoritos só podem ser mostrados se suporte real já existir.
- **FR-017**: Banners promocionais secundários podem ser conteúdo visual estático frontend, mas MUST NOT alegar descontos ou campanhas reais inexistentes; seus CTAs MUST levar a uma rota ou filtro existente.
- **FR-018**: Estados de carregamento de categorias, catálogo, detalhe de produto e adição ao carrinho MUST permanecer visíveis e compreensíveis; skeletons devem ser usados onde apropriado.
- **FR-019**: Estados vazios MUST explicar a ausência de resultados e oferecer ação coerente, sem valores ou produtos fictícios.
- **FR-020**: Erros `400`, `401`, `403`, `404`, `409`, `429`, `5xx` e indisponibilidade/rede MUST resultar em mensagens amigáveis, sem stack traces, UUIDs ou detalhes internos; a mensagem deve respeitar o mapeamento existente quando aplicável.
- **FR-021**: A interface MUST manter resultados, categorias e erros independentes, permitir nova tentativa quando recuperável e impedir que respostas obsoletas substituam o estado de uma consulta mais recente.
- **FR-022**: O layout MUST adaptar header, navegação, busca, Hero, categorias, cards e banners para desktop, tablet e mobile, sem rolagem horizontal da página em larguras suportadas.
- **FR-023**: Controles MUST ser semanticamente apropriados, operáveis por teclado e toque, ter labels e foco visível; imagens devem ter alt apropriado e estados não devem depender somente de cor.
- **FR-024**: O frontend MUST consumir APIs somente através do API Gateway e dos clientes/estado existentes ou extensão compatível com eles; não deve acessar PostgreSQL nem introduzir endpoint ou serviço novo.
- **FR-025**: A feature MUST NOT alterar Product Service, Cart Service, User Service, API Gateway, banco, migrations, autenticação JWT/OAuth ou contratos backend.
- **FR-026**: A autenticação MUST permanecer integrada ao fluxo existente, sem credenciais frontend, sem armazenar JWT em `localStorage` por esta feature e sem confiar no cliente para preço, disponibilidade ou autorização.
- **FR-027**: O fluxo atual de carrinho MUST continuar funcionando sem regressão; total, subtotal, preço, quantidade, limite, disponibilidade e estoque sob decisão transacional continuam pertencendo ao serviço.
- **FR-028**: Testes MUST usar mocks dos clientes/stores existentes, sem depender de serviços externos reais, e cobrir Home, categoria, produtos, busca, filtros, ordenação, paginação, detalhe, carrinho, badge, carregamentos, erros, vazio, responsividade crítica e acessibilidade básica.
- **FR-029**: Imagens de produto MUST ser carregadas de forma responsiva e não bloquear desnecessariamente o primeiro conteúdo da página.

### Entidades Principais

- **Produto de catálogo**: Dados públicos retornados pelo Product Service, identificados por ID e categoria, com nome, preço, quantidade, marca, SKU opcional, imagem e demais campos permitidos pela resposta atual. Não inclui no frontend dados comerciais inferidos.
- **Categoria de catálogo**: Categoria ativa retornada pelo Product Service, com nome, ID, categoria-pai opcional e ordem de exibição. Não possui imagem no contrato atual.
- **Item/carrinho confirmado**: Estado compartilhado do Cart Service; quantidades e valores exibidos após mutação vêm da resposta confirmada do serviço.
- **Conteúdo de campanha frontend**: Configuração estática da apresentação de Hero e banners, sem persistência/CMS e sem alterar a veracidade de preços, disponibilidade ou descontos de produto.

## Critérios de Sucesso

### Resultados Mensuráveis

- **SC-001**: Com respostas mockadas válidas, todos os elementos de navegação, conteúdo promocional, categorias e produtos da Home são renderizados sem requisição a serviço não existente.
- **SC-002**: 100% dos dados de produto apresentados em Home, catálogo e cards provêm de respostas do Product Service; nenhum preço anterior, desconto, avaliação ou badge comercial é inferido.
- **SC-003**: Busca, filtro de categoria, ordenação e paginação enviam somente os parâmetros suportados e refletem exatamente a resposta paginada da API.
- **SC-004**: Cada adição pendente gera no máximo uma chamada de adição por acionamento; o badge e feedback após sucesso refletem a resposta/store confirmados.
- **SC-005**: Testes automatizados cobrem todos os estados críticos de Home, catálogo e integração de carrinho sem serviços externos reais, e os testes frontend/build existentes permanecem aprovados.
- **SC-006**: Home, catálogo, navegação e ações principais são utilizáveis em viewports de 1280 px, 768 px e 375 px sem overflow horizontal da página.
- **SC-007**: Busca, navegação por categorias, abertura de produto e adição ao carrinho possuem nome acessível, foco visível e operação por teclado.
- **SC-008**: Nenhum arquivo backend, configuração de serviço, contrato de API ou banco é alterado para concluir esta feature.

## Premissas

- O texto de referência visual do pedido define a intenção, não um contrato rígido de pixels; o layout deve respeitar os padrões de navegação, estado e componentes frontend existentes.
- A Home passará a ocupar `/` e o catálogo completo continuará acessível em `/catalog`; detalhes, login e carrinho conservarão as rotas existentes.
- A seção “Produtos em destaque” solicitará até oito produtos mais recentes da primeira página (`sortBy=newest`, `sortOrder=desc`); o rótulo comunica uma seleção editorial da Home, não uma classificação fornecida pelo Product Service.
- Categoria sem imagem usará tratamento visual genérico no frontend; não haverá upload/associação backend nesta feature.
- O contador do carrinho continua sujeito à sessão válida e ao store compartilhado já existente, não a uma contagem calculada com base em cards.
- O debounce, o número de produtos exibidos na seção inicial, os breakpoints finais e os elementos gráficos do Hero serão definidos no plano sem introduzir dependência ou chamada backend.

## Fora de Escopo

Checkout, pagamentos, pedidos, rastreamento, CMS, administração, favoritos backend, avaliações backend, estoque backend, alterações em serviços, API Gateway, contratos, banco, migrations, JWT/OAuth, Redis, Kafka ou infraestrutura adicional.
