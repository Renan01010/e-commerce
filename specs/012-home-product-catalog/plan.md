# Plano de Implementação: Home e Catálogo de Produtos

**Branch**: `012-home-product-catalog` | **Data**: 2026-10-03 | **Spec**: [spec.md](spec.md)

**Entrada**: Especificação clarificada para a Home e o catálogo do TechStore.

## Resumo

Transformar `/` na Home comercial TechStore, com visual escuro responsivo, categorias reais, conteúdo editorial frontend e até oito produtos mais recentes do Product Service. Mover a listagem completa existente para `/catalog`, preservando busca, filtros, ordenação, paginação e detalhes. Reutilizar autenticação, API client e Zustand existentes para adição ao carrinho, mantendo o Cart Service como fonte de verdade. Nenhum serviço, rota ou contrato backend será alterado.

A implementação será concentrada em `App.tsx`, `StoreLayout`, `HomePage`, `CatalogPage`, `ProductCard`, `catalogStore`, `apiClient.ts` e `styles.css`. A consulta da seção inicial terá estado independente do resultado paginado do catálogo. A busca e o filtro de categoria viajarão pela URL ao navegar entre Home e catálogo; as operações de carrinho continuarão centralizadas no store existente.

## Contexto Técnico

**Linguagem/Versão**: TypeScript 5.7.2 e React 18.3.1.

**Dependências Primárias**: Vite 5.4.11, React Router 6.28.1, Axios 1.7.9,
Zustand 5.0.2 e `lucide-react` 1.48+ já instalados.

**Armazenamento**: Nenhuma persistência nova no frontend. Products, categories e estado autoritativo do carrinho permanecem nos respectivos serviços existentes.

**Testes**: Vitest, React Testing Library, `user-event` e `jest-dom`; atualizar testes de App, catálogo, ProductCard e stores e acrescentar testes da Home.

**Plataforma Alvo**: SPA React existente, navegadores desktop/tablet/mobile, viewport mínimo de 320 px.

**Tipo de Projeto**: Aplicação web single-page, frontend exclusivamente.

**Metas de Desempenho**: Preservar timeout HTTP atual de 10 segundos; uma consulta da página inicial de produtos (`pageSize=8`, `newest`, `desc`) e uma lista de categorias reutilizada após carga bem-sucedida entre Home e catálogo; deduplicar requests de categorias concorrentes, não repetir uma leitura bem-sucedida durante a sessão SPA e permitir retry explícito após erro; imagens dos cards abaixo da dobra com lazy loading; skeletons durante leitura inicial.

**Restrições**:

- Consumir somente `GET /api/products`, `GET /api/categories`, `GET /api/products/{id}` e operações de carrinho já consumidas pelo API client.
- Usar os campos, valores de ordenação e DTOs verificados; não apresentar `cost`, preços anteriores, avaliações, descontos ou badges não existentes.
- Preservar autenticação e store de carrinho existentes; não calcular nem estimar preços, quantidades, disponibilidade ou limites.
- Não adicionar dependências, assets remotos obrigatórios, endpoints ou serviços de infraestrutura.
- Não alterar backend, Gateway, contratos, banco, migrations ou autenticação.

**Escala/Âmbito**: Home `/`, catálogo `/catalog`, detalhes `/products/:id`, cabeçalho compartilhado e rotas já existentes de login/carrinho; uma consulta de oito produtos recentes, categorias ativas e a consulta paginada atual.

## Verificação da Constituição

*Gate: verificar antes da pesquisa e reavaliar depois das decisões de design.*

| Princípio | Estado | Evidência |
|---|---|---|
| I. Microsserviços orientados ao domínio | PASS | Product e Cart Service continuam donos dos respectivos dados; nenhum serviço novo. |
| II. Arquitetura Hexagonal | PASS | Nenhuma mudança backend; acesso frontend continua pelo API Gateway e `apiClient`. |
| III. Clean Code e SOLID | PASS | Estado e dados em stores/API client; componentes apresentam jornadas e estados. |
| IV. Test-first | PASS | Testes de navegação, consultas, UI responsiva, erros, carrinho e acessibilidade serão ampliados. |
| V. API e contratos | PASS | Reutiliza as rotas e campos efetivamente existentes; divergências documentadas não serão convertidas em chamadas novas. |
| VI. Segurança por padrão | PASS | Sessão e autorização permanecem no fluxo atual; sem credenciais/client-side authority. |
| VII. Observabilidade | N/A | A feature não cria serviços nem telemetria backend. |
| VIII. Arquitetura frontend | PASS | Reutiliza React, React Router, Zustand, API client e componentes compartilhados. |
| IX. Reprodutibilidade | PASS | Usa scripts e dependências já instalados em `frontend/`. |
| X. Spec-driven | PASS | A especificação foi criada e clarificada antes deste plano; as tarefas serão derivadas em `/speckit.tasks`. |

