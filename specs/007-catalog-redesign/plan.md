# Plano de Implementação: Redesign Visual do Catálogo

**Branch**: `007-catalog-redesign` | **Data**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Entrada**: [Especificação clarificada](spec.md)

## Resumo

Evoluir a experiência visual do catálogo existente para uma interface de e-commerce premium,
minimalista e tecnológica, preservando a identidade off-white/verde-limão e todo o comportamento
funcional da feature `006-frontend-product-catalog`.

A implementação será uma refatoração de apresentação: ampliar o `StoreLayout` para header e
navegação, reorganizar a composição da `CatalogPage`, atualizar a hierarquia de `ProductCard` e
centralizar tokens, estados e breakpoints em `frontend/src/styles.css`. `catalogStore`, `catalogApi`,
rotas, contratos e Product Service permanecerão inalterados, exceto por ajustes de apresentação
necessários para renderizar os dados já disponíveis.

## Contexto Técnico

**Linguagem/Versão**: TypeScript 5.7.2, React 18.3.1 e CSS existente.

**Dependências Primárias**: React Router 6.28.1, Zustand 5.0.2, Axios 1.7.9, Vitest 2.1.5,
React Testing Library 16.1.0 e `lucide-react` já disponível no `frontend/package.json`.

**Armazenamento**: N/A. O redesign não cria persistência nem altera o estado autoritativo do
catálogo.

**Testes**: Vitest, React Testing Library, user-event e jest-dom para regressão comportamental;
validação manual/visual em desktop, tablet e mobile para layout e acessibilidade.

**Plataforma Alvo**: Navegadores desktop, tablet e mobile, com largura mínima de 320 px.

**Tipo de Projeto**: Aplicação web single-page React/TypeScript existente.

**Metas de Desempenho**: Manter as mesmas requisições e parâmetros do catálogo atual; não adicionar
chamadas de dados para o redesign; manter transições curtas e não bloquear interação ou leitura.

**Restrições**: Não alterar backend, Gateway, endpoints, contratos, regras de negócio, busca,
filtros, ordenação, paginação, autenticação ou carrinho. Não inventar avaliações, descontos,
preços anteriores ou contagem de itens.

**Escala/Âmbito**: Header compartilhado do catálogo, hero introdutório, filtros, toolbar, grid,
card, estados de carregamento/erro/vazio e breakpoints desktop/tablet/mobile.

## Verificação da Constituição

*Gate: deve passar antes da pesquisa e ser reavaliado após o design.*

| Princípio | Estado | Evidência |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Nenhum serviço, endpoint ou banco é criado ou alterado. |
| II. Arquitetura Hexagonal | PASS | Backend permanece intocado; o frontend preserva o cliente e store existentes. |
| III. Clean Code/SOLID | PASS | Layout, páginas, cards, estado e API continuam separados. |
| IV. Test-first | PASS | Testes de regressão cobrem rotas, dados e estados antes/depois do redesign. |
| V. API e contratos | PASS | Nenhum contrato é alterado; apenas dados já existentes são apresentados. |
| VI. Segurança por padrão | PASS | Nenhuma credencial, token ou dado administrativo é introduzido na UI. |
| VII. Observabilidade | N/A | Nenhum serviço ou telemetria nova faz parte da feature. |
| VIII. Arquitetura frontend | PASS | Componentes existentes são reutilizados e lógica funcional não é duplicada. |
| IX. Reprodutibilidade | PASS | Sem dependências novas; usa scripts e tokens atuais do frontend. |
| X. Spec-driven | PASS | Spec, clarificação e plano precedem tasks e implementação. |

**Resultado antes da pesquisa**: PASS.

**Resultado após o design**: PASS. O plano restringe mudanças à apresentação e não cria exceções
arquiteturais.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/007-catalog-redesign/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── catalog-ui-invariants.md
└── tasks.md             # criado por /speckit.tasks
```

### Código-fonte

```text
frontend/
└── src/
    ├── App.tsx                           # header, navegação e affordances da loja
    ├── components/
    │   ├── ProductCard.tsx                # hierarquia visual e estados do card
    │   └── __tests__/ProductCard.test.tsx
    ├── pages/
    │   ├── CatalogPage.tsx                # hero, filtros, toolbar, grid e estados
    │   └── __tests__/CatalogPage.test.tsx
    ├── pages/ProductDetailPage.tsx        # manter coerência visual da navegação
    └── styles.css                          # tokens, composição, breakpoints e motion
```

**Decisão de estrutura**: manter a lógica de dados em `catalogStore` e `apiClient`; concentrar o
redesign em composição JSX sem lógica de negócio e CSS. O header será compartilhado pelo layout
existente. O card continuará sendo um único link para detalhes, evitando ações de compra novas.

## Sequência de Implementação

1. Mapear os estados e conteúdo atuais para preservar os contratos de renderização.
2. Atualizar `StoreLayout` com navegação, conta e carrinho futuro/inativo, mantendo acessibilidade.
3. Reorganizar `CatalogPage` em breadcrumb, hero/pesquisa, contexto de categorias, filtros,
   quantidade, ordenação, grid e estados existentes.
4. Atualizar `ProductCard` para nova hierarquia, proporções estáveis, estados de foco/hover/touch
   e apresentação condicional apenas dos dados disponíveis.
5. Refatorar `styles.css` com tokens preservados, layouts desktop/tablet/mobile, `prefers-reduced-motion`,
   foco e transições sem deslocamento estrutural.
6. Regressar testes funcionais e validar visualmente as viewports e os estados descritos no
   quickstart.

## Rastreamento de Complexidade

Nenhuma violação constitucional ou dependência nova foi identificada. Ratings, desconto e preço
anterior não serão modelados sem dados existentes; a alternativa de inventar esses valores foi
rejeitada por risco de inconsistência e violação da autoridade do Product Service.
