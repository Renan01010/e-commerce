# Pesquisa e Decisões: Conclusão do Carrinho no Frontend

**Data**: 2026-10-02 | **Branch da Feature**: `011-frontend-cart-completion`

## Contexto Verificado

- O frontend já usa React 18.3.1, TypeScript 5.7.2, React Router 6.28.1, Axios 1.7.9 e Zustand 5.0.2; Vitest e React Testing Library já estão disponíveis no `frontend/package.json`.
- `frontend/src/services/apiClient.ts` contém Axios compartilhado, base URL `VITE_API_URL` (padrão `/api`), timeout atual de 10 segundos, interceptor JWT e métodos atuais `cartApi`.
- `frontend/src/types/cart.ts` já declara `items`, `maxItemQuantity`, `total`, `totalAvailable`, `quantity`, `available`, `product`, `unitPriceSnapshot`, `priceAvailable` e `subtotal`.
- `frontend/src/store/cartStore.ts` deduplica GET inicial, sincroniza POST/PUT a partir das respostas, serializa mutações, separa estado por sessão e já consulta novamente após DELETE de linha. Porém, calcula localmente total como fallback para respostas sem corpo; limpeza não busca o resumo oficial após o DELETE.
- No fluxo atual de remoção, a atualização local aguarda tanto o `204` quanto o GET posterior. Se o GET falhar após o DELETE ter sido confirmado, o item removido pode permanecer visível. O design separa esses dois resultados e cobre essa falha em teste.
- `frontend/src/pages/CartPage.tsx` já tem skeleton, erro/retry inicial, estado vazio, confirmação de limpeza, resumo de quantidade de produtos/unidades e estado de total indisponível. A página atualmente não mostra subtotal agregado separado; o DTO fornece apenas `total` agregado.
- `frontend/src/components/cart/CartItem.tsx` mostra unidade e subtotal, mas não rotula claramente o preço por unidade e exibe `productId` na linha disponível e na indisponível. A condição de preço precisa também proteger subtotal nulo antes de formatá-lo.
- `frontend/src/App.tsx` já conta unidades totais no badge, inclusive linhas indisponíveis. `ProductDetailPage` já usa `addItem` e o store para adicionar produtos.
- Testes existentes relevantes: `CartItem.test.tsx`, `CartPage.test.tsx`, `cartStore.test.ts`, `apiClient.test.ts`, `ProductDetailPage.test.tsx` e `App.test.tsx`. Não há necessidade confirmada de instalar pacote ou criar suíte separada.
- Os DTOs/controllers atuais do Cart Service retornam resumo e snapshot financeiros. O OpenAPI versionado em `specs/004-cart/contracts/cart-service-api.openapi.yaml` não descreve esses campos nem os conflitos `409`; a clarificação aprovada define DTOs/controllers como referência e proíbe alteração do OpenAPI nesta feature.

## Decisões

### 1. Manter o contrato de consumo existente, usando DTOs/controllers atuais

**Decisão**: consumir as cinco rotas já existentes por `cartApi`; documentar que o OpenAPI está desatualizado, sem modificá-lo.

**Racional**: o usuário confirmou que o serviço está em produção com essas capacidades e escolheu explicitamente DTOs/controllers atuais como fonte do contrato desta feature. O frontend não cria endpoint nem altera API.

**Alternativas consideradas**: alterar o OpenAPI ou mudar o serviço. Rejeitadas por estarem fora do escopo frontend-only.

### 2. Tratar todo valor monetário como dado autoritativo

**Decisão**: renderizar `unitPriceSnapshot`, `subtotal`, `total` e disponibilidade exclusivamente a partir de resposta confirmada. Remover `totalForItems` e qualquer fallback que some subtotais. `product.price` é preço atual do resumo de produto, não substituto do snapshot da linha.

**Racional**: o Cart Service preserva snapshot e pode ter preço desconhecido; somar no cliente pode divergir do estado financeiro retornado.

**Alternativas consideradas**: recalcular total a partir de subtotais, preencher desconhecido com zero ou usar preço atual do catálogo. Todas rejeitadas por inventarem um valor financeiro.

