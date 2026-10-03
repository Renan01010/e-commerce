# Pesquisa e Decisões: Home e Catálogo de Produtos

**Data**: 2026-10-03 | **Branch da Feature**: `012-home-product-catalog`

## Contexto Verificado

- O frontend usa React 18.3.1, TypeScript 5.7.2, React Router 6.28.1, Axios 1.7.9, Zustand 5.0.2 e Vitest/Testing Library; `lucide-react` já está instalado. Não há necessidade de nova dependência.
- `frontend/src/App.tsx` atualmente monta `CatalogPage` em `/`; `/products/:id`, `/cart`, `/login` e `/register` já estão implementados. `StoreLayout` possui a marca, navegação, menu mobile, link de conta, badge e carregamento do carrinho para sessão válida.
- `CatalogPage` mantém busca com debounce de 250 ms, filtros/ordenação/página no `catalogStore`, categorias, hero e grade. Usa React Router `useSearchParams` para ler `categoryId`, porém seus links de categorias atualmente apontam para `/?categoryId=...`; mudar a rota inicial requer apontá-los para `/catalog?categoryId=...`.
- `catalogStore.loadProducts` lê sempre os filtros, ordenação, página e tamanho da lista compartilhados (tamanho padrão 20) e grava o resultado em `products`. Reutilizar essa ação para a Home sobrescreveria a listagem ativa ou permitiria uma resposta Home contaminar estado de catálogo; por isso a coleção de recentes precisa de estado e ação isolados no mesmo store.
- O store já incrementa IDs de requisição para ignorar respostas obsoletas; categorias têm `categoriesError`, mas não estado carregando, deduplicação/cache ou retry explícito na interface. A Home e o catálogo devem compartilhar categorias em vez de emitir GETs iguais durante a navegação.
- `catalogApi.getProducts` aceita e envia `query`, filtros suportados, `sortBy`, `sortOrder`, `page` e `pageSize`; valida que `content` seja lista. O Product Service suporta `sortBy=newest`, `sortOrder=desc` e page size até 100.
- A implementação atual ordena `newest` pelo timestamp `created_at` decrescente com desempate de `id ASC`; a Home pode descrever “mais recentes” somente nesse sentido técnico.
- `GET /api/categories` retorna categorias ativas sem parâmetros de pesquisa; `CategoryController` não aceita `search`. O DTO backend retorna `slug`, mas o tipo `Category` do frontend não declara esse campo. A Home precisa apenas de campos já tipados e não fará busca remota de categorias.
- `ProductResponse` inclui preço, marca, SKU, categoria, quantidade e imagem; também inclui `cost`, mas apenas admins recebem esse campo e o frontend não deve apresentá-lo. Não há campos de imagem de categoria, avaliação, preço anterior, desconto ou popularidade.
- `ProductCard` hoje é somente link para detalhe, não contém botão de carrinho. Como todo o card atual está dentro de um `<Link>`, adicionar o botão no seu interior produziria controles interativos aninhados; o card deve ser reestruturado como artigo com link de detalhe e botão de carrinho irmãos. `ProductDetailPage` valida sessão, estado carregado do carrinho, limites e chamadas via `cartStore.addItem`; o store rejeita adições se não carregado e deduplica usando chave `add:<productId>`.
- `cartStore.addItem` reflete no estado a resposta retornada e mantém erro/sucesso. O cabeçalho soma quantidades das linhas confirmadas no estado do store; `hasConfirmedCart` exige sessão válida e `status === 'loaded'`.
- `getApiErrorMessage` do API client usa mensagem do corpo se disponível e mensagem genérica para outros erros; não diferencia `429`. A implementação deverá normalizar a mensagem pública sem exibir detalhes internos arbitrários, especialmente no cenário de Gateway.
- Os estilos globais em `frontend/src/styles.css` usam tokens e superfícies claras; media queries existentes tratam menu mobile, grid, filtros e outros estados. Para preservar páginas existentes, a direção escura será aplicada à Home e às superfícies compartilhadas estritamente necessárias em vez de reestilizar autenticação.
- Testes relevantes existentes: `App.test.tsx`, `CatalogPage.test.tsx`, `ProductCard.test.tsx`, `catalogStore.test.ts`, `cartStore.test.ts` e `apiClient.test.ts`. Todos usam mocks existentes e comandos `npm test` / `npm run build`.
- A especificação registrou que `specs/009-category-management/contracts/category-management-api.md` menciona `search` em `GET /api/categories`, embora a implementação não o aceite; o contrato efetivamente implementado e `catalogApi` são a referência.
- A referência desktop fornecida foi aberta e revisada. O arquivo presente é `specs/012-home-product-catalog/references/TechStore_ Tecnologia sem limites.png`; o nome `references/home-desktop-reference.png` mencionado na instrução não existe atualmente. A imagem demonstra header em duas faixas, hero panorâmico com artwork tecnológico, seção de benefícios, faixa de cards de categoria, grade de produtos e três banners secundários, com fundo navy/dark, acentos ciano/azul/roxo e verde-lima.

