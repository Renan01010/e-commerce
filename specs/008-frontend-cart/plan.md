# Plano de Implementação: Carrinho no Frontend

**Branch**: `008-frontend-cart` | **Data**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Entrada**: [Especificação clarificada](spec.md), compatível com o contrato aprovado em `004-cart`.

## Resumo

Adicionar ao SPA existente a experiência de carrinho autenticado: acesso à rota `/cart`, leitura e manutenção dos itens através do API Gateway, inclusão a partir do detalhe do produto e badge compartilhado no cabeçalho. O plano reutiliza Axios, Zustand, React Router, controles visuais e testes presentes. O Cart Service permanece fonte de verdade para identidade, validação do produto, quantidades e persistência; não haverá mudanças no backend ou novas dependências.

O estado de carrinho será carregado sob demanda uma vez por sessão e reutilizado entre cabeçalho, detalhe e página do carrinho. POST/PUT substituem o estado pelo `CartResponse` confirmado; DELETE de item e limpeza atualizam o estado somente após `204`. Não haverá atualização otimista nem cálculo de total financeiro.

## Contexto Técnico

**Linguagem/Versão**: TypeScript 5.7.2 e React 18.3.1.

**Dependências Primárias**: Vite 5.4.11, React Router 6.28.1, Axios 1.7.9, Zustand 5.0.2, Vitest 2.1.5, React Testing Library 16.1.0 e `@testing-library/user-event` 14.5.2.

**Armazenamento**: Nenhuma persistência frontend. A sessão e o estado do carrinho permanecem em memória; linhas e quantidades persistem exclusivamente no Cart Service/PostgreSQL existentes.

**Testes**: Vitest e React Testing Library; ampliar testes de `apiClient`, stores, `App`, `ProductDetailPage` e componentes/página do carrinho. Build/typecheck pelo script `npm run build`.

**Plataforma Alvo**: Navegadores desktop, tablet e mobile, com suporte responsivo a partir de 320 px; aplicação Vite atual.

**Tipo de Projeto**: Single-page application React/TypeScript.

**Metas de Desempenho**: Sem novo SLA. Reutilizar timeout HTTP atual de 10 segundos, evitar GET por renderização e desduplicar carregamentos simultâneos para a sessão atual.

**Restrições**: Consumir somente `GET/POST/PUT/DELETE /api/cart...` via `apiClient`; autenticação pelo interceptor existente; nenhum `userId` no frontend; manter `product` nulo válido; sem checkout, total, regra de estoque, armazenamento local de token, APIs ou alterações backend.

**Escala/Âmbito**: Uma rota `/cart`, uma integração de API para cinco operações já existentes, estado global de linhas/feedback, badge no cabeçalho, alteração do detalhe do produto e componentes focados para linha, estado vazio e confirmação de limpeza.

## Verificação da Constituição

*Gate: deve passar antes da pesquisa e ser reavaliado após o design.*

| Princípio | Estado | Evidência |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Nenhum serviço/database novo; Cart Service continua dono do carrinho e Product Service do catálogo. |
| II. Arquitetura Hexagonal | PASS | Sem alteração backend; comunicação frontend permanece encapsulada em serviço HTTP. |
| III. Clean Code/SOLID | PASS | API, estado compartilhado, apresentação da linha e página mantêm responsabilidades separadas sem nova abstração de domínio. |
| IV. Test-first | PASS | Plano contempla testes de API/store, estados de componentes, fluxo de detalhe, sessão e navegação. |
| V. API e contratos | PASS | Reutiliza sem alterações o OpenAPI `specs/004-cart/contracts/cart-service-api.openapi.yaml`. |
| VI. Segurança por padrão | PASS | JWT vem da sessão em memória via interceptor; sem `userId`, tokens em storage ou exposição de credenciais. |
| VII. Observabilidade | N/A | Nenhum serviço backend ou telemetria nova. |
| VIII. Arquitetura frontend | PASS | Estado, integração HTTP, páginas e componentes permanecem separados; não replicam regras autoritativas. |
| IX. Reprodutibilidade | PASS | Utiliza scripts e dependências já presentes no `frontend/package.json`. |
| X. Spec-driven Development | PASS | Especificação clarificada precede plano; tasks serão geradas pelo comando subsequente. |

**Resultado antes da pesquisa**: PASS. Nenhuma violação constitucional exige exceção.

