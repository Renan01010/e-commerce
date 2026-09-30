# Tasks: Catálogo de Produtos no Frontend

**Input**: Design documents from `specs/006-frontend-product-catalog/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/catalog-gateway-consumption.md` e `quickstart.md`

**Organization**: tarefas agrupadas por história de usuário e ordenadas por dependência.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar a superfície existente do frontend e preparar a execução incremental.

- [x] T001 Confirmar scripts `test` e `build` em `frontend/package.json` e a base URL do cliente em `frontend/src/services/apiClient.ts`
- [x] T002 [P] Confirmar as rotas públicas `/` e `/products/:id` em `frontend/src/App.tsx` e registrar os cenários de validação no quickstart da feature em `specs/006-frontend-product-catalog/quickstart.md`

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: estabilizar o contrato consumido e as regras compartilhadas antes das histórias.

- [x] T003 [P] Alinhar tipos `Product`, `Category`, `ProductPage`, filtros, ordenação e paginação ao contrato em `frontend/src/types/catalog.ts`
- [x] T004 [P] Cobrir no cliente HTTP os caminhos, parâmetros opcionais, paginação e ausência de `cost` em `frontend/src/services/__tests__/apiClient.test.ts`
- [x] T005 Consolidar o estado inicial, carregamento, erro, produto selecionado e reinício de página no store em `frontend/src/store/catalogStore.ts`
- [x] T006 [P] Definir nos testes os nomes acessíveis e os estados esperados de busca, filtros, ordenação, paginação e mensagens em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

**Checkpoint**: contrato, estado compartilhado e critérios de teste estão definidos; as histórias podem ser implementadas.

## Phase 3: User Story 1 - Explorar produtos (Priority: P1) 🎯 MVP

**Goal**: permitir que um visitante navegue por produtos ativos em cards e páginas, sem login.

**Independent Test**: abrir `/` com uma resposta paginada, confirmar cards com nome/preço/imagem ou substituto/disponibilidade, trocar de página e verificar que o produto inativo não é mostrado.

### Tests for User Story 1

- [x] T007 [P] [US1] Testar card com nome, preço em BRL, imagem/substituto, disponibilidade e link para `/products/:id` em `frontend/src/components/__tests__/ProductCard.test.tsx`
- [x] T008 [P] [US1] Testar carregamento da listagem, total de resultados, paginação e resposta sem produtos em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 1

- [x] T009 [US1] Implementar o mapeamento da resposta paginada e o carregamento de produtos ativos através de `GET /api/products` em `frontend/src/services/apiClient.ts` e `frontend/src/store/catalogStore.ts`
- [x] T010 [US1] Renderizar cards com nome, preço, imagem/substituto e disponibilidade derivada de `quantity` em `frontend/src/components/ProductCard.tsx`
- [x] T011 [US1] Renderizar a grade de produtos, total, paginação e navegação para detalhes em `frontend/src/pages/CatalogPage.tsx`
- [x] T012 [US1] Manter a rota de catálogo pública e a navegação do card para `/products/:id` em `frontend/src/App.tsx`

**Checkpoint**: US1 é demonstrável e testável sem pesquisa, filtros ou login.

## Phase 4: User Story 2 - Encontrar e refinar produtos (Priority: P1)

**Goal**: permitir pesquisa, filtros cumulativos, ordenação e limpeza de critérios com retorno à primeira página.

**Independent Test**: iniciar em uma página posterior, alterar busca/filtro/ordenação, confirmar nova consulta com os parâmetros suportados, retorno à página zero e limpeza dos critérios.

### Tests for User Story 2

- [x] T013 [P] [US2] Testar composição de `query`, categoria, faixa de preço, marca, estoque, ordenação e paginação em `frontend/src/services/__tests__/apiClient.test.ts`
- [x] T014 [P] [US2] Testar que alterações de busca, filtro e ordenação reiniciam a página e que limpar remove os critérios em `frontend/src/store/__tests__/catalogStore.test.ts`
- [x] T015 [US2] Testar interação de busca, filtros, ordenação, resumo da consulta e paginação em `frontend/src/pages/__tests__/CatalogPage.test.tsx`

