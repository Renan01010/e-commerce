# Pesquisa: Catálogo de Produtos no Frontend

## Decisões

### 1. Reutilizar os contratos públicos existentes

**Decisão**: o frontend consumirá `GET /api/products`, `GET /api/categories` e
`GET /api/products/{id}` através do cliente HTTP apontado para o API Gateway.

**Racional**: o Product Service é a autoridade já definida para produtos, categorias, preços,
quantidade e estado ativo. Criar endpoints ou um contrato paralelo duplicaria a fundação de
`001-foundation-product-catalog` e aumentaria o risco de divergência.

**Alternativas consideradas**: acessar o Product Service diretamente, acessar PostgreSQL ou
criar um BFF específico. Foram rejeitadas porque violam o Gateway/serviço definido na spec e a
separação de responsabilidades do projeto.

### 2. Manter o estado de consulta no Zustand existente

**Decisão**: usar `catalogStore` para consulta, filtros, ordenação, paginação, produto
selecionado, carregamento e erro; usar `catalogApi` para a comunicação HTTP.

**Racional**: essas abstrações já existem, são exercitadas por testes e mantêm a apresentação
separada da integração. A mudança deve consolidar comportamento, não introduzir outro mecanismo
de estado.

**Alternativas consideradas**: estado local duplicado por página, contexto React ou biblioteca
adicional. Foram rejeitadas por espalhar o estado da consulta e divergir do padrão atual.

### 3. Derivar disponibilidade da quantidade retornada

**Decisão**: `quantity > 0` será exibido como disponível e `quantity === 0` como esgotado;
produto inativo ou inexistente será tratado como não encontrado no detalhe.

**Racional**: corresponde ao modelo `Product`, ao filtro `inStock` e aos critérios de aceitação
existentes. O frontend não reserva, recalcula nem garante estoque.

**Alternativas consideradas**: criar um serviço de inventário, inferir disponibilidade pela
existência da imagem ou permitir compra de produto indisponível. Todas estão fora do escopo.

### 4. Preservar estados visuais e autenticação existente

**Decisão**: manter o layout atual do catálogo e o catálogo acessível sem login; quando existir
sessão, o interceptor compartilhado poderá anexar o token sem tornar as consultas dependentes
dele. O botão de carrinho continua desabilitado.

**Racional**: `005-frontend-login` define autenticação separada, enquanto a API de leitura do
catálogo é pública. A mudança visual deve seguir o frontend já implementado, sem reabrir o escopo
do login ou do carrinho.

**Alternativas consideradas**: exigir login para navegar, redesenhar a identidade visual ou
ativar o carrinho nesta feature. Todas contradizem as specs anteriores ou `006`.

## Conclusão

Não restam decisões técnicas bloqueantes. O plano pode avançar para decomposição em tarefas;
detalhes de componentes e casos de teste devem seguir os arquivos existentes e os contratos
referenciados.
