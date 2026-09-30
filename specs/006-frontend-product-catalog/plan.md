# Plano de Implementação: Catálogo de Produtos no Frontend

**Branch**: `006-frontend-product-catalog` | **Data**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Entrada**: [Especificação clarificada](spec.md)

## Resumo

Entregar e consolidar a experiência pública de catálogo no frontend existente, permitindo
listar, pesquisar, filtrar, ordenar e consultar detalhes de produtos através do API Gateway.
O Product Service continua sendo a autoridade para dados, preço, categoria e quantidade; não
serão criados endpoints, persistência ou regras de carrinho nesta feature.

O trabalho será concentrado nas superfícies já existentes: `CatalogPage`, `ProductCard`,
`ProductDetailPage`, `catalogStore` e `catalogApi`. O plano prioriza completar os cenários e
estados da spec, preservar os padrões visuais atuais e ampliar os testes de comportamento,
acessibilidade básica, contrato consumido e responsividade.

## Contexto Técnico

**Linguagem/Versão**: TypeScript 5.7.2, React 18.3.1.

**Dependências Primárias**: Vite 5.4.11, React Router 6.28.1, Axios 1.7.9,
Zustand 5.0.2, Vitest 2.1.5 e React Testing Library 16.1.0.

**Armazenamento**: N/A no frontend; produtos e categorias permanecem no Product Service e
PostgreSQL existente.

**Testes**: Vitest, React Testing Library, user-event e jest-dom; testes existentes de páginas,
componentes, store e cliente HTTP serão estendidos.

**Plataforma Alvo**: Navegadores desktop e mobile, a partir de 320 px; SPA servida pelo Vite
em desenvolvimento e pelo container web em entrega.

**Tipo de Projeto**: Aplicação web single-page React/TypeScript existente.

**Metas de Desempenho**: Não há SLA novo na spec; respeitar o timeout HTTP atual de 10 segundos,
mostrar carregamento durante requisições e evitar requisições redundantes durante uma interação.

**Restrições**: Consultas públicas somente pelo Gateway; reutilizar contratos e cliente HTTP
existentes; não expor `cost`; não persistir dados de catálogo como fonte autoritativa; não
alterar autenticação, Gateway ou Product Service.

**Escala/Âmbito**: Uma rota de catálogo, uma rota de detalhes, filtros suportados pelo contrato,
paginação padrão de 20 itens e as telas/componentes já existentes; sem novas áreas administrativas.

## Verificação da Constituição

*Gate: deve passar antes da pesquisa e ser reavaliado após o design.*

| Princípio | Estado | Evidência |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Nenhum serviço ou banco novo; o Product Service continua dono do catálogo. |
| II. Arquitetura Hexagonal | PASS | Nenhuma alteração backend; o frontend usa o adaptador HTTP existente. |
| III. Clean Code/SOLID | PASS | Apresentação, estado e comunicação continuam separados nas abstrações existentes. |
| IV. Test-first | PASS | O plano amplia testes unitários e de componentes para jornadas e estados da spec. |
| V. API e contratos | PASS | O plano consome os três endpoints públicos existentes sem alterá-los. |
| VI. Segurança por padrão | PASS | Catálogo público não recebe dados administrativos nem custo interno. |
| VII. Observabilidade | N/A | Não há serviço backend ou telemetria nova nesta feature. |
| VIII. Arquitetura frontend | PASS | Páginas, componentes, store e cliente HTTP permanecem separados e reutilizáveis. |
| IX. Reprodutibilidade | PASS | Usa toolchain e comandos já existentes no frontend. |
| X. Spec-driven | PASS | Spec clarificada precede este plano; tasks serão geradas em seguida. |

**Resultado antes da pesquisa**: PASS. Não há violação que exija complexidade adicional.

**Resultado após o design**: PASS. Os artefatos de design não criam contrato backend, camada de
persistência ou dependência externa nova.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/006-frontend-product-catalog/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── catalog-gateway-consumption.md
└── tasks.md             # criado por /speckit.tasks
```

### Código-fonte (raiz do repositório)

```text
backend/
└── ...                         # serviços existentes, sem alteração nesta feature

frontend/
├── package.json
└── src/
    ├── App.tsx
    ├── components/
    │   ├── ProductCard.tsx
    │   └── __tests__/ProductCard.test.tsx
    ├── pages/
    │   ├── CatalogPage.tsx
    │   ├── ProductDetailPage.tsx
    │   └── __tests__/
    │       ├── CatalogPage.test.tsx
    │       └── ProductDetailPage.test.tsx
    ├── services/
    │   ├── apiClient.ts
    │   └── __tests__/apiClient.test.ts
    ├── store/
    │   └── catalogStore.ts
    └── types/catalog.ts
```

**Decisão de estrutura**: manter o frontend atual e tratar o catálogo como uma fatia vertical
de páginas, componentes, estado e cliente HTTP. O `catalogApi` traduz o estado de consulta para
os parâmetros aprovados; `catalogStore` coordena carregamento, paginação e erro; as páginas
renderizam estados e navegação; `ProductCard` permanece responsável pela apresentação do resumo.
O backend não será modificado.

## Sequência de Implementação

1. Confirmar tipos e mapeamento dos três endpoints públicos, incluindo paginação, categorias,
   disponibilidade e ausência de `cost`.
2. Consolidar o estado da consulta no `catalogStore`, garantindo reinício de página ao alterar
   busca, filtro ou ordenação e descarte seguro de estado anterior durante carregamento/erro.
3. Consolidar `CatalogPage` e `ProductCard` para pesquisa, filtros, ordenação, paginação,
   estados vazio/erro/carregamento e acessibilidade.
4. Consolidar `ProductDetailPage` e as rotas para carregamento, produto inexistente, contexto
   de categoria e retorno ao catálogo, mantendo o carrinho desabilitado.
5. Validar responsividade, navegação por teclado e mensagens sem alterar login, cadastro,
   autenticação ou roteamento do Gateway.

## Rastreamento de Complexidade

Nenhuma violação constitucional ou novo componente arquitetural foi identificado. Não há
alternativa rejeitada a registrar.