### Implementation for User Story 2

- [x] T016 [US2] Encaminhar somente parâmetros preenchidos do estado para `GET /api/products` em `frontend/src/services/apiClient.ts`
- [x] T017 [US2] Implementar atualização de consulta, combinação cumulativa, limpeza e retorno à página zero em `frontend/src/store/catalogStore.ts`
- [x] T018 [US2] Implementar controles de pesquisa, categoria hierárquica, preço, marca, disponibilidade, ordenação e paginação em `frontend/src/pages/CatalogPage.tsx`
- [x] T019 [US2] Carregar categorias pelo endpoint existente e apresentar nomes/hierarquia sem expor IDs como rótulos em `frontend/src/pages/CatalogPage.tsx` e `frontend/src/store/catalogStore.ts`

**Checkpoint**: US1 e US2 funcionam juntas, mas US2 também pode ser validada com respostas mockadas do contrato.

## Phase 5: User Story 3 - Consultar detalhes de um produto (Priority: P1)

**Goal**: abrir detalhes do produto selecionado, contextualizar categoria e retornar ao catálogo sem ativar compra.

**Independent Test**: abrir um card, confirmar os dados do produto e a categoria navegável, voltar ao catálogo e abrir um identificador inexistente/inativo.

### Tests for User Story 3

- [x] T020 [P] [US3] Testar carregamento de detalhes, campos públicos, preço, disponibilidade, retorno e botão de carrinho desabilitado em `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`
- [x] T021 [P] [US3] Testar rota `/products/:id`, fallback de produto não encontrado e retorno ao catálogo em `frontend/src/App.tsx` e `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`

### Implementation for User Story 3

- [x] T022 [US3] Carregar produto por identificador via `GET /api/products/{id}` e limpar seleção antes da consulta em `frontend/src/store/catalogStore.ts` e `frontend/src/services/apiClient.ts`
- [x] T023 [US3] Renderizar detalhes públicos, imagem/substituto, categoria, disponibilidade e preço sem exibir `cost` em `frontend/src/pages/ProductDetailPage.tsx`
- [x] T024 [US3] Implementar breadcrumbs, link de categoria, retorno ao catálogo e estado seguro para produto inexistente/inativo em `frontend/src/pages/ProductDetailPage.tsx`
- [x] T025 [US3] Garantir que a ação de carrinho permaneça desabilitada e não faça chamadas de carrinho em `frontend/src/pages/ProductDetailPage.tsx`

**Checkpoint**: US3 é uma jornada completa de descoberta para detalhe, sem checkout ou mutação de dados.

## Phase 6: User Story 4 - Entender os estados do catálogo (Priority: P2)

**Goal**: tornar carregamento, vazio e falha distinguíveis, recuperáveis e acessíveis.

**Independent Test**: simular carregamento lento, resposta vazia, falha de rede/serviço, categorias indisponíveis e erro de detalhe; verificar mensagens seguras e ações de recuperação.

### Tests for User Story 4

- [x] T026 [P] [US4] Testar skeleton/estado de carregamento, erro recuperável, estado vazio e preservação dos filtros em `frontend/src/pages/__tests__/CatalogPage.test.tsx`
- [x] T027 [P] [US4] Testar carregamento, erro e não encontrado sem dados antigos ou de outro produto em `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`
- [x] T028 [P] [US4] Testar imagem ausente/erro, disponibilidade zero, preço zero e nomes acessíveis em `frontend/src/components/__tests__/ProductCard.test.tsx`

### Implementation for User Story 4

- [x] T029 [US4] Separar estados de carregamento, vazio e erro e adicionar nova tentativa/retorno sem apagar critérios em `frontend/src/pages/CatalogPage.tsx`
- [x] T030 [US4] Tratar falha de categorias sem expor UUIDs e manter fallback seguro de contexto em `frontend/src/pages/CatalogPage.tsx` e `frontend/src/store/catalogStore.ts`
- [x] T031 [US4] Tratar imagem ausente ou falha de carregamento, quantidade zero e preço zero no componente em `frontend/src/components/ProductCard.tsx` e `frontend/src/pages/ProductDetailPage.tsx`
- [x] T032 [US4] Garantir labels, roles, foco/navegação por teclado e feedback textual dos controles em `frontend/src/pages/CatalogPage.tsx`, `frontend/src/pages/ProductDetailPage.tsx` e `frontend/src/components/ProductCard.tsx`

