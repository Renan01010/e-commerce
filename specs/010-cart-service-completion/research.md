# Research: Cart Service Completion

## Decisões

### 1. V2 aditiva e estado explícito para preço legado

**Decisão:** manter `V1__create_cart_items.sql` intocada. V2 adiciona `unit_price NUMERIC(12,2)` sem preço default e um estado de snapshot (`PENDING`, `KNOWN`, `UNKNOWN`). `KNOWN` exige preço não nulo; `UNKNOWN` e `PENDING` não podem carregar preço.

**Rationale:** preço nulo isolado não diferencia backfill ainda não executado de produto que foi consultado e estava ausente/inativo. O estado impede que uma reinicialização posterior use automaticamente preço de produto que voltou a ficar ativo, em desacordo com a spec. `NUMERIC(12,2)` coincide com o tipo de preço do catálogo.

**Alternatives considered:** alterar V1 foi rejeitado pela spec; `DEFAULT 0` ou valor sentinela foi rejeitado porque inventaria preço; nullable sem estado foi rejeitado por permitir fallback indevido em reinicializações.

### 2. Backfill de preço fora do SQL Flyway

**Decisão:** depois de Flyway aplicar V2, um caso de uso do Cart Service consulta Product Service pelas portas existentes e resolve linhas PENDING. Produto ativo fica KNOWN pelo preço atual; resposta 404/inativo fica UNKNOWN sem preço e sem remoção. Timeout/5xx não é evidência de produto ausente: mantém PENDING e impede readiness até retry/restart bem-sucedido. Updates são condicionais a PENDING e idempotentes.

**Rationale:** migrations SQL não devem depender de rede nem conhecer outro serviço. O adapter atual já trata 404 como ausência e a resposta atual do Product Service inclui preço, status ativo e quantidade. A initialização deve concluir antes de o Cart servir leituras que prometem total financeiro.

**Alternatives considered:** consultar banco do Product Service viola ownership; chamar serviço dentro de migration Flyway mistura persistência e rede; resolver toda linha apenas na primeira leitura posterga migração e não distingue um backfill interrompido.

### 3. Estoque e concorrência

**Decisão:** estender o `ProductCatalogPort` com `quantity` já presente no payload recebido pelo `ProductResponse`. Validar quantidade resultante para add e quantidade absoluta para set. A persistência usa condição atômica no upsert/update para não ultrapassar o máximo nem o estoque observado em corridas locais. Não há reserva ou decremento de estoque.

**Rationale:** mantém Product Service como fonte e não modifica seu contrato. A leitura atual do carrinho já faz fetch de catálogo; add/update podem reaproveitar o resumo do produto da operação.

**Alternatives considered:** validar somente antes do SQL foi rejeitado porque duas adições concorrentes podem ultrapassar o limite; reservar estoque foi excluído pela spec.

### 4. Modelo monetário e respostas

**Decisão:** o snapshot persistido é a fonte financeira. Para preço conhecido, subtotal = snapshot × quantidade; total = soma. Para qualquer UNKNOWN, a linha comunica preço/subtotal indisponíveis e o total é null com sinalizador explícito. Preço desconhecido não é zero. O resumo de produto não pode substituir snapshot pelo preço atual do catálogo.

**Alternatives considered:** usar preço live na leitura viola estabilidade; total parcial omite linhas e seria enganoso; remover automaticamente linhas desconhecidas viola a spec.

### 5. Erros de novas regras

**Decisão:** manter status atuais de sucesso, autenticação, ausência e indisponibilidade. Teto e estoque insuficiente serão conflitos de estado (HTTP 409), documentados em OpenAPI com mensagem legível. Nenhuma rota nova.

**Rationale:** as requisições são sintaticamente válidas, mas incompatíveis com o estado atual do carrinho ou estoque; 409 diferencia a rejeição de JSON/formato inválido (400).

### 6. Consumidor frontend

**Decisão:** adaptar o consumidor atual de `/api/cart`, pois a spec requer exibir preço estável, subtotal, total e indisponibilidade. Atualizar type, store e cart UI; não alterar URL/método das chamadas.

**Rationale:** hoje a página não guarda o total e mostra `product.price` live; só alterar o DTO não entregaria o cenário ao cliente.

### 7. Docker e Railway

**Decisão:** nenhum Dockerfile deve mudar. Testar que a imagem atual empacota V2. Na Railway, disponibilizar Product Service antes do Cart, evitar escrita por versão antiga durante migration/backfill e só liberar tráfego após readiness.

**Rationale:** o Dockerfile existente copia `backend/` e gera o JAR do Cart com recursos; deployment escalonado sem controle permite versão antiga criar/atualizar linhas sem snapshots.

## Limites da pesquisa

A pesquisa foi feita nos arquivos locais atuais. Não foi realizado acesso a Railway, PostgreSQL de produção ou execução de testes/deploy. A validação final de runtime fica para o quickstart de implantação.