**Resultado antes da pesquisa**: PASS.
**Resultado após as decisões de design**: PASS. A consulta de produtos recentes usa parâmetros já suportados e os dados auxiliares são estado transitório, sem persistência ou nova autoridade.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/012-home-product-catalog/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── catalog-home-gateway-consumption.md
└── tasks.md             # criado por /speckit.tasks
```

### Código-fonte (raiz do repositório)

```text
frontend/src/
├── App.tsx
├── components/
│   ├── ProductCard.tsx
│   └── __tests__/ProductCard.test.tsx
├── pages/
│   ├── HomePage.tsx                  # nova
│   ├── CatalogPage.tsx
│   └── __tests__/
│       ├── HomePage.test.tsx         # novo
│       └── CatalogPage.test.tsx
├── services/
│   └── apiClient.ts
├── store/
│   ├── catalogStore.ts
│   ├── cartStore.ts                  # reutilizado; mudança só se necessária para estado de ação
│   └── __tests__/
│       └── catalogStore.test.ts
├── styles.css
└── __tests__/
    └── App.test.tsx
```

**Decisão de estrutura**: preservar o frontend e os stores existentes. Adicionar uma página Home separada, tornar `/catalog` a rota explícita da listagem atual e manter `/products/:id`, `/cart`, `/login` e `/register`. `StoreLayout` continua dono do cabeçalho e da sincronização do carrinho. `HomePage` compõe componentes de apresentação com o `catalogStore`; `ProductCard` reaproveitável oferece detalhe e a ação de carrinho sem duplicar regra de domínio.

## Design e Sequência de Implementação

**Gate visual obrigatório**: antes de implementar T013–T016 ou T038–T041 em `tasks.md`, consultar `specs/012-home-product-catalog/references/TechStore_ Tecnologia sem limites.png`. A referência orienta composição e linguagem visual; não copiar nomes ou dados comerciais, preços, produtos, avaliações, badges ou percentuais exibidos. O caminho `references/home-desktop-reference.png` inicialmente informado não existe; esse é o arquivo efetivamente presente na feature.

1. **Rotas e navegação**
   - Adicionar `/catalog` para a atual `CatalogPage`; fazer `/` renderizar `HomePage`.
   - Atualizar o link “Início” para `/`, “Produtos”/“Ver catálogo” para `/catalog` e o acesso a categorias para uma âncora de categorias válida.
   - Atualizar breadcrumbs e ações “voltar ao catálogo” em cards/detalhes para `/catalog`, sem alterar a rota de detalhe.
   - Manter rotas existentes e indicar o destino de fallback como `/catalog`.
   - Tratar `query` e `categoryId` da URL como fonte de verdade: hidratar e sincronizar ambos com `catalogStore`, limpar do store um critério cujo parâmetro esteja ausente e responder à navegação voltar/avançar. A busca da Home/cabeçalho navega para `/catalog?query=...` e a categoria real para `/catalog?categoryId=...`. Outros filtros, ordenação e paginação continuam no estado existente do catálogo e não precisam ser serializados na URL nesta feature.

2. **Consultas de catálogo sem colisão de estado**
   - Estender `catalogStore` com coleção, status, erro e retry dedicados a produtos da Home; carregar a primeira página com `sortBy: 'newest'`, `sortOrder: 'desc'`, `page: 0`, `pageSize: 8`, sem alterar produtos, filtros, ordenação, paginação, erro ou loading da listagem completa.
   - Reutilizar `catalogApi.getProducts` e `catalogApi.getCategories`; não introduzir endpoint, tipo de produto ou API de categoria alternativa.
   - Manter proteção contra respostas obsoletas e deduplicar requisição idêntica enquanto pendente. Cachear em memória da SPA a lista de categorias após sucesso, para reutilizá-la entre Home e catálogo; falhas não são cacheadas e retry explícito emite nova leitura. Manter erro e loading separados dos produtos.
   - Ordenar categorias reais pelo `displayOrder` na apresentação; nenhuma categoria ou UUID será hardcoded como dado.

3. **Home e conteúdo editorial**
   - Consultar o gate visual e criar `HomePage` seguindo a referência desktop: header/navegação em duas faixas; Hero panorâmico com tipografia de destaque, arte tecnológica, CTA e benefícios; faixa de categorias em cards horizontais; grade de produtos e banners secundários. Adaptar a composição responsivamente.
   - Manter Hero editorial configurável no frontend, benefícios somente como texto informativo aprovado na spec, categorias reais, grid dos até oito produtos mais recentes e banners sem preços/descontos fictícios.
   - Não reproduzir conteúdo comercial da referência: percentuais, textos de oferta, preço, produto, marca, categorias que não vierem da API, imagens de produto, avaliações ou badges “Mais vendido”, “Oferta” ou “Novo”.
   - Usar artwork CSS/gradientes e ícones já instalados como representação visual; não depender de imagem externa ou inventar imagem para categoria.
   - Direcionar CTAs de banner para `/catalog`; não fixar IDs nem nomes de categoria no código. Exibir skeleton, estado vazio e retry independente para categorias e produtos.

4. **Cards e adição ao carrinho**
   - Ampliar `ProductCard` com imagem responsiva/lazy, nome, marca quando fornecida, preço da API e disponibilidade de `quantity`; remover UUID da UI. Manter link de detalhe e botão de adicionar como controles irmãos dentro do card, sem botão aninhado em link nem link aninhado em botão.
   - Mostrar ação para login em visitante não autenticado e adicionar via `cartStore.addItem` para sessão válida. Desabilitar enquanto carrinho não estiver `loaded`, produto estiver indisponível ou `pendingOperations['add:<productId>']` estiver ativo; não emitir POST duplicado.
   - Anunciar feedback de sucesso/erro usando a resposta/mensagens do store, sem mostrar sucesso otimista. O cabeçalho continuará usando as linhas do store confirmado para atualizar o badge.
   - Preservar o comportamento atual da página de detalhe e o API client. Se mudanças forem necessárias ao store de carrinho, limitar às lacunas provadas pelos testes e manter seu contrato financeiro atual.

5. **Catálogo, visual e acessibilidade**
   - Corrigir links de categoria e caminhos de retorno do catálogo após a mudança de rota; manter busca, filtro, ordenação, paginação e seleção apoiados no contrato atual.
   - Adicionar busca ao cabeçalho compartilhado e preservar a busca submetida entre Home e `/catalog`; usar o debounce existente onde aplicável.
   - Aplicar direção escura e acentos TechStore à Home e aos elementos compartilhados necessários (especialmente cabeçalho e ProductCard), sem reescrever páginas de login/autenticação nem introduzir regras visuais incompatíveis com suas folhas específicas.
   - Definir comportamento tablet/mobile no `styles.css`: header/menu operável, categorias roláveis em eixo horizontal quando necessário, grid adaptável, Hero e alvos de toque; manter foco visível, labels, landmarks e mensagens com `role="status"`/`role="alert"` apropriados.

6. **Mensagens e testes**
   - Tratar categorias e produtos com estado independente. Mapear falhas de rede e HTTP do catálogo a mensagens seguras/amigáveis, incluindo `429` e fallback para demais `4xx`/`5xx`, sem apresentar corpo técnico arbitrário.
   - Atualizar testes de `App` para rotas, navegação e badge; criar testes de Home para parâmetros de consulta, renderização real, retry, vazio, falha, filtro de categoria, links, login e adição; atualizar testes de `ProductCard`, `CatalogPage` e stores para estado isolado e URL.
   - Usar mocks de `catalogApi`, `cartApi` e Zustand; não depender de Gateway/serviços externos na suíte.

## Rastreamento de Complexidade

Nenhuma violação constitucional, nova dependência, persistência, serviço ou abstração arquitetural foi identificada. O novo estado de produtos da Home é necessário para impedir que a chamada “recentes” substitua a consulta filtrada/paginada do catálogo, e reutiliza o store/cliente existentes.