**Resultado após o design**: PASS. A proposta não adiciona pacote, persistência, endpoint ou serviço; quantidade e disponibilidade continuam autoritativas no backend.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/008-frontend-cart/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── cart-gateway-consumption.md
└── tasks.md             # será criado por /speckit.tasks
```

### Código-fonte planejado

```text
frontend/
├── package.json                         # sem dependências novas
└── src/
    ├── App.tsx                          # rota /cart, link de header e badge
    ├── components/
    │   └── cart/                        # linha, quantidade e confirmação acessível
    ├── pages/
    │   ├── CartPage.tsx
    │   └── __tests__/
    │       └── CartPage.test.tsx
    ├── services/
    │   ├── apiClient.ts                 # adicionar cartApi sobre o Axios existente
    │   └── __tests__/
    │       └── apiClient.test.ts        # verificar paths, payloads e 204
    ├── store/
    │   ├── authStore.ts                 # reutilizar sessão/clearSession existentes
    │   ├── cartStore.ts                 # estado em memória, sem persist middleware
    │   └── __tests__/
    │       └── cartStore.test.ts
    ├── pages/__tests__/
    │   └── ProductDetailPage.test.tsx  # substituir expectativa de placeholder
    └── styles.css                       # estilos responsivos coerentes com a loja
```

**Decisão de estrutura**: manter o projeto e padrões atuais. `cartApi` adiciona métodos ao serviço/cliente HTTP existente; `cartStore` coordena carga, mutações confirmadas, feedback e estado compartilhado; a página e componentes renderizam cada linha e estado; `StoreLayout` exibe navegação e badge derivado. O store observa mudanças de sessão sem persistir token ou carrinho; chamadas simultâneas de carga inicial para a mesma sessão compartilham o mesmo request. Endpoints e estado autoritativo continuam no backend.

## Sequência de Implementação

1. Definir tipos TypeScript para `CartResponse`, `CartItem` e `ProductSummary` correspondentes ao OpenAPI, incluindo `product: null`.
2. Acrescentar `cartApi` ao cliente Axios compartilhado para GET/POST/PUT/DELETE, com mensagens próprias para erro de carrinho sem mudar o tratamento do catálogo.
3. Criar o `cartStore` em memória: carga inicial deduplicada por sessão, linhas confirmadas, estados de carregamento/erro/sucesso e badge derivado da soma das quantidades. Limpar dados ao invalidar, encerrar ou substituir sessão; em `401`, invalidar a sessão pela ação existente e apresentar orientação inline sem sair de `/cart`.
4. Integrar o store ao layout do cabeçalho para ligar o controle do carrinho à rota `/cart` e mostrar unidades reais; evitar GET em cada renderização e impedir resposta inicial antiga de sobrescrever mutação já confirmada.
5. Habilitar inclusão em `ProductDetailPage` com quantidade positiva, request único pendente, feedback e atualização do estado a partir do POST. Sem sessão, mostrar orientação/link de login e não chamar a API.
6. Implementar `/cart` e componentes de linha, controles e estado vazio/erro/carregamento. Sincronizar POST/PUT pela resposta completa e só aplicar remoção/limpeza após `204`; produto indisponível é identificável e removível, sem controles de atualização.
7. Implementar confirmação acessível de limpeza; cancelar não chama a API nem altera o estado. Oferecer navegação ao catálogo e ao detalhe quando houver informação suportada.
8. Cobrir contratos, stores, estados/componentes, autenticação, contador, concorrência de respostas e navegação com testes existentes; validar build e cenários desktop/tablet/mobile.

## Riscos e Controles

- **Resposta de GET atrasada**: um GET iniciado antes de uma mutação pode terminar depois e sobrescrever estado mais recente. O store deve descartar resposta inicial obsoleta quando uma mutação confirmada ou troca de sessão ocorrer.
- **Sessão volátil**: reload descarta sessão e carrinho em memória; ao retornar, mostrar estado de login na rota e não enviar consulta sem token.
- **DELETE sem body**: os métodos de remoção não desserializam resposta; alteram estado somente após HTTP 204.
- **Produto indisponível**: não acessar propriedades de `product` nulo; manter identificação pelo UUID e permitir remover.
- **Sem total financeiro**: preço é informativo por produto; não mostrar subtotal/total nem frete.
- **Chamadas repetidas**: desabilitar a ação específica durante request e deduplicar carga inicial. Não criar mecanismo de concorrência que replique a regra atômica de persistência do backend.

## Rastreamento de Complexidade

Nenhuma violação constitucional, dependência externa ou novo serviço identificado. A atualização em memória após `204` é necessária porque o contrato não fornece corpo nas operações DELETE.