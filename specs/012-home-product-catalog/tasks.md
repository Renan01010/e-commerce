# Tasks: Home e Catálogo de Produtos

**Input**: Documentos de design em `specs/012-home-product-catalog/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contrato de consumo](contracts/catalog-home-gateway-consumption.md) e [quickstart.md](quickstart.md)

**Tests**: Incluídos porque a especificação exige cobertura automatizada para Home, catálogo, integração de carrinho, erros, estados e acessibilidade. Os testes usam mocks, sem depender de serviços externos.

**Organization**: Tarefas agrupadas por história de usuário. Testes de comportamento precedem a implementação correspondente. Rotas e contratos de estado compartilhados são pré-requisitos das histórias.

**Referência visual obrigatória antes da implementação da Home**: consultar `specs/012-home-product-catalog/references/TechStore_ Tecnologia sem limites.png` antes das tarefas T013–T016 e T038–T041. Usar a composição, hierarquia, espaçamento, Hero, header, categorias, cards, banners e direção dark/tech como referência. Não copiar textos comerciais, percentuais/ofertas, produtos, marcas, imagens de produto, avaliações, badges, preços ou dados de categoria/disponibilidade da imagem. O caminho `references/home-desktop-reference.png` indicado inicialmente não existe; este é o nome do arquivo presente.

## Phase 1: Setup

**Purpose**: confirmar que a feature usa a toolchain e os clientes existentes.

- [X] T001 Confirmar scripts `test`/`build`, React Router, Zustand, lucide-react e ausência de necessidade de dependências novas em `frontend/package.json`.
- [X] T002 [P] Registrar no quickstart os caminhos `/`, `/catalog`, `/products/:id`, `/cart`, a comparação visual com a referência desktop e os cenários principais de validação manual em `specs/012-home-product-catalog/quickstart.md`.

---

## Phase 2: Foundational

**Purpose**: estabelecer rotas, estado compartilhado seguro e normalização de erros antes das histórias dependentes.

- [X] T003 [P] Testar rotas da Home em `/`, do catálogo em `/catalog` e preservação de detalhe, login, cadastro e carrinho em `frontend/src/__tests__/App.test.tsx`.
- [X] T004 [P] Testar que categorias compartilhadas deduplicam carregamentos concorrentes, reutilizam resposta bem-sucedida entre rotas, expõem loading/erro/retry explícito e ignoram resposta obsoleta em `frontend/src/store/__tests__/catalogStore.test.ts`.
- [X] T005 [P] Testar mensagens de erro públicas seguras para `400`, `401`, `403`, `404`, `409`, `429`, `5xx` e rede, sem expor mensagens técnicas arbitrárias, em `frontend/src/services/__tests__/apiClient.test.ts`.
- [X] T006 Registrar a Home em `/`, mover a listagem existente para `/catalog` e manter todas as demais rotas funcionando em `frontend/src/App.tsx`.
- [X] T007 Implementar estado explícito, cache em memória após sucesso, deduplicação de requests concorrentes e retry que refaz a chamada de categorias sem alterar o contrato nem a coleção de produtos paginada em `frontend/src/store/catalogStore.ts`.
- [X] T008 Normalizar erros do catálogo por status/rede com mensagens amigáveis, incluindo `429` e fallback seguro, em `frontend/src/services/apiClient.ts`.

**Checkpoint**: `/` e `/catalog` têm destinos distintos, categorias podem ser compartilhadas e falhas da API não vazam detalhes técnicos.

---

## Phase 3: User Story 1 - Descobrir a loja pela Home (Priority: P1) 🎯 MVP

**Goal**: apresentar uma Home responsiva com categorias reais e até oito produtos mais recentes, sem conteúdo comercial inventado.

**Independent Test**: renderizar `/` com mocks de produtos/categorias; confirmar parâmetros de consulta, Hero, navegação real, produtos, links e estados de carregamento, vazio e falha.

### Tests for User Story 1

- [X] T009 [P] [US1] Cobrir estado isolado de produtos recentes, request `newest/desc`, página zero/tamanho oito, erro/retry e resposta obsoleta sem alterar a listagem em `frontend/src/store/__tests__/catalogStore.test.ts`.
- [X] T010 [P] [US1] Testar Home com categorias ordenadas por `displayOrder`, produtos retornados, CTA para catálogo, links de detalhe e estados independentes de carregamento/vazio/erro em `frontend/src/pages/__tests__/HomePage.test.tsx`.
- [X] T011 [P] [US1] Testar categoria navegável usando ID real e busca encaminhada a `/catalog` sem inventar IDs, nomes ou imagens em `frontend/src/pages/__tests__/HomePage.test.tsx`.

### Implementation for User Story 1

- [X] T012 [US1] Adicionar estado/ação de consulta dos produtos recentes separado da busca paginada, solicitando `query` vazio, filtros vazios, `sortBy: 'newest'`, `sortOrder: 'desc'`, página zero e `pageSize: 8` via `catalogApi` em `frontend/src/store/catalogStore.ts`.
- [X] T013 [US1] Criar `HomePage` com Hero editorial, busca, benefícios informativos, categorias reais ordenadas, seção de produtos e banners com CTAs para rotas existentes em `frontend/src/pages/HomePage.tsx`.
- [X] T014 [US1] Implementar skeletons, retry e estados vazios separados para produtos recentes e categorias em `frontend/src/pages/HomePage.tsx`.
- [X] T015 [US1] Tornar consulta de pesquisa da Home navegável como `query` na URL de `/catalog`, preservando caracteres e sem executar uma requisição por tecla em `frontend/src/pages/HomePage.tsx`.
- [X] T016 [US1] Exibir categorias sem imagens com ícones/tratamento visual genérico e links baseados somente em categorias recebidas em `frontend/src/pages/HomePage.tsx`.

**Checkpoint**: A Home apresenta conteúdo de API ou estados de falha/vazio explícitos e navega para categorias reais, catálogo e detalhe.

---

## Phase 4: User Story 2 - Encontrar produtos no catálogo completo (Priority: P1)

**Goal**: manter o catálogo existente integralmente acessível em `/catalog` e inicializar busca (`query`) e categoria (`categoryId`) pela URL; outros filtros, ordenação e paginação permanecem no estado existente do catálogo.

**Independent Test**: abrir `/catalog` com parâmetros `query`/`categoryId`; confirmar hidratação desses dois parâmetros, limpeza quando ausentes, sincronização ao editar e restauração por voltar/avançar, além de consulta real, filtros, ordenação, paginação, vazio, erro e abertura do detalhe.

### Tests for User Story 2

- [X] T017 [P] [US2] Cobrir hidratação bidirecional de `query`/`categoryId`, limpeza quando ausentes na URL, restauração ao voltar/avançar e paginação reiniciada ao mudar critérios em `frontend/src/pages/__tests__/CatalogPage.test.tsx`.
- [X] T018 [P] [US2] Testar destinos `/catalog?categoryId=<id>` para categorias do catálogo e retorno de detalhe para `/catalog` em `frontend/src/pages/__tests__/CatalogPage.test.tsx` e `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`.
- [X] T019 [P] [US2] Confirmar que links compartilhados de navegação e fallback levam ao catálogo completo em `frontend/src/__tests__/App.test.tsx`.

### Implementation for User Story 2

- [X] T020 [US2] Tratar `query` e `categoryId` como fonte de verdade bidirecional: hidratar/limpar o `catalogStore` ao entrar em `/catalog` ou mudar histórico/URL e atualizar os parâmetros quando os controles de busca/categoria mudarem, sem sobrescrever outros filtros locais, em `frontend/src/pages/CatalogPage.tsx`.
- [X] T021 [US2] Atualizar chips de categoria, breadcrumbs, ações de retorno e links internos para usar `/catalog` e o `categoryId` real em `frontend/src/pages/CatalogPage.tsx` e `frontend/src/pages/ProductDetailPage.tsx`.
- [X] T022 [US2] Preservar filtros, ordenação, paginação, skeleton, estado vazio e retry da listagem ao separar o catálogo da rota inicial em `frontend/src/pages/CatalogPage.tsx`.

**Checkpoint**: busca, filtros, ordenação, paginação e detalhe funcionam no destino `/catalog`, independentemente da Home.

---

## Phase 5: User Story 3 - Adicionar um produto ao carrinho (Priority: P1)

**Goal**: permitir adição segura nos cards, respeitando sessão, disponibilidade e estado confirmado pelo Cart Service.

**Independent Test**: com catálogo e carrinho mockados, acionar a adição; confirmar login para visitante, loading/bloqueio de duplicação, resposta confirmada, badge e erro.

### Tests for User Story 3

- [X] T023 [P] [US3] Testar card com nome, marca/preço/disponibilidade, imagem ausente/com erro e fallback, ausência de UUID, navegação para detalhe e controles interativos não aninhados em `frontend/src/components/__tests__/ProductCard.test.tsx`.
- [X] T024 [P] [US3] Testar ação de login para visitante, adição única para cliente autenticado, loading, erro e estado desabilitado por indisponibilidade em `frontend/src/components/__tests__/ProductCard.test.tsx`.
- [X] T025 [P] [US3] Testar no App que a resposta confirmada da adição atualiza o badge do cabeçalho sem cálculo local nem segundo POST em `frontend/src/__tests__/App.test.tsx`.

### Implementation for User Story 3

- [X] T026 [US3] Reestruturar `ProductCard` como artigo com link de detalhe e botão/link de login como controles irmãos, nunca interativos aninhados; adicionar a ação via `cartStore.addItem` e estados acessíveis derivados do store em `frontend/src/components/ProductCard.tsx`.
- [X] T027 [US3] Bloquear a ação quando o carrinho não estiver carregado, o produto estiver indisponível ou a operação `add:<productId>` estiver pendente; não duplicar validação financeira/limites em `frontend/src/components/ProductCard.tsx`.
- [X] T028 [US3] Apresentar feedback de sucesso/erro e preservar a resposta confirmada no card, em coordenação com a ação `dismissFeedback` existente, em `frontend/src/components/ProductCard.tsx`.
- [X] T029 [US3] Manter badge derivado exclusivamente do estado confirmado de `cartStore` e assegurar atualização após mutação originada no card em `frontend/src/App.tsx`.

**Checkpoint**: adição em Home e catálogo reutiliza o store existente, impede cliques concorrentes e sincroniza badge/feedback sem estimativas.

---

## Phase 6: User Story 4 - Compreender carregamento, vazio e falhas (Priority: P1)

**Goal**: tornar recuperáveis e compreensíveis os estados de falha tanto de Home quanto de catálogo.

**Independent Test**: simular HTTP `400`, `401`, `403`, `404`, `409`, `429`, `5xx`, falha de rede, falha de categoria e resposta vazia; confirmar mensagens seguras e retries adequados.

### Tests for User Story 4

- [X] T030 [P] [US4] Cobrir estados Home de falha/retry independentes para produto e categoria sem apagar o conteúdo da outra consulta em `frontend/src/pages/__tests__/HomePage.test.tsx`.
- [X] T031 [P] [US4] Cobrir erro de catálogo, vazio após filtro e retry preservando critérios da URL em `frontend/src/pages/__tests__/CatalogPage.test.tsx`.
- [X] T032 [P] [US4] Cobrir ausência de preço promocional/avaliação e que nenhuma mensagem de erro renderiza UUID, stack trace ou detalhe técnico em testes de `HomePage` e `CatalogPage`.

### Implementation for User Story 4

- [X] T033 [US4] Apresentar falha de catálogo em estado próprio com retry e manter busca/filtros atuais em `frontend/src/pages/CatalogPage.tsx`.
- [X] T034 [US4] Adicionar mensagens e ações de recuperação distintas para falhas de categorias/produtos na Home em `frontend/src/pages/HomePage.tsx`.
- [X] T035 [US4] Assegurar feedback semântico `role="alert"`/`role="status"` e evitar interpolar mensagem técnica arbitrária na Home e no catálogo em `frontend/src/pages/HomePage.tsx` e `frontend/src/pages/CatalogPage.tsx`.

**Checkpoint**: respostas vazias, falhas de transporte e status HTTP não são confundidos; cada leitura recuperável tem retry coerente.

---

## Phase 7: User Story 5 - Navegar com conforto em diferentes telas (Priority: P2)

**Goal**: aplicar a identidade TechStore escura, layout adaptável e padrões básicos de acessibilidade à Home e às superfícies compartilhadas necessárias.

**Independent Test**: conferir 1280 px, 768 px e 375 px, navegação por teclado, nomes acessíveis, foco e ausência de overflow horizontal da página.

### Tests for User Story 5

- [X] T036 [P] [US5] Testar busca, navegação de categoria, menu responsivo e acesso a conta/carrinho por nomes acessíveis e teclado em `frontend/src/__tests__/App.test.tsx` e `frontend/src/pages/__tests__/HomePage.test.tsx`.
- [X] T037 [P] [US5] Testar labels, alt/fallback de imagem, botão de adição e anúncios de estado em `frontend/src/components/__tests__/ProductCard.test.tsx` e `frontend/src/pages/__tests__/HomePage.test.tsx`.

### Implementation for User Story 5

- [X] T038 [US5] Definir tokens e estilos escuros para Home, cartões, categorias, benefícios, banners e estados de loading/error, preservando estilos específicos de login em `frontend/src/styles.css`.
- [X] T039 [US5] Adaptar Hero, busca, categorias, grid, banners e espaçamento a desktop/tablet/mobile, com overflow horizontal restrito às faixas de categoria quando necessário, em `frontend/src/styles.css`.
- [X] T040 [US5] Integrar busca no cabeçalho, menu acessível e navegação de início/produtos/categorias às rotas novas sem remover conta ou carrinho em `frontend/src/App.tsx` e `frontend/src/styles.css`.
- [X] T041 [US5] Aplicar foco visível, alvos de toque, reduced motion e contraste legível às superfícies da Home e ações do card em `frontend/src/styles.css`.

**Checkpoint**: Home e rotas compartilhadas são utilizáveis por toque, teclado e leitor de tela nos breakpoints definidos.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: verificar integração final, regressões de carrinho, performance e conformidade com escopo frontend-only.

- [X] T042 [P] Rever `frontend/src/App.tsx`, `frontend/src/pages/HomePage.tsx`, `frontend/src/pages/CatalogPage.tsx` e `frontend/src/components/ProductCard.tsx` para confirmar que nenhum UUID, preço antigo, desconto, avaliação ou badge comercial fictício é renderizado.
- [X] T043 [P] Rever as requisições em `frontend/src/services/apiClient.ts` e `frontend/src/store/catalogStore.ts` para confirmar ausência de endpoint novo e chamadas duplicadas a produto/categorias.
- [X] T044 Executar a suíte frontend (`npm test`) em `frontend/` e corrigir falhas relacionadas à feature.
- [X] T045 Executar o build TypeScript/Vite (`npm run build`) em `frontend/` e corrigir falhas relacionadas à feature.
- [X] T046 Executar cenários de `specs/012-home-product-catalog/quickstart.md`, inclusive viewports 1280 px, 768 px e 375 px, e registrar gaps remanescentes nos artefatos da feature.
- [X] T047 Confirmar por `git status --short` que os arquivos alterados, incluindo novos não rastreados, pertencem ao escopo frontend/documentação e não incluem backend, Gateway, contratos backend, banco, migrations ou autenticação; não descartar mudanças de terceiros.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: independente; confirmar toolchain e registrar cenários.
- **Foundational (Phase 2)**: bloqueia as histórias que dependem das novas rotas, estados compartilhados e erros seguros.
- **User Story 1 (Phase 3)**: depende da fase Foundational; entrega Home funcional independentemente do catálogo filtrado.
- **User Story 2 (Phase 4)**: depende da separação de rota; pode ser validada com APIs mockadas.
- **User Story 3 (Phase 5)**: depende do card exibido na Home/catalog e do cart store já existente; não depende de backend novo.
- **User Story 4 (Phase 6)**: depende dos estados de leitura das histórias 1 e 2 e do mapeamento HTTP fundacional.
- **User Story 5 (Phase 7)**: depende da estrutura visual e controles das histórias 1–3.
- **Polish (Phase 8)**: depende de todas as histórias implementadas; fecha validação de testes/build e limites de escopo.

### User Story Dependencies

- **US1 (P1)**: pode ser implementada após a fase Foundational; MVP de Home e descoberta de produtos.
- **US2 (P1)**: pode avançar em paralelo com a apresentação da Home depois que rotas foram estabelecidas; a passagem de busca/categoria é integrada por URL.
- **US3 (P1)**: requer `ProductCard` disponível; integra-se tanto à Home quanto ao catálogo, mas pode ser testada por componente com mocks próprios.
- **US4 (P1)**: integra Home e catálogo após estados de loading/erro implementados.
- **US5 (P2)**: aplica os breakpoints e verificações de acessibilidade nas superfícies já concluídas.

### Within Each User Story

- Escrever os testes de cada comportamento antes da implementação e confirmar que falham pelo motivo esperado.
- Manter tarefas [P] somente quando atuam em arquivos independentes e não dependem do resultado de outra tarefa.
- Completar UI, estado, navegação e tratamento de erro da história antes de avançar para o próximo checkpoint.
- Executar o teste focalizado da história depois de sua implementação e atualizar testes existentes quando a rota ou componente compartilhado mudar.

### Parallel Opportunities

- Na US1, store (T009) e testes visuais/semânticos (T010–T011) podem ser preparados em arquivos separados; implementação Home começa depois do contrato de store estabilizado.
- Na US2, os testes do App, catálogo e detalhe (T017–T019) são independentes.
- Na US3, testes do card e do badge do App (T023–T025) são independentes antes da implementação.
- Na US4, testes isolados de Home e catálogo (T030–T032) podem ser escritos em paralelo.
- Na US5, testes de navegação/Home e ProductCard (T036–T037) podem ser escritos em paralelo.
- Na Phase 8, revisões de UI e cliente HTTP (T042–T043) podem ser conduzidas em paralelo.

## Implementation Strategy

1. Concluir Setup e Foundation para fixar as rotas, estabelecer categorias reutilizáveis e evitar erros inseguros.
2. Implementar US1 primeiro para produzir um MVP demonstrável da Home com produtos/categorias reais.
3. Implementar US2 para terminar a transição sem regressão do catálogo e compatibilizar links e query params.
4. Implementar US3 para levar a ação do carrinho aos cards e confirmar sincronização com a resposta oficial.
5. Completar US4/US5 e rodar os gates automatizados, build e validação responsiva do quickstart.

**MVP**: conclusão de T001–T016: `/` apresenta Home funcional com consulta recente, categorias reais, estados e navegação para o catálogo.
