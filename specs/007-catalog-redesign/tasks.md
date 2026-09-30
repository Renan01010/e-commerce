# Tasks: Redesign Visual do CatÃ¡logo

**Input**: Design documents from `specs/007-catalog-redesign/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/catalog-ui-invariants.md` e `quickstart.md`

**Organization**: tarefas agrupadas por jornada visual e ordenadas por dependÃªncia.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: registrar a superfÃ­cie visual existente e preparar a regressÃ£o funcional.

- [x] T001 Confirmar os scripts `test` e `build`, dependÃªncias e ausÃªncia de dependÃªncias visuais novas em `frontend/package.json`
- [x] T002 [P] Registrar a baseline funcional de rotas, busca, filtros, ordenaÃ§Ã£o, paginaÃ§Ã£o e detalhe nos testes existentes em `frontend/src/pages/__tests__/CatalogPage.test.tsx` e `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`
- [x] T003 [P] Confirmar tokens de cor, tipografia, breakpoints, foco e animaÃ§Ãµes atuais antes da refatoraÃ§Ã£o em `frontend/src/styles.css`

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: preservar os invariantes funcionais enquanto a composiÃ§Ã£o visual muda.

- [x] T004 [P] Cobrir as invariantes de rota, navegaÃ§Ã£o de card e ausÃªncia de chamadas de carrinho em `frontend/src/pages/__tests__/CatalogPage.test.tsx` e `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`
- [x] T005 [P] Definir no teste os nomes acessÃ­veis esperados para header, pesquisa, filtros, ordenaÃ§Ã£o, cards e estados em `frontend/src/pages/__tests__/CatalogPage.test.tsx` e `frontend/src/components/__tests__/ProductCard.test.tsx`
- [x] T006 Garantir que a implementaÃ§Ã£o visual use somente dados e seletores jÃ¡ fornecidos por `frontend/src/store/catalogStore.ts` e `frontend/src/services/apiClient.ts`

**Checkpoint**: regressÃ£o funcional e invariantes de UI definidos; nenhuma alteraÃ§Ã£o em backend, Gateway ou contratos.

## Phase 3: User Story 1 - Orientar a navegaÃ§Ã£o da loja (Priority: P1)

**Goal**: criar um header premium, claro e responsivo com marca, navegaÃ§Ã£o, conta e carrinho futuro/inativo.

**Independent Test**: renderizar o layout do catÃ¡logo em desktop, tablet e mobile; confirmar links, nomes acessÃ­veis, foco visÃ­vel e carrinho sem mutaÃ§Ã£o.

### Tests for User Story 1

