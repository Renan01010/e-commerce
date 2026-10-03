# Tasks: Consistência Visual e Redesign das Telas Existentes

**Input**: Design documents from `/specs/013-design-consistency-redesign/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/frontend-ui-preservation.md`, `quickstart.md`

**Tests**: Incluídos conforme solicitado na especificação; usar Vitest, React Testing Library e mocks existentes. A inspeção visual responsiva complementa os testes automatizados.

**Organization**: Tarefas agrupadas pelas cinco histórias da especificação. Stores, serviços e contratos permanecem fora do escopo de alteração.

## Format: `[ID] [P?] [Story] Description`

- `[P]`: tarefa paralelizável sem dependência de uma tarefa incompleta que altere o mesmo arquivo.
- `[Story]`: história correspondente à especificação.
- Todas as tarefas indicam o caminho do arquivo que será validado ou alterado.

## Phase 1: Setup

**Purpose**: Registrar o estado de validação prévio antes de mudanças visuais.

- [X] T001 Executar `npm test` e `npm run build` em `frontend/package.json` e registrar quaisquer falhas preexistentes para comparação com a validação final.

---

## Phase 2: Foundational — Tokens compartilhados

**Purpose**: Estabelecer a base visual reutilizada pelas telas sem afetar a aparência vigente da Home.

- [X] T002 Consolidar em `frontend/src/styles.css` tokens reutilizáveis para fundo navy, superfícies, texto, texto secundário, bordas e acento lima a partir dos valores dark/tech já usados pelo header e pela Home; substituir os valores equivalentes sem alterar a aparência da Home nem mudar globalmente telas fora do escopo.

**Checkpoint**: Tokens visuais compartilhados prontos; iniciar as adaptações por história, preservando os comportamentos existentes.

---

## Phase 3: User Story 1 — Navegar pela loja com identidade visual consistente (Priority: P1) 🎯 MVP

**Goal**: Alinhar visualmente Catálogo, header e fallback à Home, mantendo o shell, o badge e todos os controles/rotas atuais do catálogo.

**Independent Test**: Com mocks, navegar entre Home, catálogo, detalhe, carrinho e fallback; confirmar header único nas rotas da loja, badge derivado do carrinho confirmado e apresentação dark do catálogo sem regressão de busca, filtros, ordenação, paginação, estados ou destinos.

### Tests for User Story 1

- [X] T003 [P] [US1] Atualizar `frontend/src/__tests__/App.test.tsx` para cobrir header compartilhado nas rotas da loja, navegação, Conta, badge condicionado ao estado confirmado do carrinho, fallback e ausência do header nas rotas standalone de autenticação.
- [X] T004 [P] [US1] Atualizar `frontend/src/pages/__tests__/CatalogPage.test.tsx` para cobrir renderização e preservação de busca, categoria, filtros, ordenação, paginação e estados de loading, erro e vazio com clientes/estados mockados.

### Implementation for User Story 1

- [X] T005 [US1] Adaptar em `frontend/src/styles.css` as superfícies, textos, campos, filtros, drawer móvel, toolbar, cards, estados, paginação e footer do Catálogo à paleta dark/tech compartilhada, preservando markup, breakpoints e comportamento existentes.

**Checkpoint**: Catálogo e navegação da loja mantêm os fluxos atuais com identidade visual alinhada à Home.

---

## Phase 4: User Story 2 — Consultar detalhes reais de um produto (Priority: P1)

**Goal**: Aplicar a identidade visual da Home aos cards e ao detalhe do produto, preservando dados, fallback de mídia, disponibilidade e integração de carrinho.

**Independent Test**: Com respostas mockadas, carregar produto disponível e indisponível, verificar mídia real/fallback acessível, erro/loading, informação realmente disponível e ação de carrinho inalterada.

### Tests for User Story 2

