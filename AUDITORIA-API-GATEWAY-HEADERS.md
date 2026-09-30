# Auditoria do API Gateway: HTTP Headers

## 1. Resumo executivo

O API Gateway recebe requisições que falham no decode do Reactor Netty com `TooLongHttpHeaderException` e limite de 8192 bytes.

A causa mais provável é que o bloco de headers já chega maior que o limite do servidor, possivelmente por headers adicionados pelo Railway Edge ou por um header grande como `Cookie` ou `Authorization`.

Não foi encontrada evidência de duplicação causada pelo código da aplicação. Os filtros usam `set()`, não `add()`, e a exceção ocorre antes da execução dos filtros Spring.

**Conclusão:** provável, mas não confirmado apenas pelo repositório.

## 2. Causa encontrada

Não foi encontrada uma linha do projeto que, sozinha, cause a exceção.

O Gateway utiliza Reactor Netty por meio do Spring Cloud Gateway, sem configuração explícita de limite de headers em `backend/api-gateway/src/main/resources/application.yml`.

Não foram encontrados:

- `maxHeaderSize`;
- `server.max-http-request-header-size`;
- `spring.cloud.gateway.httpclient.max-header-size`;
- configuração explícita de `reactor.netty`;
- `server.forward-headers-strategy`;
- customização de `HttpServer` ou `NettyReactiveWebServerFactory`.

O filtro de correlation ID substitui o valor existente:

```java
ServerHttpRequest request = exchange.getRequest().mutate()
        .headers(headers -> headers.set(HEADER, requestId)).build();
```

Esse comportamento não concatena valores e não gera duplicação de `X-Correlation-ID`.

## 3. Evidências encontradas

- Não há uso de `headers.add()` para `X-Correlation-ID` no Gateway.
- Não há manipulação manual de `Forwarded` ou `X-Forwarded-*`.
- Não há cópia integral de `request.getHeaders()`.
- Não há `RouteLocator` customizado.
- Não há retry configurado nas rotas.
- Não há `PreserveHostHeader`, `SetRequestHeader` ou `AddRequestHeader`.
- `AuthRouteMatchedGlobalFilter` apenas registra a rota e marca um atributo interno.
- `AuthRouteDiagnosticWebFilter` apenas executa para `/api/auth/**` e `/api/users/**`.
- O frontend adiciona `Authorization` quando existe sessão, mas não adiciona `Forwarded`, `X-Forwarded-*`, `Cookie` ou `X-Correlation-ID`.
- O CORS declara headers permitidos, mas não cria headers grandes.
- A exceção ocorre durante o decode HTTP do Netty, antes de `WebFilter` e `GlobalFilter`.

## 4. Filtros customizados

### `CorrelationIdFilter`

Arquivo: `backend/api-gateway/src/main/java/com/techstore/gateway/filter/CorrelationIdFilter.java`

- É um `GlobalFilter`.
- Executa em todas as rotas.
- Lê `X-Correlation-ID`.
- Gera UUID quando o header não existe.
- Usa `headers.set()` para modificar a requisição.
- Usa `set()` também na resposta.
- Não copia todos os headers.
- Não adiciona `Forwarded` ou `X-Forwarded-*`.

### `AuthRouteDiagnosticWebFilter`

Arquivo: `backend/api-gateway/src/main/java/com/techstore/gateway/filter/AuthRouteDiagnosticWebFilter.java`

- É um `WebFilter` de alta precedência.
- Executa somente em `/api/auth` e `/api/users`.
- Usa `headers.set()` para `X-Correlation-ID`.
- Define o mesmo header na resposta.
- Não executa em `/api/products` nem em `/api/categories`.

### `AuthRouteMatchedGlobalFilter`

Arquivo: `backend/api-gateway/src/main/java/com/techstore/gateway/filter/AuthRouteMatchedGlobalFilter.java`

- É um `GlobalFilter`.
- Marca a rota como encontrada.
- Registra informações da rota.
- Não altera headers.
- Não executa forwarding adicional.

