# Relatório de Análise: Conclusão do Carrinho no Frontend

**Data**: 2026-10-02  
**Feature**: `011-frontend-cart-completion`  
**Escopo da análise**: consistência entre especificação, plano, contrato de consumo e tarefas. A análise foi somente leitura; nenhum artefato existente foi alterado.

## Achados

| ID | Categoria | Severidade | Localização | Achado | Recomendação |
|---|---|---|---|---|---|
| A1 | Constituição/contrato | CRÍTICA | [plan.md](plan.md), [contrato de consumo](contracts/cart-gateway-consumption.md), [OpenAPI versionado](../004-cart/contracts/cart-service-api.openapi.yaml) | O OpenAPI não corresponde aos DTOs/controllers atuais. O plano registra a divergência, mas classifica o princípio V como aprovado. A constituição exige contratos OpenAPI documentados; essa ressalva não resolve o conflito preexistente. | Resolver a documentação do contrato em uma mudança separada e aprovada antes da implementação. Não alterar backend ou contrato dentro desta feature. |
| A2 | Ordem de execução | MÉDIA | [tasks.md](tasks.md) | O plano permite executar US1 e US2 em paralelo, mas ambas alteram `cartStore.ts`, `CartItem.tsx` e `CartPage.tsx`. Isso conflita com a orientação de integrar esses arquivos em sequência. | Tornar US2 dependente de US1 ou restringir o paralelismo a tarefas em arquivos independentes. |
| A3 | Cobertura de testes | MÉDIA | [spec.md](spec.md), [tasks.md](tasks.md), [App.test.tsx](../../frontend/src/__tests__/App.test.tsx) | RF-012 requer contador sincronizado após adicionar, alterar quantidade, remover e limpar. O teste atual do header verifica a carga inicial; as tarefas não exigem teste explícito do badge após cada mutação. | Acrescentar testes que confirmem o badge após POST, PUT, DELETE de item e limpeza. |
| A4 | Limite na inclusão | MÉDIA | [plan.md](plan.md), [tasks.md](tasks.md), [ProductDetailPage.tsx](../../frontend/src/pages/ProductDetailPage.tsx) | O limite retornado é coberto para os controles do carrinho, mas não está definido o comportamento da inclusão no detalhe enquanto o GET inicial ainda não carregou `maxItemQuantity`. A store inicia com o valor padrão `99`, e a API pode ter outro limite configurado. | Definir e testar o comportamento da inclusão até que o limite oficial esteja carregado, sem substituir a validação do serviço. |

## Cobertura de Requisitos

Há tarefa relacionada a cada requisito funcional e critério de sucesso (24/24). Essa associação não significa que cada critério já tenha teste específico; RF-012 tem uma lacuna de teste descrita em A3.

| Requisito | Tarefas | Requisito | Tarefas |
|---|---|---|---|
| RF-001 | T004, T007 | RF-010 | T018, T021 |
| RF-002 | T005, T008, T009 | RF-011 | T019, T022–T024 |
| RF-003 | T004, T007, T009 | RF-012 | T016, T021, T022, T026 |
| RF-004 | T005, T008, T009 | RF-013 | T005, T013, T019, T023, T025, T027 |
| RF-005 | T006, T014, T016 | RF-014 | T002, T003, T025 |
| RF-006 | T011, T015 | RF-015 | T020, T026 |
| RF-007 | T012, T013, T018, T019, T021, T023 | RF-016 | T004, T007, T032 |
| RF-008 | T012, T013, T016, T017 | RF-017 | T025, T028, T029, T031 |
| RF-009 | T004, T007, T015 | RF-018 | T032 |
| CS-001 | T004–T009 | CS-004 | T002, T003, T011, T012, T015 |
| CS-002 | T004, T005, T007–T009 | CS-005 | T004, T005, T011, T014, T018, T019, T025 |
| CS-003 | T006, T012, T016, T018, T021, T022, T026 | CS-006 | T028, T031 |

## Métricas

- Requisitos funcionais e critérios de sucesso: 24
- Requisitos com ao menos uma tarefa associada: 24/24 (100%)
- Tarefas: 32
- Ambiguidades ou duplicações materiais: 0
- Achados: 4, sendo 1 crítico
- Tarefas cross-cutting sem história específica: T001 e T029–T031

## Próximos Passos

1. Resolver A1 fora do escopo desta feature, em mudança documental aprovada, antes de implementar.
2. Corrigir A2–A4 em `plan.md` e `tasks.md`.
3. Executar `/speckit.analyze` novamente após as correções.

Os hooks de extensão não foram executados porque `.specify/extensions.yml` não existe.