# Tasks: Correção Responsiva da UI do Catálogo

**Input**: Design documents from `specs/008-catalog-ui-correction/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/catalog-ui-invariants.md` e `quickstart.md`

**Organization**: tarefas agrupadas por jornada visual e ordenadas por dependência.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: registrar baseline funcional e preparar a matriz visual.

- [x] T001 Confirmar scripts `test`/`build`, dependências e ausência de bibliotecas novas em `frontend/package.json`
- [x] T002 [P] Registrar baseline de rotas, busca, filtros, ordenação, paginação e detalhes nos testes em `frontend/src/pages/__tests__/CatalogPage.test.tsx` e `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`
- [x] T003 [P] Registrar tokens, breakpoints, regras de container, animações e classes reutilizáveis atuais em `frontend/src/styles.css`

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: proteger invariantes funcionais enquanto a composição é alterada.

- [x] T004 [P] Testar que o catálogo continua usando os mesmos parâmetros e resultados mockados em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T005 [P] Testar que cards continuam navegando para `/products/:id` sem iniciar carrinho em `frontend/src/components/__tests__/ProductCard.test.tsx`
- [x] T006 [P] Definir nomes acessíveis esperados para menu, filtros, busca, ordenação, estados e ações em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T007 Garantir que nenhuma alteração visual introduza estado de dados paralelo em `frontend/src/store/catalogStore.ts` ou chamada nova em `frontend/src/services/apiClient.ts`

**Checkpoint**: baseline e invariantes funcionais prontos; a implementação visual pode começar.

## Phase 3: User Story 1 - Ver o catálogo sem desperdício de espaço (Priority: P1)

**Goal**: compactar hero, integrar busca e aproximar o catálogo da primeira área de visualização.

**Independent Test**: abrir `/` em desktop e confirmar a sequência header, breadcrumb, hero, busca, categorias e início dos produtos sem vazio vertical desproporcional.

### Tests for User Story 1

- [x] T008 [P] [US1] Testar presença e ordem de breadcrumb, título, descrição, busca, categorias e área de produtos em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T009 [P] [US1] Testar que a busca do hero preserva debounce, parâmetros e aplicação do catálogo em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 1

- [x] T010 [US1] Reorganizar a composição de breadcrumb, hero, busca, categorias e catálogo sem duplicar estado em `frontend/src/pages/CatalogPage.tsx`
- [x] T011 [US1] Corrigir container, espaçamento, limite do título, altura da hero e posicionamento da busca em `frontend/src/styles.css`

**Checkpoint**: hero compacta e catálogo visível sem alterar a consulta funcional.

## Phase 4: User Story 2 - Navegar em qualquer tamanho de tela (Priority: P1)

**Goal**: oferecer header e navegação apropriados para desktop, tablet e mobile sem overflow.

**Independent Test**: validar as larguras 375, 390, 430, 768, 1024, 1280, 1440 e 1920 px, confirmando logo, conta, carrinho, menu e ausência de scroll horizontal.

### Tests for User Story 2