- [X] T006 [P] [US2] Atualizar `frontend/src/pages/__tests__/ProductDetailPage.test.tsx` para cobrir dados reais, disponibilidade, loading, erro, mídia ausente/falha e ação de carrinho sem alteração da integração atual.
- [X] T007 [P] [US2] Atualizar `frontend/src/components/__tests__/ProductCard.test.tsx` para cobrir imagem real, fallback ausente/com falha, disponibilidade e ações acessíveis do card.

### Implementation for User Story 2

- [X] T008 [US2] Adaptar em `frontend/src/styles.css` a área de mídia, breadcrumb, informações, disponibilidade, controles de quantidade, feedback, estados e ações de `ProductDetailPage` e `ProductCard` ao padrão dark/tech, preservando valores e comportamento.

**Checkpoint**: Cards e detalhe usam identidade visual coerente sem inventar dados nem alterar carrinho.

---

## Phase 5: User Story 3 — Entrar na conta sem alterar autenticação (Priority: P1)

**Goal**: Integrar visualmente o Login à marca da Home, mantendo composição standalone e autenticação intacta.

**Independent Test**: Com mocks, validar composição standalone sem header da loja, campos e teclado, validação, submissão pendente, erro e sucesso com os mesmos dados, sessão e destino de antes.

### Tests for User Story 3

- [X] T009 [P] [US3] Atualizar `frontend/src/pages/__tests__/LoginPage.test.tsx` para cobrir layout standalone, campos, mensagens, loading, falha e sucesso sem alteração do fluxo de autenticação.
- [X] T010 [P] [US3] Atualizar `frontend/src/components/auth/__tests__/LoginForm.test.tsx` para cobrir labels, validação, estado pendente, bloqueio de submissão duplicada, erro e valores enviados existentes.

### Implementation for User Story 3

- [X] T011 [US3] Adaptar em `frontend/src/pages/LoginPage.css` acentos, superfícies, foco, campos, botão, links, mensagens e estados do Login aos tokens dark/tech da Home, preservando o layout standalone, imagem existente, breakpoints e cores semânticas de erro.

**Checkpoint**: Login mantém sua experiência de autenticação standalone com identidade visual integrada.

---

## Phase 6: User Story 4 — Revisar e operar o carrinho com confiança (Priority: P1)

**Goal**: Aplicar o padrão visual dark/tech a linhas, estados, resumo e confirmação do carrinho sem alterar fontes de dados nem operações.

**Independent Test**: Com respostas mockadas, verificar estados autenticado/visitante, carregamento, erro, vazio, itens, disponibilidade, valores conhecidos/indisponíveis, quantidades, remoção, limpeza e badge sem regressões.

### Tests for User Story 4

- [X] T012 [P] [US4] Atualizar `frontend/src/pages/__tests__/CartPage.test.tsx` para cobrir carregamento, erro/retry, carrinho vazio, valores disponíveis/indisponíveis, resumo, remoção, limpeza, feedback e acesso de visitante.
- [X] T013 [P] [US4] Atualizar `frontend/src/components/cart/__tests__/CartItem.test.tsx` para cobrir apresentação de item, mídia/fallback, quantidade/limite, disponibilidade, preço indisponível e operações pendentes.

### Implementation for User Story 4

- [X] T014 [US4] Adaptar em `frontend/src/styles.css` linhas de produto, controles de quantidade, resumo, estados vazio/loading/erro, mensagens, botões e diálogo de limpeza do carrinho à identidade dark/tech, sem alterar valores, lógica ou acessibilidade existente.

**Checkpoint**: Carrinho conserva as operações e os dados autoritativos com apresentação visual coerente.

---

## Phase 7: User Story 5 — Reconhecer o estado indisponível de cadastro (Priority: P2)

**Goal**: Alinhar visualmente o estado existente de cadastro indisponível sem criar formulário ou fluxo.

**Independent Test**: Abrir `/register` e confirmar estado standalone de indisponibilidade e retorno ao Login, sem header da loja, campos ou chamadas de cadastro.

### Tests for User Story 5