Mesmo nas rotas de autenticação, os dois filtros que tratam correlation ID usam `set()`. Portanto, podem sobrescrever o mesmo valor, mas não acumulá-lo.

## 5. Fluxo atual dos headers

```text
Frontend
  |
  | Authorization opcional
  | Content-Type
  | X-Correlation-ID somente se fornecido
  v
Railway Edge
  |
  | Forwarded
  | X-Forwarded-For
  | X-Forwarded-Host
  | X-Forwarded-Proto
  | X-Forwarded-Port
  | X-Real-Ip
  | X-Railway-Request-Id
  | X-Request-Start
  v
API Gateway / Reactor Netty
  |
  | Decode e validação dos headers
  | Falha quando o limite é excedido
  v
Spring WebFlux
  |
  | CorrelationIdFilter
  | AuthRouteDiagnosticWebFilter em auth/users
  | AuthRouteMatchedGlobalFilter
  v
Spring Cloud Gateway
  |
  | Forwarding para o serviço de destino
  v
Product Service
```

Como o erro ocorre durante o decode do Netty, os filtros Spring não conseguem aumentar os headers da requisição que falha na entrada.

## 6. Headers duplicados

| Header | Situação |
|---|---|
| `X-Correlation-ID` | Não há duplicação comprovada. O código usa `set()`. |
| `Forwarded` | Não há manipulação manual. Pode ser gerado por proxy ou filtros padrão. |
| `X-Forwarded-For` | Não há manipulação manual. Pode acumular valores em múltiplos proxies. |
| `X-Forwarded-Host` | Não há manipulação manual encontrada. |
| `X-Forwarded-Proto` | Não há manipulação manual encontrada. |
| `X-Forwarded-Port` | Não há manipulação manual encontrada. |
| `X-Real-Ip` | Não há manipulação manual encontrada. |
| `Authorization` | É enviado pelo frontend quando há sessão; não é duplicado no código encontrado. |
| `Cookie` | Não é criado pelo frontend nem pelos filtros encontrados; o valor real em produção não foi medido. |

## 7. Rotas

No Gateway, as rotas estão definidas assim:

```yaml
- id: product-service
  uri: ${PRODUCT_SERVICE_URL:http://localhost:8081}
  predicates:
    - Path=/api/products/**,/api/categories/**

- id: cart-service
  uri: ${CART_SERVICE_URL:http://localhost:8083}
  predicates:
    - Path=/api/cart/**

- id: user-service
  uri: ${USER_SERVICE_URL:http://localhost:8082}
  predicates:
    - Path=/api/auth/**,/api/users/**
  filters:
    - StripPrefix=1
```

As rotas esperadas estão corretas quando as variáveis de ambiente de produção estão configuradas.

O fallback de Product Service aponta para `localhost:8081`. Isso é compatível com o cenário local antigo, mas deve ser sobrescrito no Railway por `PRODUCT_SERVICE_URL`. Não há evidência de que esse fallback seja a causa do erro atual.

## 8. Configurações de deploy

O repositório não contém arquivo específico do Railway.

A documentação de deploy orienta configurar no Railway:

- `PRODUCT_SERVICE_URL`;
- `USER_SERVICE_URL`;
- `CART_SERVICE_URL`;
- `SERVER_PORT`;
- `CORS_ALLOWED_ORIGINS`.

O Dockerfile do Gateway apenas inicia o JAR na porta 8080 e não configura parâmetros adicionais para Netty.

O `docker-compose.yml` usa `product-service:8081` no ambiente local, porque o Product Service local é configurado para escutar nessa porta. Isso não representa necessariamente a configuração do Railway.

## 9. Limite de 8192 bytes

O Gateway depende do Spring Cloud Gateway, que usa Reactor Netty no servidor WebFlux. O limite de 8192 bytes é compatível com o limite padrão do decoder HTTP do Netty/Reactor Netty.

O repositório não sobrescreve esse limite.