**Checkpoint**: todas as histórias têm estados de sucesso e falha verificáveis.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: validar integração, responsividade e limites da feature.

- [x] T033 [P] Verificar layout entre 320 px e 1440 px sem corte ou sobreposição em `frontend/src/styles.css`
- [x] T034 [P] Adicionar ou ajustar testes de integração das rotas públicas e ausência de chamadas de carrinho em `frontend/src/pages/__tests__/CatalogPage.test.tsx` e `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`
- [x] T035 [P] Confirmar que login, cadastro indisponível, autenticação, Gateway e backend permanecem sem alterações funcionais em `frontend/src/App.tsx`, `frontend/src/services/apiClient.ts` e `backend/api-gateway/src/main/resources/application.yml`
- [x] T036 Executar `npm test` e `npm run build` em `frontend/` e seguir os cenários do `specs/006-frontend-product-catalog/quickstart.md`

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências; confirma ambiente e superfície existente.
- **Foundational (Phase 2)**: depende da Setup e bloqueia as histórias.
- **US1 (Phase 3)**: depende da Foundation; é o MVP.
- **US2 (Phase 4)**: depende da Foundation e integra com US1; pode ser testada com mocks.
- **US3 (Phase 5)**: depende da Foundation e da rota/componentes de US1.
- **US4 (Phase 6)**: depende das telas de US1-US3 para cobrir seus estados.
- **Polish (Phase 7)**: depende das histórias desejadas concluídas.

### User Story Dependencies

- **US1**: independente após a Foundation.
- **US2**: depende da superfície de listagem da US1, mas seus testes de contrato/store são isoláveis.
- **US3**: depende da navegação de card/rota criada na US1.
- **US4**: depende das implementações das telas para validar todos os estados; não cria escopo de negócio novo.

### Parallel Opportunities

- T003, T004 e T006 podem ser executadas em paralelo.
- Dentro da US1, T007 e T008 podem ser escritas em paralelo; depois T009-T012 seguem a ordem de dependência.
- Dentro da US2, T013-T015 podem ser escritos em paralelo; T016-T019 exigem o contrato e o store estabilizados.
- Dentro da US3, T020 e T021 podem ser preparados em paralelo; T022 precede T023-T025.
- Dentro da US4, T026-T028 podem ser escritos em paralelo; T029-T032 podem ser separados por arquivo quando não houver conflito.
- T033-T035 são paralelizáveis; T036 é a validação final.

## Parallel Example: User Story 1

```text
T007: testes do ProductCard em frontend/src/components/__tests__/ProductCard.test.tsx
T008: testes da listagem em frontend/src/pages/__tests__/CatalogPage.test.tsx
```

## Parallel Example: User Story 2

```text
T013: contrato de parâmetros em frontend/src/services/__tests__/apiClient.test.ts
T014: transições do store em frontend/src/store/__tests__/catalogStore.test.ts
T015: interação da página em frontend/src/pages/__tests__/CatalogPage.test.tsx
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Concluir Setup e Foundation.
2. Implementar US1: listagem, cards, paginação e navegação para detalhe.
3. Executar os testes da US1 e validar o quickstart.
4. Parar para demonstração antes de adicionar filtros, detalhe completo e estados avançados.

### Incremental Delivery

1. Adicionar US2 para pesquisa, filtros e ordenação.
2. Adicionar US3 para detalhes e navegação contextual.
3. Adicionar US4 para estados, acessibilidade e recuperação de falhas.
4. Executar Polish e validação final sem ativar carrinho, checkout ou administração.

## Notes

- `[P]` indica tarefas em arquivos diferentes e sem dependência incompleta.
- `[USn]` vincula a tarefa à história correspondente da spec.
- Cada tarefa possui ID sequencial, caminho de arquivo e resultado observável.
- Nenhuma tarefa altera Product Service, API Gateway ou contratos backend.