### 3. Separar confirmação DELETE de atualização do resumo

**Decisão**: após `204`, aplicar a remoção local confirmada e realizar um GET para obter o resumo oficial; aplicar a resposta inteira se bem-sucedido. Se GET falhar, preservar a remoção, deixar total indisponível e oferecer retry exclusivamente do GET.

**Racional**: DELETE não inclui `CartResponse`. A clarificação requer consulta após remoção e limpeza; falha na leitura não pode desfazer a mutação já confirmada nem repetir DELETE.

**Alternativas consideradas**: esperar pelo GET antes de refletir o DELETE (pode mostrar linha removida) ou calcular novo total em memória (contraria fonte de verdade). Rejeitadas.

### 4. Reutilizar Zustand e serialização existente

**Decisão**: manter `cartStore` como fonte de estado compartilhada em memória, preservar deduplicação por sessão, `requestVersion` e bloqueio de mutações concorrentes. Introduzir somente o estado/ação necessário para erro e retry do resumo posterior ao DELETE.

**Racional**: cabeçalho, detalhe e página já compartilham store e autenticação; não há benefício em novo state manager.

### 5. Usar limite retornado para controles, sem validar estoque no frontend

**Decisão**: usar `maxItemQuantity` da resposta para entrada/controle e desabilitar incremento no limite. Não inferir estoque nem consolidar quantidades localmente. Manter conflito do backend como autoridade e refletir sua mensagem acionável.

**Racional**: o limite é informação do serviço; estoque e soma da inclusão pertencem ao domínio do Cart Service.

### 6. Normalizar erros na fronteira HTTP

**Decisão**: ampliar o tipo/mapeamento `CartApiError` para respostas de validação (`400`), autenticação/autorização (`401`/`403`), não encontrado (`404`), conflito (`409`), validação adicional (`422`), `5xx` e rede. Para status não produzidos pelo Cart Service atual, manter fallback amigável. Preservar detalhes seguros e úteis do `ErrorResponse`.

**Racional**: componentes não devem conhecer exceções Axios ou apresentar `HTTP nnn`; erros de catálogo continuam separados.

### 7. Apresentar identificação comercial, não técnica

**Decisão**: remover UUID da linha; mostrar SKU apenas se já existir em dados usados pela UI. Produto indisponível permanece removível e preço/subtotal conhecidos do snapshot continuam independentes da disponibilidade.

**Racional**: identificadores internos não ajudam o cliente e não existe SKU no payload do carrinho atual.

### 8. Reaproveitar testes e estilos existentes

**Decisão**: atualizar testes atuais de HTTP, store, linha, página, detalhe e App; editar regras responsivas existentes em `frontend/src/styles.css`. Nenhum framework de teste, componente global de modal ou arquivo de aplicação novo.

## Estratégia de Testes

- **apiClient**: os cinco paths/payloads, sucesso POST `200/201`, PUT `200`, DELETE `204` sem corpo, erros reconhecidos, preservação de detalhe útil e falha de rede.
- **cartStore**: respostas POST/PUT substituem estado e resumo; não soma subtotais localmente; GET inicial desduplicado; serialização; sessão trocada; DELETE confirmado com GET bem-sucedido; GET pós-DELETE falho sem restaurar linha ou repetir mutação; retry faz somente GET.
- **CartItem**: preço unitário explicitamente rotulado, subtotal, preço/subtotal nulos sem moeda zero, estado disponível/indisponível, remoção e ausência de UUID visível.
- **CartPage**: vazio, um/vários produtos, contagem de produtos/unidades, total da API, total indisponível, limpeza cancelada/confirmada, GET posterior e falha/retry.
- **ProductDetailPage/App**: adição/consolidação refletida pela resposta, pendência e badge contado em unidades após operações.
- **Gates**: `npm test` e `npm run build` dentro de `frontend/`; inspeção manual mobile/tablet/desktop conforme guia de validação.

## Pendências

Nenhuma clarificação funcional bloqueante permanece. A divergência do OpenAPI é dívida documental conhecida e não será alterada nem usada como fonte do payload nesta feature.