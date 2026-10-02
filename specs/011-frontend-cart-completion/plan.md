# Implementation Plan: Conclusão do Carrinho no Frontend

**Branch**: `011-frontend-cart-completion` | **Date**: 2026-10-02 | **Spec**: [spec.md](spec.md)

**Input**: [Especificação clarificada](spec.md)

## Summary

Completar a apresentação e a sincronização do carrinho na aplicação frontend existente, exibindo preço unitário, subtotal por linha e total somente a partir dos valores confirmados pelo Cart Service. Reutilizar o cliente Axios, a store Zustand, as páginas/componentes e a suíte Vitest existentes; não adicionar dependências nem alterar serviços, gateway, autenticação, contrato versionado ou persistência.

POST/PUT substituirão o estado pelo `CartResponse` retornado. DELETE confirma a mutação sem corpo: após `204`, a UI aplica a remoção confirmada e faz exatamente um GET para obter o resumo financeiro atualizado. Se esse GET falhar, a mutação continua confirmada, os itens/contador não são restaurados e o total fica indisponível até uma nova consulta explícita. Limpeza segue a mesma regra. Nenhum total será somado no frontend.

## Technical Context

**Language/Version**: TypeScript 5.7.2 e React 18.3.1.

**Primary Dependencies**: Vite 5.4.11, React Router 6.28.1, Axios 1.7.9, Zustand 5.0.2, Vitest 2.1.5, React Testing Library 16.1.0 e `@testing-library/user-event` 14.5.2; sem dependências novas.

**Storage**: Nenhuma persistência nova no frontend. Sessão e estado do carrinho continuam em memória; armazenamento autoritativo permanece nos serviços existentes.

**Testing**: Vitest e React Testing Library nos testes de cliente HTTP, store, componentes, páginas e layout; `npm run build` verifica TypeScript e build Vite.

**Target Platform**: Navegadores desktop, tablet e mobile, preservando o tema e os breakpoints existentes.

**Project Type**: Single-page application React/TypeScript já existente.

**Performance Goals**: Não há SLA definido. Preservar o timeout HTTP atual de 10 segundos, deduplicar o carregamento inicial e não consultar por renderização. Uma consulta após cada DELETE é intencional e definida pela clarificação para obter resumo financeiro oficial.

**Constraints**: Consumir somente a API de carrinho existente através do `apiClient`; usar JWT/interceptor atuais; não enviar `userId`; não inferir preço, subtotal, total, estoque, disponibilidade ou limite. DTOs/controllers atuais são a referência aprovada; OpenAPI versionado permanece inalterado e sua divergência está documentada.

**Scale/Scope**: Uma página de carrinho, uma integração existente com cinco operações, inclusão existente no detalhe do produto, estado compartilhado e badge de unidades; alteração apenas no frontend e nos testes frontend existentes.

## Constitution Check

*Gate: verificado antes da pesquisa e reavaliado após o design.*

| Princípio | Estado | Evidência |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Nenhum serviço, endpoint ou armazenamento novo; o Cart Service mantém propriedade das regras e dos dados. |
| II. Arquitetura Hexagonal | PASS | Nenhuma alteração backend; chamadas seguem encapsuladas na camada HTTP frontend existente. |
| III. Clean Code/SOLID | PASS | Reutiliza responsabilidades atuais de API, store, componentes e páginas; sem abstração ou dependência nova. |
| IV. Test-first quality | PASS | O plano amplia testes existentes para sucesso, falha, concorrência, respostas `204` e estados visuais. |
| V. API e contratos | DIVERGÊNCIA PREEXISTENTE REGISTRADA; NÃO BLOQUEIA ESTA FEATURE FRONTEND | Por decisão clarificada, esta feature consome os DTOs/controllers atuais. O OpenAPI está desatualizado e permanece inalterado; sua atualização é uma ação documental futura, fora deste escopo e sem bloquear a implementação frontend. |
| VI. Segurança por padrão | PASS | JWT e propriedade continuam no backend; sessão/carrinho não são persistidos no navegador nem enviados com `userId`. |
| VII. Observabilidade | N/A | Feature exclusivamente frontend; nenhuma telemetria ou serviço novo está no escopo. |
| VIII. Arquitetura frontend | PASS | Integração, estado compartilhado, apresentação e navegação permanecem separados; nenhuma regra financeira/de estoque é duplicada. |
| IX. Reprodutibilidade | PASS | Usa scripts, configuração e dependências já existentes no frontend. |
| X. Spec-driven development | PASS | Especificação e clarificações precedem este plano; tasks serão geradas na etapa seguinte. |

**Resultado antes da pesquisa**: PASS para o escopo frontend, com a divergência documental preexistente registrada como não bloqueante por decisão do responsável pela feature. Nenhuma alteração de backend ou OpenAPI será feita aqui.