- [x] T012 [P] [US2] Testar logo, conta, carrinho, links desktop e controle de menu mobile em `frontend/src/App.tsx` e `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T013 [P] [US2] Adicionar validação de layout sem overflow e foco visível para o shell em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 2

- [x] T014 [US2] Implementar estado de menu mobile e navegação apropriada em `frontend/src/App.tsx` sem alterar rotas funcionais
- [x] T015 [US2] Adaptar header, logo, conta, carrinho e menu por faixa de viewport em `frontend/src/styles.css`
- [x] T016 [US2] Aplicar container fluido, padding lateral responsivo e limites de largura sem `100vw` estrutural em `frontend/src/styles.css`

**Checkpoint**: header e container permanecem navegáveis e sem overflow nas oito larguras.

## Phase 5: User Story 3 - Usar filtros sem perder os produtos de vista (Priority: P1)

**Goal**: manter filtros funcionais, compactar sidebar e oferecer drawer/superfície para mobile.

**Independent Test**: aplicar categoria, preço, marca e estoque em desktop, tablet e mobile; confirmar mesmos critérios, resultados, paginação e limpar.

### Tests for User Story 3

- [x] T017 [P] [US3] Testar abertura, fechamento, aplicação e limpeza da superfície de filtros em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T018 [P] [US3] Testar labels, foco e estado selecionado dos controles de filtros em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 3

- [x] T019 [US3] Adicionar controle visual de abrir/fechar filtros e ações aplicar/limpar sem duplicar os valores de `frontend/src/pages/CatalogPage.tsx`
- [x] T020 [US3] Organizar filtros como sidebar proporcional desktop, área recolhível tablet e drawer/superfície mobile em `frontend/src/pages/CatalogPage.tsx`
- [x] T021 [US3] Estilizar estados selecionados, overlay, foco, fechamento e espaçamento dos filtros em `frontend/src/styles.css`

**Checkpoint**: filtros preservam comportamento e não dominam a viewport mobile.

## Phase 6: User Story 4 - Comparar produtos em uma grade estável (Priority: P1)

**Goal**: adaptar grid e cards a imagens, textos e larguras variadas sem deformação ou overflow.

**Independent Test**: renderizar produtos com imagem, sem imagem, título longo, preço zero e estoque zero em cada faixa e confirmar altura, mídia e link.

### Tests for User Story 4

- [x] T022 [P] [US4] Testar imagem responsiva/fallback, textos longos, disponibilidade, preço e link em `frontend/src/components/__tests__/ProductCard.test.tsx`
- [x] T023 [P] [US4] Testar quantidade de colunas e ausência de overflow da grade em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 4

- [x] T024 [US4] Ajustar markup do `ProductCard` para área de mídia, conteúdo e ação visual estáveis em `frontend/src/components/ProductCard.tsx`
- [x] T025 [US4] Implementar `object-fit: contain` ou equivalente, alturas proporcionais e grid adaptável em `frontend/src/styles.css`
- [x] T026 [US4] Ajustar tipografia, truncamento, gaps e estados hover/foco/touch dos cards em `frontend/src/styles.css`

**Checkpoint**: cards e grid permanecem comparáveis e utilizáveis em todas as larguras.

## Phase 7: User Story 5 - Recuperar-se de estados do catálogo (Priority: P2)

**Goal**: integrar loading, erro, vazio e sem resultados à nova composição sem mascarar a API.

**Independent Test**: simular cada estado, confirmar mensagem/ação existente, espaço proporcional e foco acessível.

### Tests for User Story 5

- [x] T027 [P] [US5] Testar loading, erro com retry, catálogo vazio e busca sem resultados em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T028 [P] [US5] Testar fallback de imagem, foco e acessibilidade de estados do card em `frontend/src/components/__tests__/ProductCard.test.tsx`

### Implementation for User Story 5

- [x] T029 [US5] Reorganizar markup dos estados de loading, erro, vazio e sem resultados mantendo textos e ações funcionais em `frontend/src/pages/CatalogPage.tsx`
- [x] T030 [US5] Estilizar skeletons, aviso “O catálogo não pôde ser carregado”, empty state e ações de recuperação em `frontend/src/styles.css`

**Checkpoint**: estados são distintos, proporcionais e recuperáveis sem dados fictícios.

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: validar acessibilidade, redução de movimento, todas as larguras e regressão final.

- [x] T031 [P] Ajustar `prefers-reduced-motion`, foco, contraste, `alt`, overflow de texto e navegação por teclado em `frontend/src/styles.css`, `frontend/src/App.tsx` e `frontend/src/pages/CatalogPage.tsx`
- [x] T032 [P] Validar visualmente 375, 390, 430, 768, 1024, 1280, 1440 e 1920 px e corrigir cortes/overflows em `frontend/src/styles.css`
- [x] T033 [P] Confirmar que `catalogStore`, `apiClient`, `frontend/src/types/catalog.ts` e `backend/api-gateway/src/main/resources/application.yml` não receberam alterações funcionais
- [x] T034 Executar `npm test` e `npm run build` em `frontend/` e seguir `specs/008-catalog-ui-correction/quickstart.md`

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências; registra a baseline.
- **Foundational (Phase 2)**: depende da Setup e bloqueia as histórias.
- **US1 (Phase 3)**: depende da Foundation; estabelece composição compacta.
- **US2 (Phase 4)**: depende da Foundation; pode avançar com US1 em arquivos distintos, salvo CSS compartilhado.
- **US3 (Phase 5)**: depende da estrutura da US1 e compartilha `CatalogPage`/CSS.
- **US4 (Phase 6)**: depende da grade da US1 e pode avançar em paralelo com filtros se arquivos forem coordenados.
- **US5 (Phase 7)**: depende da composição final para estilizar estados.
- **Polish (Phase 8)**: depende de todas as histórias concluídas.

### User Story Dependencies

- **US1**: independente após Foundation.
- **US2**: depende do shell/header, mas é funcionalmente isolável.
- **US3**: depende da estrutura de catálogo da US1; mantém estado funcional existente.
- **US4**: depende da grade/card e compartilha CSS com US1-US3.
- **US5**: depende das superfícies finais de catálogo e card.

### Parallel Opportunities

- T002/T003 e T004-T006 podem ser executadas em paralelo.
- T008/T009, T012/T013, T017/T018, T022/T023 e T027/T028 são testes paralelizáveis quando separados por responsabilidade.
- T015/T016 e T025/T026 compartilham CSS e devem ser serializadas no mesmo arquivo.
- T031-T033 podem ser divididas por arquivo; T034 é a validação final.

## Parallel Example: User Story 3

```text
T017: comportamento de abrir/fechar filtros em frontend/src/pages/__tests__/CatalogPage.test.tsx
T018: labels e foco dos filtros em frontend/src/pages/__tests__/CatalogPage.test.tsx
```

## Parallel Example: User Story 4

```text
T022: comportamento do ProductCard em frontend/src/components/__tests__/ProductCard.test.tsx
T023: responsividade da grade em frontend/src/pages/__tests__/CatalogPage.test.tsx
```

## Implementation Strategy

### MVP First (User Stories 1-4)

1. Concluir Setup e Foundation.
2. Corrigir hero/container e header responsivo.
3. Implementar filtros móveis e grid/cards responsivos.
4. Executar testes e validar as oito larguras antes de tratar estados avançados.

### Incremental Delivery

1. Adicionar US5 para estados visuais.
2. Executar Polish com acessibilidade, redução de movimento e regressão funcional.
3. Confirmar que nenhuma chamada, regra, rota ou contrato funcional foi alterado.

## Notes

- `[P]` indica tarefas independentes e sem conflito de arquivo incompleto.
- `[USn]` vincula a tarefa à história correspondente.
- Todas as tarefas possuem ID, ação específica e caminho de arquivo.
- Nenhuma tarefa cria catálogo, endpoint, store ou regra de negócio nova.

