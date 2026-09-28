# Guia de Validação: Carrinho no Frontend

## Pré-requisitos

- Node.js/npm compatíveis com o projeto frontend.
- Dependências instaladas em `frontend/`.
- Para teste integrado manual: API Gateway e Cart Service existentes acessíveis pela `VITE_API_URL`, Product Service disponível para consultas do carrinho e credencial que permita obter JWT válido pelo login atual.
- O frontend mantém token somente em memória. Após reload, autenticar novamente; não adicionar token em arquivo ou comando versionado.

## Testes Automatizados

Na pasta `frontend/`:

```bash
npm test
npm run build
```

Resultado esperado: testes de API/store/página/header aprovados; TypeScript e build Vite concluídos. Os testes devem usar mocks para os endpoints e não depender de serviços reais.

## Validação Manual Integrada

1. Configure `VITE_API_URL` para o Gateway ativo (padrão local `/api`) e inicie o frontend:

   ```bash
   npm run dev
   ```

2. Sem autenticação, abra diretamente `/cart`. A página deve permanecer nessa rota, explicar a necessidade de login e mostrar link para `/login`; não deve iniciar GET de carrinho nem exibir linhas de outra sessão.
3. Entre com uma conta válida, navegue até um produto ativo e selecione quantidade positiva. A inclusão deve enviar `POST /api/cart/items`, exibir feedback e atualizar o badge com o estado retornado.
4. Abra `/cart` pelo cabeçalho. O GET deve ocorrer no máximo uma vez para carregar a sessão; linhas e badge devem refletir `items` e soma de `quantity`.
5. Aumente/reduza quantidade e verifique PUT e atualização pela resposta. Remova item e verifique que só depois de 204 a linha/badge mudam.
6. Acione “Limpar carrinho”, cancele e confirme que nenhum DELETE ocorreu; abra novamente, confirme, verifique `DELETE /api/cart` e lista/badge vazios após 204.
7. Verifique produto com `available: false` e `product: null`: indicação indisponível, sem tentativa de acessar resumo, controle de quantidade indisponível e remoção disponível.
8. Simule `401`, `404`, `503` e falha de rede: mensagens claras, nenhuma mutação falsa e estado anterior preservado quando a sessão continua válida. Para 401, estado da sessão/carrinho é limpo sem redirecionamento automático.
9. Verifique retorno ao catálogo, navegação por teclado, foco visível e viewports desktop, tablet e mobile; confirme ausência de overflow horizontal.

## Contrato

Consulte [contrato de consumo](contracts/cart-gateway-consumption.md) e a fonte autoritativa [OpenAPI 004-cart](../004-cart/contracts/cart-service-api.openapi.yaml). Nenhum endpoint adicional deve ser configurado para esta feature.