**Resultado após o design**: PASS. O design não introduz dependências, persistência, APIs, regras de negócio ou alterações em serviços.

## Design da Implementação

### Estado e fontes de verdade

- Reutilizar `useCartStore` como estado compartilhado para página, detalhe e header; preservar isolamento por sessão e proteção contra respostas antigas.
- Aplicar integralmente respostas POST/PUT, incluindo `items`, `maxItemQuantity`, `total` e `totalAvailable`.
- Considerar `maxItemQuantity` oficial somente após um GET bem-sucedido na sessão atual. Enquanto o store estiver `idle`/`loading`, bloquear a inclusão no detalhe e comunicar que o carrinho está sendo carregado; em `error`, manter a inclusão bloqueada e oferecer retry de GET. Nunca usar o valor inicial `99` do frontend para habilitar inclusão. Após a carga, usar o limite retornado para orientar/validar a quantidade; o Cart Service permanece validador definitivo.
- Remover a soma local de subtotais usada como fallback. A UI apresenta valores somente quando a resposta os declara disponíveis; valores ausentes/desconhecidos nunca são formatados como zero.
- Para DELETE de item e limpeza, após `204`, atualizar localmente apenas a mudança confirmada, invalidar o resumo anterior e executar um GET. Sucesso substitui o estado pelo carrinho retornado; falha mantém a mutação confirmada, marca total como indisponível e expõe retry que faz GET sem repetir DELETE.
- Serializar mutações no nível do carrinho como já faz o store, desabilitar controles pendentes e preservar a última quantidade confirmada quando POST/PUT falhar.
- Derivar o badge pela soma de quantidades das linhas, inclusive indisponíveis; não contar UUIDs como informação visível.
- Testar o badge integrado após POST, PUT (aumentar e diminuir), DELETE de item e limpeza. O badge deriva apenas das quantidades da resposta/estado confirmado; não participa de cálculos financeiros.

### Erros e apresentação

- Estender o mapeamento do `cartApi` para mensagens de usuário: `401`, `404`, `409`, `400`, `403`/`422` quando recebidos, `5xx` e rede. Preservar detalhes acionáveis do serviço, sobretudo conflito de estoque/limite, sem apresentar status HTTP como mensagem.
- Em `401`, reutilizar `clearSession` e limpar estado associado à sessão.
- Distinguir falha da mutação de falha do GET posterior a DELETE para que não haja mensagem de remoção malsucedida quando ela já foi confirmada.
- Ajustar item e resumo para rotular preço unitário, apresentar subtotal recebido, preço/total indisponível e estado de produto indisponível. A UI não mostra `productId`; SKU só é exibido se já existir nos dados usados.
- Preservar os estados existentes de loading inicial, erro, vazio, pendência e confirmação acessível; adaptar estilos existentes em `styles.css`, sem novo sistema visual ou checkout.

### Arquivos previstos

```text
frontend/src/
├── components/cart/CartItem.tsx
├── components/cart/__tests__/CartItem.test.tsx
├── pages/CartPage.tsx
├── pages/ProductDetailPage.tsx
├── pages/__tests__/CartPage.test.tsx
├── pages/__tests__/ProductDetailPage.test.tsx
├── __tests__/App.test.tsx
├── services/apiClient.ts
├── services/__tests__/apiClient.test.ts
├── store/cartStore.ts
├── store/__tests__/cartStore.test.ts
└── styles.css
```

`types/cart.ts` já declara os campos observados nos DTOs, e `App.tsx` já deriva o badge por unidades; não são alvos de alteração prevista salvo incompatibilidade encontrada ao testar a integração. O teste existente `frontend/src/__tests__/App.test.tsx` será ampliado para cobrir o badge após cada mutação. Não serão criados arquivos de aplicação ou teste novos.

## Project Structure

### Documentação desta feature

```text
specs/011-frontend-cart-completion/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── cart-gateway-consumption.md
├── checklists/
│   └── requirements.md
└── tasks.md             # será criado por /speckit.tasks
```

### Código-fonte existente

```text
frontend/src/
├── App.tsx
├── components/cart/
├── pages/
├── services/apiClient.ts
├── store/cartStore.ts
├── types/cart.ts
└── styles.css
```

**Structure Decision**: Manter a SPA e suas camadas atuais. As mudanças ficam nos componentes/páginas de carrinho, store, API client, estilos e testes correspondentes. A documentação de contrato desta feature descreve consumo e divergências; não substitui nem edita o OpenAPI/backend.

## Complexity Tracking

Nenhuma violação constitucional nova, dependência externa, serviço ou abstração de domínio é proposta. A consulta GET após DELETE é necessária porque a API responde `204` sem o resumo financeiro; uma falha de consulta não reverte a mutação confirmada.
