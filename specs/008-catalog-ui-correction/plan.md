# Plano de Implementação: Correção Responsiva da UI do Catálogo

**Branch**: `008-catalog-ui-correction` | **Data**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Entrada**: [Especificação clarificada](spec.md)

## Resumo

Corrigir a composição responsiva da tela de catálogo existente, reduzindo a hero, aproximando a
busca e os produtos, criando um container fluido, oferecendo navegação mobile adequada e
transformando filtros em uma superfície apropriada para toque. A implementação preservará os
resultados e as chamadas da feature `006-frontend-product-catalog`.

O trabalho ficará concentrado em `App.tsx`, `CatalogPage.tsx`, `ProductCard.tsx` e `styles.css`,
com testes de regressão em páginas/componentes. `catalogStore`, `apiClient`, tipos, backend,
Gateway e contratos funcionais não serão alterados.

## Contexto Técnico

**Linguagem/Versão**: TypeScript 5.7.2, React 18.3.1 e CSS existente.

**Dependências Primárias**: React Router 6.28.1, Zustand 5.0.2, Axios 1.7.9, Vitest 2.1.5,
React Testing Library 16.1.0 e `lucide-react` já disponível.

**Armazenamento**: N/A. Estado transitório de abertura dos filtros pode permanecer na página;
nenhum dado de catálogo será persistido ou duplicado.

**Testes**: Vitest, React Testing Library, user-event e jest-dom; inspeção visual manual ou
automatizada com navegador nas oito larguras definidas na spec.

**Plataforma Alvo**: Navegadores desktop, tablet e mobile de 320 px a 1920 px ou mais.

**Tipo de Projeto**: Aplicação web single-page React/TypeScript existente.

**Metas de Desempenho**: Nenhuma chamada adicional de catálogo; primeira composição deve manter
hero e busca compactas; transições não podem bloquear interação ou leitura.

**Restrições**: Não alterar Product Service, API Gateway, banco, contratos, autenticação, store,
cliente HTTP, busca, filtros, ordenação, paginação ou regras de negócio. Não usar larguras
estruturais fixas incompatíveis com viewport.

**Escala/Âmbito**: Header, hero, busca, categorias, superfície de filtros, toolbar, grid, cards,
estados e breakpoints da rota de catálogo; detalhe deve manter coerência visual sem reimplementar
sua lógica.

## Verificação da Constituição

*Gate: deve passar antes da pesquisa e ser reavaliado após o design.*

| Princípio | Estado | Evidência |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Nenhum serviço, endpoint ou banco é criado ou alterado. |
| II. Arquitetura Hexagonal | PASS | Backend intocado; cliente e store funcionais permanecem os mesmos. |
| III. Clean Code/SOLID | PASS | UI, estado funcional e comunicação continuam separados. |
| IV. Test-first | PASS | Testes protegem comportamento e estados enquanto a apresentação muda. |
| V. API e contratos | PASS | Nenhum contrato é modificado; respostas existentes são apenas renderizadas. |
| VI. Segurança por padrão | PASS | Nenhum dado sensível ou fluxo protegido é introduzido. |
| VII. Observabilidade | N/A | Não há serviço ou telemetria nova nesta feature. |
| VIII. Arquitetura frontend | PASS | Componentes e tokens existentes são reutilizados; estado novo é somente visual. |
| IX. Reprodutibilidade | PASS | Sem dependências novas; usa scripts atuais e validação documentada. |
| X. Spec-driven | PASS | Spec e clarificação precedem plano, tarefas e implementação. |

**Resultado antes da pesquisa**: PASS.

**Resultado após o design**: PASS. O plano mantém a fronteira funcional e não cria exceções.

## Estratégia de Breakpoints

| Faixa | Comportamento planejado |
|---|---|
| `320-374px` | Header mínimo, menu acessível, busca quase integral, filtros em drawer, uma coluna quando necessário. |
| `375-767px` | Header compacto, categorias roláveis, filtros em drawer, uma ou duas colunas conforme espaço real. |
| `768-1023px` | Header adaptado, filtros recolhíveis/expansíveis e grid de tablet sem sidebar rígida. |
| `1024-1279px` | Layout intermediário com container contido, filtros compactos e grid adaptável. |
| `1280-1919px` | Header desktop, sidebar proporcional e grid de múltiplas colunas. |
| `1920px+` | Mesmo container máximo centralizado, sem espalhar conteúdo até as bordas. |

Os breakpoints são guias de comportamento, não devem ser usados para introduzir larguras fixas
que causem overflow.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/008-catalog-ui-correction/
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
    ├── App.tsx                           # header desktop/mobile e menu
    ├── components/
    │   ├── ProductCard.tsx                # mídia e proporções do card
    │   └── __tests__/ProductCard.test.tsx
    ├── pages/
    │   ├── CatalogPage.tsx                # hero, filtros, drawer e estados
    │   └── __tests__/CatalogPage.test.tsx
    ├── pages/ProductDetailPage.tsx        # coerência visual sem nova lógica
    └── styles.css                          # container, breakpoints, grid e motion
```

**Decisão de estrutura**: conservar `catalogStore`, `catalogApi` e tipos intocados. `CatalogPage`
pode possuir somente estado transitório para abertura/fechamento/aplicação visual dos filtros;
seus valores continuam sendo lidos e escritos pelas ações existentes. O menu mobile deve ser
controlado pela composição do layout, sem criar uma nova camada de navegação funcional.

## Sequência de Implementação

1. Registrar baseline de rotas, chamadas, filtros, ordenação, paginação, cards e estados.
2. Corrigir container e hero para sequência contínua e compacta.
3. Criar header mobile com menu, mantendo logo, conta e carrinho acessíveis.
4. Reorganizar filtros para sidebar desktop, superfície recolhível tablet e drawer mobile.
5. Ajustar grid/cards para imagens proporcionais, colunas adaptáveis e textos longos.
6. Reestilizar estados loading, erro, vazio e sem resultados sem alterar mensagens/ações.
7. Aplicar foco, contraste, redução de movimento e validar as oito larguras.
8. Executar testes, build e matriz visual do quickstart.

## Rastreamento de Complexidade

Nenhuma violação constitucional ou dependência nova foi identificada. Um drawer de filtros é
preferível a uma sidebar comprimida no mobile porque preserva espaço para os produtos sem copiar
ou alterar as regras de filtro.
