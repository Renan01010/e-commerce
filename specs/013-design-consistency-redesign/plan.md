# Implementation Plan: Consistência Visual e Redesign das Telas Existentes

**Branch**: `013-design-consistency-redesign` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

**Input**: Especificação clarificada da Feature 013.

## Summary

Aplicar às rotas e superfícies existentes do TechStore a identidade dark/tech da Home da Feature 012, incluindo o redesign visual integral do Catálogo, Detalhe do Produto e Carrinho. Preservar `StoreLayout` como shell compartilhado das rotas da loja e manter Login e estado indisponível de cadastro standalone, sem o header da loja. O trabalho ficará na camada de apresentação e nos testes frontend: consolidar os valores dark já usados pela Home em tokens CSS reutilizáveis, adaptar os estilos existentes do catálogo, detalhe, carrinho, componentes de produto e autenticação, e validar os fluxos e estados sem modificar stores, clientes, APIs ou contratos.

## Technical Context

**Language/Version**: TypeScript 5.7.2, React 18.3.1 e CSS.

**Primary Dependencies**: Vite 5.4.11, React Router 6.28.1, Zustand 5.0.2, Axios 1.7.9, `lucide-react` 1.48+, Vitest 2.1.5 e React Testing Library 16.1.0, todos já instalados.

**Storage**: N/A para mudanças. Sessão, catálogo e carrinho continuam nos stores em memória e nos serviços existentes.

**Testing**: Vitest em jsdom, React Testing Library, `user-event` e `jest-dom`; `npm test` e `npm run build` a partir de `frontend/`. Testes visuais/funcionais dos estados permanecem mockados, sem serviços externos.

**Target Platform**: SPA React em navegadores desktop, tablet e mobile; validar larguras de 1280 px, 1024 px, 768 px e 375 px, sem overflow horizontal.

**Project Type**: Aplicação web frontend React/TypeScript.

**Performance Goals**: Nenhuma chamada, dependência ou estado de negócio adicional. Preservar comportamento de carregamento e renderização; evitar alterações que causem requests, renders ou carregamentos de mídia adicionais.

**Constraints**:

- Alterar somente apresentação e testes frontend; manter `apiClient`, stores, autenticação, rotas, DTOs e fluxos atuais.
- Catálogo mantém busca, filtros, ordenação, paginação, resultados e estados atuais.
- Carrinho mantém valores e decisões confirmados pelo Cart Service; não calcular totais, disponibilidade ou quantidades na UI.
- Login e cadastro indisponível seguem standalone; não adicionar header, fluxo de cadastro ou campos.
- Reutilizar CSS e componentes existentes; não adicionar dependências nem uma paleta concorrente.
- Usar imagem existente quando disponível e fallback de marca sem inventar mídia ou atributos.

**Scale/Scope**: Shell/header em `App.tsx`; rotas de Home, Catálogo, Detalhe, Login, cadastro indisponível e Carrinho; `ProductCard`, `CartItem`, confirmação de limpeza e componentes de autenticação; folhas `styles.css` e `LoginPage.css`; testes frontend diretamente relacionados.

## Constitution Check

| Principle | Status | Evidence |
|---|---|---|
| I. Domain-Driven Microservices | PASS | Product e Cart Services continuam proprietários dos dados; nenhum serviço é criado ou alterado. |
| II. Hexagonal Architecture | PASS | Nenhuma alteração backend; os clientes continuam acessando serviços existentes via Gateway. |
| III. Clean Code and SOLID | PASS | Reutilizar páginas, shell, controles e componentes atuais; nenhuma regra de negócio ou abstração desnecessária. |
| IV. Test-First Quality | PASS | Atualizar testes de navegação, catálogo, detalhe, autenticação e carrinho; exigir suíte e build bem-sucedidos. |
| V. API and Contract Discipline | PASS | Nenhum endpoint, DTO ou contrato é alterado; validar interações com os clientes e contratos já consumidos. |
| VI. Security by Default | PASS | Sessão e autorização permanecem no fluxo existente; sem credenciais, persistência de token ou confiança em cálculos frontend. |
| VII. Observability | N/A | Nenhum serviço ou telemetria é criado. |
| VIII. Frontend Architecture | PASS | Separação existente entre páginas, componentes, Zustand e clientes permanece intacta; o escopo adapta apresentação. |
| IX. Infrastructure and Reproducibility | PASS | Usa scripts e dependências já definidos em `frontend/package.json`. |
| X. Spec-Driven Development | PASS | Spec clarificada precede o plano; tarefas serão derivadas em `/speckit.tasks`. |

**Gate antes da pesquisa**: PASS. Nenhuma exceção constitucional é necessária.
**Gate após o design**: PASS. A proposta é frontend-only, não introduz APIs, entidades, dependências ou lógica autoritativa.

## Project Structure

### Documentation (this feature)

```text
specs/013-design-consistency-redesign/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── frontend-ui-preservation.md
└── tasks.md                 # criado por /speckit.tasks
```

### Source Code (repository root)

```text
frontend/
├── src/
│   ├── App.tsx
│   ├── styles.css
│   ├── pages/
│   │   ├── CatalogPage.tsx
│   │   ├── ProductDetailPage.tsx
│   │   ├── CartPage.tsx
│   │   ├── LoginPage.tsx
│   │   ├── LoginPage.css
│   │   ├── RegisterUnavailablePage.tsx
│   │   └── __tests__/
│   ├── components/
│   │   ├── ProductCard.tsx
│   │   ├── cart/
│   │   ├── auth/
│   │   └── __tests__/
│   ├── store/                 # estado e regras existentes; sem mudança planejada
│   ├── services/              # API clients existentes; sem mudança planejada
│   └── __tests__/
└── package.json
```

**Structure Decision**: Aplicação web existente em `frontend/`; manter a estrutura React atual. A adaptação visual se concentra nas folhas de estilo e páginas/componentes existentes. Diretórios `store/` e `services/` são dependências de comportamento a preservar, não alvos planejados de implementação.

## Complexity Tracking

Nenhuma violação constitucional ou complexidade excepcional identificada.
