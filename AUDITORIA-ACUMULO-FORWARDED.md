# Auditoria do acúmulo de headers no API Gateway

## CAUSA

Os dados de produção indicam acúmulo de headers de forwarding por múltiplos hops, com possibilidade de loop de proxy.

A hipótese principal é:

```text
Railway Edge
  -> API Gateway
  -> Spring Cloud Gateway acrescenta headers de forwarding
  -> destino configurado retorna ao Gateway ou passa novamente pelo Railway Edge
  -> novos headers são acrescentados
  -> repetição
```

Nesta branch, o fallback de `PRODUCT_SERVICE_URL` é `http://localhost:8080`, enquanto o próprio Gateway escuta na porta 8080:

[application.yml](backend/api-gateway/src/main/resources/application.yml#L1-L13)

```yaml
server:
  port: ${PORT:${SERVER_PORT:8080}}

spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: ${PRODUCT_SERVICE_URL:http://localhost:8080}
```

Se `PRODUCT_SERVICE_URL` não estiver definida no Railway, esse fallback pode encaminhar o Gateway para si mesmo.

O mesmo problema pode ocorrer se a variável estiver configurada com o domínio público do próprio Gateway em vez do hostname privado do Product Service.

## EVIDÊNCIAS

Foram observados em produção:

```text
Forwarded:
valueCount=40
bytes=2355
```

Também foi observado crescimento progressivo:

```text
X-Forwarded-For:
470 -> 500 -> 560 -> 590 -> 650 -> 680 -> 710 -> 740 bytes

X-Forwarded-Port:
217 -> 222 -> 237 -> 252 -> 267 -> 282 -> 297 -> 312 -> 327 -> 342 -> 357 bytes
```

Posteriormente ocorreu:

```text
TooLongHttpHeaderException:
HTTP header is larger than 8192 bytes.
```

Esse padrão não é compatível com uma única passagem normal:

```text
Browser -> Railway Edge -> Gateway -> Product Service
```

Ele é compatível com uma cadeia crescente de proxies ou com o mesmo proxy sendo percorrido várias vezes.

## Código do Gateway

Não foi encontrado código customizado adicionando:

- `Forwarded`;
- `X-Forwarded-For`;
- `X-Forwarded-Host`;
- `X-Forwarded-Proto`;
- `X-Forwarded-Port`;
- `X-Real-Ip`.

O `CorrelationIdFilter` modifica somente `X-Correlation-ID` e utiliza `headers.set()`:

[CorrelationIdFilter.java](backend/api-gateway/src/main/java/com/techstore/gateway/filter/CorrelationIdFilter.java#L17-L27)

Não há evidência de que esse filtro cause o crescimento observado.

O `HeaderDiagnosticWebFilter` apenas mede os headers recebidos:

[HeaderDiagnosticWebFilter.java](backend/api-gateway/src/main/java/com/techstore/gateway/filter/HeaderDiagnosticWebFilter.java#L19-L29)

Ele não usa `add()`, `set()`, `remove()`, `request.mutate()` ou `exchange.mutate()` para alterar a requisição.

## ARQUIVO/CONFIGURAÇÃO RESPONSÁVEL

O principal ponto de risco no código é o fallback de destino da rota de produtos:

[application.yml](backend/api-gateway/src/main/resources/application.yml#L9-L13)

```yaml
- id: product-service
  uri: ${PRODUCT_SERVICE_URL:http://localhost:8080}
```

O Compose local sobrescreve esse fallback com um destino interno:

[docker-compose.yml](docker-compose.yml#L87-L92)

```yaml
PRODUCT_SERVICE_URL: http://product-service:8081
```

Portanto:

- o Compose local não deveria criar esse ciclo;
- o valor efetivo do Railway é decisivo;
- o repositório não contém o valor real configurado no Railway.

O logger de startup registra o URI carregado para cada rota:

[GatewayRouteStartupLogger.java](backend/api-gateway/src/main/java/com/techstore/gateway/routes/GatewayRouteStartupLogger.java#L53-L56)

Esse log deve ser usado para confirmar o destino efetivo em produção.

## FLUXO DOS HEADERS

```text
Browser
  |
  v
Railway Edge
  | Forwarded/X-Forwarded-*
  v
API Gateway
  | recebe os headers existentes
  | filtros internos podem preparar headers downstream
  v
PRODUCT_SERVICE_URL
  |
  +--> Product Service privado: fluxo correto
  |
  +--> Gateway público ou localhost:8080: ciclo
          |
          +--> Railway Edge
                  |
                  +--> Gateway novamente
```

Os `HttpHeadersFilters` do Spring Cloud Gateway podem criar headers `Forwarded` e `X-Forwarded-*` para requisições enviadas ao downstream. Configurações de append podem preservar e acrescentar valores existentes.

Isso explica o crescimento no request de saída, mas não explica sozinho os 40 valores na entrada. Para os valores voltarem ao Gateway, é necessário um novo hop, múltiplos proxies ou um ciclo.

## POR QUE `Forwarded` CHEGOU A 40 VALORES

### A) Já chegavam do Railway Edge

É possível, mas não comprovado.

O Edge pode receber headers existentes e acrescentar seus próprios valores. Quarenta valores, entretanto, sugerem uma cadeia anterior ou repetição de hops.

### B) Foram criados pelo Spring/Netty

Improvável como causa isolada.

O Reactor Netty não cria uma cadeia de 40 valores por conta própria. Também não há configuração explícita no repositório para um handler customizado de forwarding.

### C) Foram adicionados durante o forwarding do Gateway

Possível para a requisição de saída.

O Spring Cloud Gateway possui filtros internos que podem criar ou acrescentar headers de forwarding. Eles se tornam parte do problema de entrada somente se a requisição retornar ao Gateway.

### D) Resultaram de múltiplos proxies

Muito provável.

O `valueCount=40` é compatível com múltiplos proxies acrescentando valores ao longo do caminho.

### E) Combinação

A hipótese mais forte é uma combinação de:

```text
Railway Edge
+ filtros de forwarding do Gateway
+ múltiplos proxies
+ possível destino apontando para o próprio Gateway
```

## POR QUE `X-FORWARDED-FOR` ESTÁ CRESCENDO

`X-Forwarded-For` representa a cadeia de endereços dos proxies atravessados.

O crescimento observado indica que novos endereços ou valores estão sendo anexados progressivamente. Isso não seria esperado em uma única passagem pelo Edge e pelo Gateway.

O padrão é compatível com:

```text
client-ip, proxy-1, proxy-2, proxy-3, ...
```

ou com o mesmo proxy sendo atravessado repetidamente por um loop.

## POR QUE `X-FORWARDED-PORT` ESTÁ CRESCENDO

Uma cadeia normal teria poucos valores, por exemplo:

```text
443
```

ou:

```text
443, 8080
```

O crescimento progressivo de `X-Forwarded-Port` indica que cada hop está preservando ou acrescentando sua porta à cadeia existente.

Isso reforça a hipótese de múltiplas passagens por proxies ou de um loop de forwarding.

## CORREÇÃO MÍNIMA

A correção mínima provável é garantir que `PRODUCT_SERVICE_URL` no Railway aponte diretamente para o Product Service na rede privada:

```text
http://product-service.railway.internal:<porta-interna>
```

O valor não deve apontar para:

```text
http://localhost:8080
```

nem para o domínio público do Gateway.

Também deve ser confirmado que:

- o Product Service não aponta de volta para o Gateway;
- nenhum rewrite do Railway cria um retorno ao Gateway;
- o Gateway usa o hostname privado do Product Service;
- o Edge não está recebendo requisições originadas pelo próprio Gateway.

Não há justificativa, neste momento, para remover headers cegamente ou aumentar o limite do Netty.

## RISCOS DA CORREÇÃO

Alterar headers de forwarding sem identificar sua origem pode:

- remover IPs legítimos;
- quebrar detecção de HTTPS atrás do proxy;
- causar problemas de CORS ou geração de URLs;
- mascarar o loop sem eliminá-lo;
- permitir spoofing de IP se headers externos forem aceitos sem confiança;
- dificultar auditoria e observabilidade.

## O QUE DEVE SER ALTERADO NO RAILWAY

Posteriormente, verificar:

1. O valor efetivo de `PRODUCT_SERVICE_URL` no Gateway.
2. O valor efetivo de `CART_SERVICE_URL` e `USER_SERVICE_URL`.
3. Se `PRODUCT_SERVICE_URL` usa hostname privado.
4. Se o destino usa a porta interna correta do Product Service.
5. Se o domínio público do Gateway está sendo usado como destino interno.
6. Se existem redirects, rewrites ou proxies intermediários circulares.
7. Se o Product Service recebe tráfego originado do próprio Gateway.

A alteração provável no Railway é corrigir `PRODUCT_SERVICE_URL`, caso esteja apontando para o Gateway ou esteja ausente.

Nenhuma alteração no Railway deve ser feita antes dessa confirmação.

## O QUE DEVE SER ALTERADO NO CÓDIGO

Neste momento, nada deve ser alterado no código.

Não há evidência suficiente para alterar:

- `CorrelationIdFilter`;
- `HeaderDiagnosticWebFilter`;
- rotas;
- filtros de forwarding;
- limite do Reactor Netty;
- headers recebidos;
- Product Service;
- Cart Service;
- User Service;
- frontend.

## O QUE NÃO DEVE SER ALTERADO

Não alterar nesta etapa:

- limite de headers do Netty;
- `maxHeaderSize`;
- rotas do Gateway;
- `Forwarded` cegamente;
- `X-Forwarded-*` cegamente;
- filtros de correlation ID;
- Product Service;
- Cart Service;
- User Service;
- frontend;
- Docker;
- arquitetura;
- Railway sem confirmar a variável responsável.

## CONCLUSÃO

O acúmulo de headers está confirmado pelos dados de produção.

A origem exata ainda não é comprovável apenas pelo repositório, porque o valor efetivo de `PRODUCT_SERVICE_URL` no Railway não está versionado.

A classificação mais provável é:

```text
Múltiplos proxies
+ headers acrescentados durante o forwarding
+ possível loop causado por PRODUCT_SERVICE_URL
```

O ponto de maior risco no código é o fallback `http://localhost:8080`, pois essa é a própria porta do Gateway.

A confirmação definitiva exige comparar:

1. o URI da rota registrado no startup do Gateway;
2. o valor efetivo de `PRODUCT_SERVICE_URL` no Railway;
3. o destino real observado no Product Service;
4. a quantidade de hops antes da requisição chegar ao Gateway.

Nenhuma correção foi implementada, nenhum limite foi aumentado, nenhuma rota foi alterada e nenhum commit foi feito.