- [X] T015 [US5] Criar ou atualizar `frontend/src/pages/__tests__/RegisterUnavailablePage.test.tsx` para verificar mensagem de indisponibilidade, ação de retorno ao Login e ausência de formulário, integração de cadastro e header compartilhado.

### Implementation for User Story 5

- [X] T016 [US5] Adaptar em `frontend/src/pages/LoginPage.css` a marca, mensagem, superfícies, foco e ação de retorno do estado indisponível para os tokens dark/tech, mantendo-o standalone e sem introduzir novos campos ou fluxos.

**Checkpoint**: Login e cadastro indisponível permanecem telas standalone coerentes com a Home.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validar integração visual, acessibilidade, responsividade e ausência de regressões em toda a aplicação.

- [X] T017 Revisar em `frontend/src/styles.css` e `frontend/src/pages/LoginPage.css` foco visível, contraste, estados semânticos, tipografia e breakpoints em todas as superfícies adaptadas, removendo estilos claros/azuis concorrentes sem alterar a paleta da Home.
- [X] T018 Validar em `frontend/src/styles.css` e `frontend/src/pages/LoginPage.css` as rotas adaptadas em 1280 px, 1024 px, 768 px e 375 px; corrigir overflow horizontal e garantir que controles e ações principais continuem utilizáveis, registrando o resultado conforme `specs/013-design-consistency-redesign/quickstart.md`.
- [X] T019 Executar a suíte completa `npm test` e o build `npm run build` via `frontend/package.json`; conferir `git diff` para confirmar que nenhuma alteração alcançou stores, clientes, autenticação, backend, Gateway, contratos ou outros escopos proibidos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências; valida o ponto de partida antes das mudanças.
- **Foundational (Phase 2)**: depende do baseline e bloqueia as adaptações visuais que reutilizam os tokens.
- **User Stories (Phases 3–7)**: dependem dos tokens compartilhados. As histórias são funcionalmente independentes, mas as implementações que editam `frontend/src/styles.css` devem ser sequenciadas para evitar conflitos.
- **Polish (Phase 8)**: depende da conclusão das cinco histórias.

### User Story Dependencies

- **US1 (P1)**: após tokens; independente em comportamento, define a apresentação dark do catálogo e valida o shell.
- **US2 (P1)**: após tokens; independente em comportamento, adapta detalhe e cards.
- **US3 (P1)**: após tokens; independente em comportamento, altera apenas Login e seus testes.
- **US4 (P1)**: após tokens; independente em comportamento, adapta carrinho e seus testes.
- **US5 (P2)**: após tokens; independente em comportamento, adapta o estado standalone de cadastro indisponível.
- **Nota de execução**: apesar da independência funcional, seguir a ordem das fases ao editar as folhas CSS compartilhadas; evitar execução paralela de tarefas que escrevem no mesmo arquivo.

### Parallel Opportunities

- T003 e T004 podem ser executadas em paralelo (arquivos de teste distintos).
- T006 e T007 podem ser executadas em paralelo.
- T009 e T010 podem ser executadas em paralelo.
- T012 e T013 podem ser executadas em paralelo.
- T015 pode ser desenvolvido/testado sem alterações às outras páginas; coordenar sua implementação CSS com T011 por ambas editarem `LoginPage.css`.
- T005, T008 e T014 alteram `styles.css` e não devem ser executadas simultaneamente.

## Implementation Strategy

### MVP First (User Story 1)

1. Registrar o baseline de testes e build.
2. Consolidar tokens visuais sem alterar a Home.
3. Atualizar testes do shell e catálogo.
4. Adaptar visualmente o catálogo e confirmar navegação/header preservados.
5. Validar independentemente os critérios de US1 antes de prosseguir.

### Incremental Delivery

1. Completar US2, US3, US4 e US5 na ordem indicada, atualizando testes antes de cada adaptação visual.
2. Após cada história, executar seus testes específicos e verificar que operações, estado e conteúdo permanecem inalterados.
3. Finalizar com inspeção manual dos quatro viewports, testes completos, build e revisão do escopo do diff.
