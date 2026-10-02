# Tasks: Conclusão do Carrinho no Frontend

**Input**: Design documents from `/specs/011-frontend-cart-completion/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cart-gateway-consumption.md](contracts/cart-gateway-consumption.md), [quickstart.md](quickstart.md)

**Tests**: Incluídos porque os critérios de sucesso e o plano exigem testes automatizados para valores, mutações, erros, autenticação e sincronização.

**Organization**: Tarefas agrupadas pelas quatro histórias da especificação. Os testes de cada fatia precedem sua implementação.

## Phase 1: Setup

**Purpose**: Confirmar que a feature pode usar a configuração frontend existente, sem introduzir ferramentas ou dependências.

- [X] T001 Confirmar scripts de teste/build e dependências existentes em `frontend/package.json`; manter o manifesto sem novas dependências.

---

## Phase 2: Foundational

**Purpose**: Completar o tratamento compartilhado de erros HTTP antes das histórias que dependem de feedback confiável do serviço.

- [X] T002 [P] Adicionar casos de teste para respostas `400`, `403`, `422`, `5xx` e detalhes úteis de `ErrorResponse` em `frontend/src/services/__tests__/apiClient.test.ts`.
- [X] T003 Mapear erros HTTP do carrinho para mensagens amigáveis, preservando detalhes seguros e sem alterar erros do catálogo em `frontend/src/services/apiClient.ts`.

**Checkpoint**: Erros do carrinho são normalizados na fronteira HTTP; histórias podem avançar usando o cliente existente.

---

## Phase 3: User Story 1 - Entender os valores do carrinho (Priority: P1) MVP

**Goal**: Apresentar preço unitário, subtotal por linha, contagens e total exclusivamente a partir dos dados confirmados.

**Independent Test**: Mockar carrinhos com preços conhecidos e desconhecidos; confirmar valores iguais aos recebidos, sem preço/total inventado e sem UUID apresentado.

### Tests for User Story 1

- [X] T004 [P] [US1] Cobrir preço unitário rotulado, subtotal recebido, preço/subtotal nulos e ausência de UUID visível em `frontend/src/components/cart/__tests__/CartItem.test.tsx`.
- [X] T005 [P] [US1] Cobrir carrinho vazio, um e vários produtos, unidades, total confirmado e total indisponível em `frontend/src/pages/__tests__/CartPage.test.tsx`.
- [X] T006 [P] [US1] Cobrir adoção de `total`/`totalAvailable` da resposta sem soma local de subtotais em `frontend/src/store/__tests__/cartStore.test.ts`.

### Implementation for User Story 1

- [X] T007 [US1] Renderizar snapshot unitário e subtotal somente quando disponíveis, informar indisponibilidade e remover `productId` da apresentação em `frontend/src/components/cart/CartItem.tsx`.
- [X] T008 [US1] Apresentar produtos distintos, unidades e total fornecido pelo store, incluindo aviso de total indisponível, em `frontend/src/pages/CartPage.tsx`.
- [X] T009 [US1] Remover o cálculo local de total como fallback e manter os campos financeiros somente a partir de respostas autoritativas em `frontend/src/store/cartStore.ts`.
- [X] T010 [P] [US1] Ajustar hierarquia visual dos valores, indisponibilidade e resumo sem quebrar os breakpoints existentes em `frontend/src/styles.css`.

**Checkpoint**: A UI mostra valores de API e nunca converte preço desconhecido em zero.

---

## Phase 4: User Story 2 - Ajustar quantidades com segurança (Priority: P1)

**Goal**: Atualizar quantidades sem estado otimista divergente, respeitando o limite recebido e tratando rejeições do serviço.

**Independent Test**: Simular sucesso de PUT/POST, limite, conflito de estoque e request pendente; confirmar quantidade apenas após resposta e preservação do último estado confirmado em erro.

### Tests for User Story 2

- [X] T011 [P] [US2] Cobrir controles no limite, decremento mínimo, loading e indisponibilidade de alteração em `frontend/src/components/cart/__tests__/CartItem.test.tsx`.
- [X] T012 [P] [US2] Cobrir respostas de conflito, quantidade anterior preservada, bloqueio de mutações concorrentes e recusa de inclusão até carregar `maxItemQuantity` oficial em `frontend/src/store/__tests__/cartStore.test.ts`.
- [X] T013 [P] [US2] Cobrir estado pendente e feedback amigável ao falhar alteração de quantidade em `frontend/src/pages/__tests__/CartPage.test.tsx`.
- [X] T014 [P] [US2] Cobrir inclusão bloqueada em `idle`/`loading`/`error`, retry até carregar o limite oficial, bloqueio acima do limite carregado e consolidação `200`/nova linha `201` em `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`.

### Implementation for User Story 2

- [X] T015 [US2] Garantir controles de quantidade orientados por `maxItemQuantity`, desabilitados durante operação e sem PUT para itens indisponíveis em `frontend/src/components/cart/CartItem.tsx`.
- [X] T016 [US2] Aplicar somente o `CartResponse` confirmado após POST/PUT, preservar estado anterior em erro, manter serialização e bloquear inclusão até o limite oficial estar carregado em `frontend/src/store/cartStore.ts`.
- [X] T017 [US2] Manter inclusão desabilitada enquanto o carrinho estiver `idle`/`loading` ou em erro, exibir feedback de carregamento e oferecer retry do GET em `frontend/src/pages/ProductDetailPage.tsx`.
- [X] T018 [US2] Apresentar feedback de erro/loading sem alterar quantidade exibida antes da confirmação em `frontend/src/pages/CartPage.tsx`.

**Checkpoint**: Inclusão e atualização refletem a resposta do serviço; conflito não altera quantidade local confirmada.

---

## Phase 5: User Story 3 - Remover itens ou esvaziar o carrinho (Priority: P1)

**Goal**: Refletir DELETE confirmado imediatamente nas linhas/contador e buscar separadamente o resumo oficial, inclusive após limpeza.

**Independent Test**: Simular `204` seguido de GET bem-sucedido ou falho; confirmar remoção/limpeza não é revertida, resumo não é calculado e retry não repete DELETE.

### Tests for User Story 3

- [X] T019 [P] [US3] Cobrir remoção, limpeza, GET posterior, erro do GET sem restauração da mutação e retry somente de consulta em `frontend/src/store/__tests__/cartStore.test.ts`.
- [X] T020 [P] [US3] Cobrir remoção de item sem preço, remoção do último item, confirmação/cancelamento de limpeza e falha/retry do resumo em `frontend/src/pages/__tests__/CartPage.test.tsx`.
- [X] T021 [P] [US3] Confirmar métodos DELETE sem corpo e paths existentes para item e carrinho em `frontend/src/services/__tests__/apiClient.test.ts`.

### Implementation for User Story 3

- [X] T022 [US3] Separar sucesso do DELETE da leitura subsequente; aplicar remoção confirmada, invalidar resumo e buscar carrinho sem cálculo local em `frontend/src/store/cartStore.ts`.
- [X] T023 [US3] Após limpeza confirmada, manter linhas vazias e consultar o resumo oficial; em falha, não restaurar itens nem repetir DELETE no retry em `frontend/src/store/cartStore.ts`.
- [X] T024 [US3] Exibir loading da remoção/limpeza, total indisponível quando a reconciliação falhar e retry que não repete mutação em `frontend/src/pages/CartPage.tsx`.
- [X] T025 [US3] Preservar a confirmação acessível de limpeza e o estado vazio com navegação ao catálogo em `frontend/src/pages/CartPage.tsx`.

**Checkpoint**: DELETE confirmado não é desfeito por falha de GET; total sempre vem da API.

---

## Phase 6: User Story 4 - Reconhecer estados e continuar navegando (Priority: P2)

**Goal**: Tornar loading, sessão, erro, vazio, feedback e responsividade claros e acessíveis.

**Independent Test**: Exercitar loading inicial, vazio, sem sessão, `401`, erro `5xx`/rede, retry e layouts nos breakpoints móveis/tablet/desktop.

### Tests for User Story 4

- [X] T026 [P] [US4] Cobrir loading inicial, carrinho vazio, retry de erro de leitura e sessão não autenticada em `frontend/src/pages/__tests__/CartPage.test.tsx`.
- [X] T027 [P] [US4] Cobrir invalidação do carrinho e da sessão após `401` em `frontend/src/__tests__/App.test.tsx`.
- [X] T028 [US4] Verificar no header o contador derivado das quantidades confirmadas após adicionar (POST), aumentar/diminuir (PUT), remover (DELETE) e limpar; assegurar que o total financeiro exibido permanece o `total` retornado em `frontend/src/__tests__/App.test.tsx`.

### Implementation for User Story 4

- [X] T029 [US4] Distinguir erro inicial de erro de reconciliação e expor retry apropriado sem apagar estado confirmado em `frontend/src/pages/CartPage.tsx`.
- [X] T030 [US4] Ajustar feedback de operação e estados de resumo em `frontend/src/styles.css` para mobile, tablet e desktop, sem overflow horizontal.

**Checkpoint**: Estados recuperáveis e vazios são claros; autenticação e badge continuam ligados ao estado real.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Executar os gates do projeto e conferir os fluxos integrados definidos no guia.

- [X] T031 [P] Revisar acessibilidade por teclado, rótulos e mensagens anunciáveis nos componentes de carrinho em `frontend/src/components/cart/CartItem.tsx` e `frontend/src/pages/CartPage.tsx`.
- [X] T032 Executar a suíte Vitest completa e corrigir regressões frontend conforme scripts em `frontend/package.json`.
- [X] T033 Executar typecheck/build frontend conforme scripts em `frontend/package.json`.
- [X] T034 Conferir que nenhum UUID aparece na interface e que nenhuma regra financeira/estoque ou alteração backend foi introduzida em `frontend/src/`.
- [ ] T035 Executar validação manual autenticada do [quickstart.md](quickstart.md) contra API Gateway/Cart Service disponíveis, cobrindo mutações e resumo financeiro; requer ambiente integrado e JWT de teste.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências; confirma toolchain existente e ausência de pacote novo.
- **Foundational (Phase 2)**: Depende de Setup; a normalização de erros é compartilhada pelas histórias.
- **US1 (Phase 3)**: Depende de Foundational; fornece o MVP de apresentação fiel dos valores.
- **US2 (Phase 4)**: Depende da conclusão de US1. Ambas alteram `cartStore.ts`, `CartItem.tsx` e `CartPage.tsx`, portanto não devem ser executadas em paralelo; concluir e validar US1 antes de iniciar implementação/testes de US2.
- **US3 (Phase 5)**: Depende de US1 e US2. Requer estado financeiro autoritativo e respostas de mutação confirmadas antes de adicionar reconciliação após DELETE.
- **US4 (Phase 6)**: Depende da fundação e integra estados das histórias anteriores.
- **Polish (Phase 7)**: Depende das histórias que serão entregues; gates finais só passam após integração.

### User Story Dependencies

- **US1 (P1)**: Pode começar após Phase 2; independente e MVP recomendado.
- **US2 (P1)**: Começa somente após o checkpoint de US1; compartilha `cartStore.ts`, `CartItem.tsx` e `CartPage.tsx`, então não é paralela a US1.
- **US3 (P1)**: Começa somente após os checkpoints de US1 e US2; compartilha store/página e depende dos fluxos de mutação estabilizados. Os testes de DELETE devem garantir que o GET falho não reverte o `204`.
- **US4 (P2)**: Integra erros/estados das histórias anteriores; completar antes do polish.

### Within Each User Story

- Escrever/ajustar testes antes da implementação e verificar que os novos cenários falham pela razão esperada.
- Implementação só altera valores quando existe resposta confirmada; DELETE é confirmado pelo `204` e a consulta financeira é etapa separada.
- Reexecutar testes focados da história antes de avançar ao próximo checkpoint.

## Parallel Opportunities

- T002 pode ser feito em paralelo com T001; T003 depende dos testes T002.
- Em US1, T004, T005 e T006 são testes em arquivos distintos e podem ser preparados em paralelo; T010 também pode ser feito em paralelo com alterações em TypeScript.
- Em US2, somente após US1: T011–T014 são testes em arquivos distintos e podem ser preparados em paralelo; T015–T018 devem seguir seus testes e podem ser paralelizados apenas quando cada atividade tocar arquivos distintos.
- Em US3, T019–T021 podem ser preparados em paralelo em arquivos distintos; T022/T023 compartilham `cartStore.ts` e devem ser implementados juntos ou em sequência.
- Em US4, T026 e T027 podem ser preparados em paralelo; T028 verifica o contador após as mutações confirmadas e o valor financeiro retornado, sem adicionar cálculo monetário no header.
- Nenhuma tarefa de US2 pode ser paralelizada com US1; US1 e US2 compartilham arquivos de implementação e testes.

### Parallel Example: User Story 1

```text
Task: T004 atualizar testes de linha em frontend/src/components/cart/__tests__/CartItem.test.tsx
Task: T005 atualizar testes da página em frontend/src/pages/__tests__/CartPage.test.tsx
Task: T006 atualizar testes do store em frontend/src/store/__tests__/cartStore.test.ts
Task: T010 revisar estilos em frontend/src/styles.css
```

## Implementation Strategy

### MVP (User Story 1)

1. Confirmar toolchain existente e concluir tratamento HTTP compartilhado.
2. Implementar US1 e validar preço unitário, subtotal, total e estados sem preço.
3. Parar no checkpoint e executar testes focados/build para demonstrar o núcleo da feature.

### Incremental Delivery

1. Entregar US1 para apresentação financeira confiável.
2. Integrar US2 para quantidade, limite e erros de estoque sem estado otimista.
3. Integrar US3 para remoção/limpeza e reconciliação financeira pós-DELETE.
4. Completar US4 e executar todos os gates e verificações responsivas.

## Notes

- `[P]` indica tarefas em arquivos distintos sem dependência de tarefa incompleta.
- `[US1]` a `[US4]` correspondem às histórias da especificação.
- Não criar nem alterar arquivos backend, migrations, JWT, API Gateway ou OpenAPI nesta feature.
- Não adicionar biblioteca, cálculo financeiro local, SKU inventado, rota de checkout ou pagamento.
- Enquanto `maxItemQuantity` da sessão atual não estiver confirmado por GET, manter a inclusão bloqueada; `99` é apenas valor inicial técnico e nunca uma autorização.
- A divergência do OpenAPI é preexistente, permanece documentada e não bloqueia esta implementação frontend; nenhuma mudança do contrato ocorre nesta feature.
- Não criar commits como parte destas tarefas.