- [x] T007 [P] [US1] Testar marca, link de catÃ¡logo, acesso Ã  conta, entrada de carrinho e estado nÃ£o funcional em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T008 [P] [US1] Testar ordem de foco e nomes acessÃ­veis do header no layout em `frontend/src/App.tsx` e `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 1

- [x] T009 [US1] Atualizar `StoreLayout` com navegaÃ§Ã£o de catÃ¡logo/categorias, conta e carrinho futuro sem chamadas ou mutaÃ§Ãµes em `frontend/src/App.tsx`
- [x] T010 [US1] Criar composiÃ§Ã£o visual responsiva do header, marca, links, estado de carrinho e foco em `frontend/src/styles.css`

**Checkpoint**: header navegÃ¡vel e coerente em todas as larguras, sem ativar carrinho.

## Phase 4: User Story 2 - Descobrir o catÃ¡logo (Priority: P1)

**Goal**: transformar o topo da pÃ¡gina em uma introduÃ§Ã£o visual com breadcrumb, tÃ­tulo, subtÃ­tulo, pesquisa e contexto de categorias.

**Independent Test**: abrir `/`, localizar o hero e iniciar uma pesquisa; confirmar que a consulta e os resultados permanecem iguais aos anteriores.

### Tests for User Story 2

- [x] T011 [P] [US2] Testar breadcrumb, tÃ­tulo, subtÃ­tulo, campo destacado de pesquisa e contexto de categorias em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T012 [P] [US2] Testar que a pesquisa do hero preserva debounce, parÃ¢metros e resultados existentes em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 2

- [x] T013 [US2] Reorganizar o topo da pÃ¡gina com breadcrumb, hero, subtÃ­tulo, categorias e pesquisa sem duplicar estado em `frontend/src/pages/CatalogPage.tsx`
- [x] T014 [US2] Estilizar hero, breadcrumb, pesquisa destacada e categorias com hierarquia premium e composiÃ§Ã£o mobile-first em `frontend/src/styles.css`

**Checkpoint**: o visitante entende a loja e pode pesquisar sem alterar a lÃ³gica funcional.

## Phase 5: User Story 3 - Comparar produtos visualmente (Priority: P1)

**Goal**: transformar cards em unidades de comparaÃ§Ã£o com imagem, contexto, nome, preÃ§o, disponibilidade e interaÃ§Ã£o suave.

**Independent Test**: renderizar produtos com/sem imagem e com/sem estoque; confirmar dimensÃµes estÃ¡veis, foco, hover/touch e link para detalhes.

### Tests for User Story 3

- [x] T015 [P] [US3] Testar hierarquia do card, preÃ§o, nome, categoria/contexto, disponibilidade, fallback de imagem e link em `frontend/src/components/__tests__/ProductCard.test.tsx`
- [x] T016 [P] [US3] Testar que campos opcionais ausentes nÃ£o geram avaliaÃ§Ã£o, desconto ou preÃ§o anterior inventados em `frontend/src/components/__tests__/ProductCard.test.tsx`

### Implementation for User Story 3

- [x] T017 [US3] Reorganizar o markup do card para priorizar imagem, categoria/contexto, nome, preÃ§o e disponibilidade em `frontend/src/components/ProductCard.tsx`
- [x] T018 [US3] Aplicar proporÃ§Ãµes estÃ¡veis, estados de hover/foco/touch, transiÃ§Ãµes e substituto de imagem em `frontend/src/styles.css`

**Checkpoint**: cards comparÃ¡veis e acessÃ­veis, preservando o destino existente e sem aÃ§Ã£o de compra.

## Phase 6: User Story 4 - Refinar resultados com pouco atrito (Priority: P2)

**Goal**: reduzir o peso visual dos filtros e organizar quantidade, ordenaÃ§Ã£o e grade sem alterar controles ou regras.

**Independent Test**: aplicar cada filtro e ordenaÃ§Ã£o em desktop/mobile e confirmar que os parÃ¢metros, resultados e paginaÃ§Ã£o permanecem inalterados.

### Tests for User Story 4

- [x] T019 [P] [US4] Testar filtros, limpar, ordenaÃ§Ã£o, quantidade e paginaÃ§Ã£o com os mesmos controles existentes em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T020 [P] [US4] Testar estados visuais selecionados e labels acessÃ­veis dos controles em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 4

- [x] T021 [US4] Reorganizar toolbar, quantidade, ordenaÃ§Ã£o e agrupamento dos filtros em `frontend/src/pages/CatalogPage.tsx` sem duplicar estado
- [x] T022 [US4] Refinar espaÃ§amento, hierarquia, controles selecionados, botÃ£o limpar e comportamento desktop/tablet/mobile em `frontend/src/styles.css`

**Checkpoint**: filtros continuam funcionais e deixam a grade como foco principal.

## Phase 7: User Story 5 - Entender os estados da experiÃªncia (Priority: P2)

**Goal**: aplicar uma linguagem visual consistente a loading, erro, catÃ¡logo vazio e busca sem resultados.

**Independent Test**: simular cada estado e confirmar mensagem, aÃ§Ã£o de recuperaÃ§Ã£o, proporÃ§Ã£o visual e acessibilidade.

### Tests for User Story 5

- [x] T023 [P] [US5] Testar skeletons, erro com retry, catÃ¡logo vazio e busca sem resultados em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T024 [P] [US5] Testar fallback visual de imagem, quantidade zero, textos longos e foco acessÃ­vel em `frontend/src/components/__tests__/ProductCard.test.tsx`

### Implementation for User Story 5

- [x] T025 [US5] Reorganizar markup dos estados de carregamento, erro, vazio e sem resultados mantendo aÃ§Ãµes existentes em `frontend/src/pages/CatalogPage.tsx`
- [x] T026 [US5] Criar estilos de skeleton, notices, empty states, mensagens e aÃ§Ãµes de recuperaÃ§Ã£o sem grandes Ã¡reas vazias em `frontend/src/styles.css`

**Checkpoint**: todos os estados sÃ£o distinguÃ­veis e visualmente integrados ao redesign.

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: concluir responsividade, acessibilidade, reduÃ§Ã£o de movimento e regressÃ£o visual.

- [x] T027 [P] Ajustar coerÃªncia visual do detalhe, breadcrumbs e retorno ao catÃ¡logo em `frontend/src/pages/ProductDetailPage.tsx` e `frontend/src/styles.css`
- [x] T028 [P] Adicionar `prefers-reduced-motion`, foco visÃ­vel, contraste e prevenÃ§Ã£o de overflow para textos longos em `frontend/src/styles.css`
- [x] T029 [P] Validar viewports de 320 px, tablet e 1440 px e corrigir sobreposiÃ§Ã£o/corte em `frontend/src/styles.css`
- [x] T030 [P] Confirmar que `catalogStore`, `apiClient`, `frontend/src/types/catalog.ts` e `backend/api-gateway/src/main/resources/application.yml` nÃ£o receberam alteraÃ§Ãµes funcionais
- [x] T031 Executar `npm test` e `npm run build` em `frontend/` e seguir a matriz visual de `specs/007-catalog-redesign/quickstart.md`

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependÃªncias; registra baseline.
- **Foundational (Phase 2)**: depende da Setup e bloqueia todas as histÃ³rias.
- **US1 (Phase 3)**: depende da Foundation; estabelece o header e Ã© parte do MVP.
- **US2 (Phase 4)**: depende da Foundation; pode ser desenvolvida apÃ³s o header.
- **US3 (Phase 5)**: depende da Foundation; pode ser paralela Ã  US2 em arquivos distintos.
- **US4 (Phase 6)**: depende da composiÃ§Ã£o da listagem da US2.
- **US5 (Phase 7)**: depende das telas reorganizadas nas histÃ³rias anteriores.
- **Polish (Phase 8)**: depende das histÃ³rias visuais concluÃ­das.

### User Story Dependencies

- **US1**: independente apÃ³s Foundation.
- **US2**: independente funcionalmente, mas compartilha `CatalogPage` e `styles.css` com US1.
- **US3**: independente funcionalmente apÃ³s Foundation; compartilha `styles.css` com US2.
- **US4**: depende da composiÃ§Ã£o de resultados de US2 para reorganizar filtros e toolbar.
- **US5**: depende da estrutura visual final de US2-US4 para padronizar estados.

### Parallel Opportunities

- T002 e T003 podem ser executadas em paralelo.
- T004-T005 podem ser executadas em paralelo antes da implementaÃ§Ã£o visual.
- T007-T008, T011-T012, T015-T016, T019-T020 e T023-T024 sÃ£o pares de testes paralelizÃ¡veis.
- T010, T014 e T018 sÃ£o tarefas CSS que devem ser serializadas se editarem as mesmas regras.
- T027-T030 podem ser paralelizadas por arquivo; T031 Ã© a validaÃ§Ã£o final.

## Parallel Example: User Story 1

```text
T007: regressÃ£o de header em frontend/src/pages/__tests__/CatalogPage.test.tsx
T008: foco e nomes acessÃ­veis em frontend/src/App.tsx e frontend/src/pages/__tests__/CatalogPage.test.tsx
```

## Parallel Example: User Story 3

```text
T015: hierarquia e estados do card em frontend/src/components/__tests__/ProductCard.test.tsx
T016: ausÃªncia de dados opcionais em frontend/src/components/__tests__/ProductCard.test.tsx
```

## Implementation Strategy

### MVP First (User Stories 1-3)

1. Concluir Setup e Foundation.
2. Implementar US1: header e navegaÃ§Ã£o.
3. Implementar US2: hero e descoberta.
4. Implementar US3: cards e grid.
5. Executar testes e validar visualmente antes de avanÃ§ar para filtros/estados.

### Incremental Delivery

1. Adicionar US4 para refinamento visual dos filtros e toolbar.
2. Adicionar US5 para loading, erro, vazio e sem resultados.
3. Executar Polish com acessibilidade, reduÃ§Ã£o de movimento e matriz de viewports.
4. Confirmar que nenhum comportamento funcional, endpoint ou fluxo de carrinho foi alterado.

## Notes

- `[P]` indica tarefas em arquivos diferentes ou testes independentes.
- `[USn]` vincula cada tarefa Ã  histÃ³ria correspondente.
- Todas as tarefas possuem ID, aÃ§Ã£o especÃ­fica e caminho de arquivo.
- O redesign nÃ£o cria componentes de estado, endpoints ou regras comerciais novas.