## Decisões

### 1. Produtos da Home: até oito produtos mais recentes

**Decisão**: Consultar `catalogApi.getProducts` com `query: ''`, filtros vazios, `sortBy: 'newest'`, `sortOrder: 'desc'`, `page: 0` e `pageSize: 8`. A seção “Produtos em destaque” usa somente os produtos retornados; o rótulo não afirma popularidade.

**Racional**: É uma escolha de seleção explícita do produto, suportada pelo parâmetro e pela ordenação observados no endpoint real; evita inventar ranking, badge ou atributo de destaque.

**Alternativa considerada**: usar os primeiros produtos da ordenação default/relevância. Rejeitada porque sem termo o critério é menos explícito e não satisfaz diretamente a necessidade acordada de selecionar os mais recentes.

### 2. Manter listagem Home e catálogo isoladas

**Decisão**: Guardar produtos recentes, loading, erro e request version separados de `products`/paginação do catálogo, mas no `catalogStore` existente. Não chamar `loadProducts` no estado compartilhado com filtros padrão e page size oito.

**Racional**: A tela inicial e o catálogo têm consultas diferentes e podem produzir respostas em corrida ao navegar. Estado separado protege filtros, paginação e resultado ativo.

### 3. Rotas explícitas e consulta navegável na URL

**Decisão**: `/` passa a ser Home e `/catalog` passa a exibir a listagem atual. A URL é a fonte de verdade para `query` e `categoryId`: parâmetros ausentes limpam os filtros correspondentes no store, mudanças desses controles atualizam a URL e navegação voltar/avançar restaura o estado. Home/cabeçalho navega para `/catalog?query=<termo>` e categorias para `/catalog?categoryId=<id>`. Outros filtros, ordenação e página ficam no estado existente e não são serializados nesta feature. Manter `/products/:id`, `/cart`, `/login` e `/register`.

**Racional**: preserva o comportamento de catálogo, torna destinos de busca e categoria compartilháveis, limpa critérios obsoletos ao abrir o catálogo sem parâmetros e mantém o histórico do navegador coerente.

### 4. Reutilizar e compartilhar categorias sem duplicar chamadas

**Decisão**: carregar categorias via `catalogApi.getCategories` e reutilizar a lista tipada existente, ordenando por `displayOrder` na apresentação. Deduplicar requests simultâneas e manter em memória da SPA a última lista carregada com sucesso para reutilização entre Home e catálogo. Não armazenar falha como cache; ação explícita de retry deve emitir nova leitura. Manter estado de carregamento/erro separado da consulta de produtos.

**Racional**: categorias têm a mesma origem e contrato em ambas as páginas. IDs e nomes devem vir da API; `slug` não é necessário; o parâmetro `search` não é suportado pela implementação atual.

**Alternativa considerada**: refazer o GET a cada montagem de página. Rejeitada por duplicar leituras equivalentes ao navegar entre Home e catálogo sem ação de mutação ou invalidação de categorias nesta feature.

### 5. Conteúdo editorial e imagens