O Maven não estava disponível no ambiente da auditoria, então não foi possível confirmar a versão transitiva exata de Reactor Netty por `dependency:tree`.

Aumentar o limite para 16 KB ou 32 KB pode funcionar como workaround, mas não deve ser considerado a correção principal antes de descobrir por que os headers ultrapassaram 8192 bytes.

## 10. Possibilidade de loop

Não foi encontrada configuração que encaminhe para:

- o próprio Gateway em `localhost:8080`;
- `localhost:8080` no Product Service;
- uma rota que retorne do Product Service para o Gateway;
- um retry que repita a requisição indefinidamente.

O endereço `L:/127.0.0.1:8080` em logs do Netty é compatível com o socket local do servidor e, isoladamente, não prova um loop de proxy.

## 11. Problemas secundários

### Médio

O fallback do Gateway usa `localhost:8081` para Product Service, enquanto o Product Service possui porta padrão 8080 no próprio `application.yml`. O Compose sobrescreve esse valor, mas uma implantação sem `PRODUCT_SERVICE_URL` pode falhar por conexão recusada.

### Baixo

Não há testes que verifiquem o encaminhamento de todos os headers `Forwarded` e `X-Forwarded-*`.

### Baixo

Não há observabilidade que registre os tamanhos dos headers recebidos antes do encaminhamento.

## 12. Correção recomendada

A correção deve começar pela medição dos headers reais recebidos no ambiente de produção:

1. Registrar nomes e tamanhos dos headers recebidos pelo Gateway.
2. Comparar uma requisição que falha com uma requisição simples.
3. Verificar principalmente `Cookie`, `Authorization`, `Forwarded` e `X-Forwarded-For`.
4. Confirmar se Railway Edge, CDN ou outro proxy está acumulando valores.
5. Corrigir a origem da repetição, caso exista.
6. Só então avaliar o aumento do limite do Netty.

Não há alteração de código confirmada como necessária com base apenas no repositório.

## 13. Workaround

Configurar um limite maior de headers pode reduzir as falhas temporariamente.

Esse workaround não corrige a causa se houver:

- headers de forwarding acumulados;
- cookies excessivos;
- tokens JWT muito grandes;
- proxy configurado para repetir headers;
- loop de proxy.

## 14. Classificação de risco

- **ALTO:** requisições legítimas falham no Gateway antes de alcançar o Product Service.
- **MÉDIO:** possível crescimento de headers de forwarding entre Railway Edge e Gateway; não confirmado no código.
- **MÉDIO:** `Cookie` ou `Authorization` excessivamente grandes; não medido.
- **BAIXO:** fallback local apontando para `localhost:8081`.
- **BAIXO:** ausência de testes específicos para headers de proxy.

Não há evidência para classificar `X-Correlation-ID` como causa principal.

## 15. Checklist posterior

- [ ] Medir o tamanho individual e total dos headers recebidos.
- [ ] Registrar a quantidade de valores de cada header forwarding.
- [ ] Conferir os headers configurados no Railway Edge.
- [ ] Confirmar todas as URLs privadas configuradas no Railway.
- [ ] Confirmar a versão efetiva de Reactor Netty.
- [ ] Verificar proxies intermediários, CDN e regras de rewrite.
- [ ] Corrigir eventual acumulação de `Forwarded` ou `X-Forwarded-*`.
- [ ] Avaliar aumento do limite somente depois da investigação.
- [ ] Adicionar testes de encaminhamento de headers.
- [ ] Não remover filtros de correlation ID sem evidência de duplicação.

## 16. Conclusão

O problema é **provável, mas não confirmado**.

O código não mostra duplicação de headers causada pelos filtros customizados. A exceção acontece antes da execução desses filtros, no decoder HTTP do Reactor Netty. A hipótese mais forte é que a requisição recebida em produção já exceda o limite de 8192 bytes por headers gerados ou acumulados no Railway Edge, por outro proxy intermediário, ou por valores grandes de `Cookie`/`Authorization`.

Nenhum arquivo de código foi alterado nesta auditoria.