**Decisão**: manter títulos, textos do Hero, benefícios informativos e conteúdo de banners em configuração frontend local. Usar decoração CSS/ícones existentes e fallback genérico de categoria; CTA de banners aponta para `/catalog`, sem ID fictício, preço anterior ou desconto.

**Racional**: atende direção comercial/visual sem CMS, asset obrigatório de terceiros ou afirmação de disponibilidade promocional inexistente.

**Referência visual obrigatória**: antes da implementação da Home, comparar a composição desktop com `references/TechStore_ Tecnologia sem limites.png`, usando a hierarquia visual, layout, espaçamento, paleta e linguagem como inspiração. A imagem não é fonte de conteúdo: não copiar textos promocionais, percentuais, produtos, imagens de produto, marcas, avaliações, categorias, disponibilidade ou preços. A referência deve orientar a estrutura do header/Hero/categorias/cards/banners sem introduzir dependência visual externa.

### 6. Adição ao carrinho no card com autoridade existente

**Decisão**: reutilizar `cartStore.addItem({ productId, quantity: 1 })`; mostrar link de login a visitantes sem sessão válida. Desabilitar adição até carrinho estar carregado, durante operação daquela chave e quando o produto tiver quantidade zero. A resposta do store mantém feedback e badge sincronizados.

**Racional**: o `cartStore` exige estado carregado, valida limite e deduplica mutações da mesma linha; chamar `cartApi` diretamente ou duplicar validação em componentes quebraria a arquitetura.

### 7. Erros, acessibilidade e estilo

**Decisão**: preservar mensagens seguras de catálogo com tratamento explícito de indisponibilidade/`429`, skeletons e retry; aplicar visual escuro na Home e ajustar superfícies comuns necessárias sem reestilizar login.

**Racional**: status do Product Service/Gateway não devem expor dados técnicos, e a direção da Home não justifica mudanças globais fora do caminho da feature.

### 8. Testes sem serviços externos

**Decisão**: extender testes Vitest/RTL existentes, mockando `catalogApi`, `cartApi`, auth/cart/catalog stores; executar `npm test` e `npm run build` em `frontend/`.

**Racional**: os mocks e ferramentas estão disponíveis e validam chamadas/estados sem dependência de Gateway real.

## Estratégia de Testes

- **App/rotas**: `/` renderiza Home, `/catalog` lista, links Home/categorias/produtos/conta/carrinho navegam aos destinos corretos; badge segue resposta confirmada e rotas de auth permanecem.
- **Home**: request recente usa ordenação/página/tamanho aprovados; categorias reais ordenadas e resposta bem-sucedida reutilizada entre rotas; skeletons, vazio, erro/retry independentes; categoria e pesquisa preservam seus parâmetros na URL; sem dados promocionais ou IDs fictícios.
- **ProductCard**: preço/nome/imagem/disponibilidade consumidos, UUID não visível, fallback de imagem, detalhe correto, acesso ao login para visitante, adição bloqueada durante loading e feedback de sucesso/erro.
- **CatalogPage/store**: consulta independente não altera listagem paginada; `query`/`categoryId` são sincronizados nos dois sentidos com a URL, critérios ausentes limpam valores anteriores e histórico voltar/avançar restaura os valores. Busca, filtros reais, ordenação, paginação e proteção contra resposta obsoleta funcionam; outros filtros e controles não precisam ser serializados na URL.
- **Carrinho/API**: ação do card chama `addItem` uma vez, mock confirma estado do carrinho e atualiza badge; falha não emite sucesso nem altera os valores confirmados.
- **Acessibilidade/responsividade**: nomes acessíveis, navegação por teclado, status/alert e layout testado em larguras 1280 px, 768 px e 375 px via inspeção de CSS/browser quando aplicável.
- **Gates**: `npm test`; `npm run build`; revisão de mudanças para confirmar que somente frontend e documentação da feature foram alterados.

## Pendências

Nenhuma decisão funcional bloqueante permanece. A divergência de documentação de `search` em categorias e a ausência de campos editoriais na API estão registradas; esta feature não modifica o contrato backend.
