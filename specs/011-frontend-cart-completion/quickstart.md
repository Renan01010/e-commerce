# Guia de Validação: Conclusão do Carrinho no Frontend

## Pré-requisitos

- Node.js/npm compatíveis com o projeto e dependências instaladas em `frontend/`.
- Para validação integrada manual: API Gateway e serviços existentes acessíveis pela configuração atual `VITE_API_URL`, e sessão JWT obtida pelo fluxo de login existente.
- Não adicionar tokens, credenciais ou dados sensíveis a arquivos versionados.

## Testes Automatizados

Na pasta `frontend/`, executar os testes focados:

```bash
npm test -- src/services/__tests__/apiClient.test.ts src/store/__tests__/cartStore.test.ts src/components/cart/__tests__/CartItem.test.tsx src/pages/__tests__/CartPage.test.tsx src/pages/__tests__/ProductDetailPage.test.tsx src/__tests__/App.test.tsx
```

Executar gates frontend completos:

```bash
npm test
npm run build
```

Resultado esperado: testes aprovados, checagem TypeScript concluída e build Vite concluído. Testes automatizados devem mockar a API e não depender de backend real.

## Validação Manual Integrada

1. Configure `VITE_API_URL` para o Gateway (padrão local `/api`) e inicie o frontend com `npm run dev`.
2. Sem sessão, abra `/cart`: deve aparecer orientação de login; não deve haver GET autenticado nem dados de sessão anterior.
3. Autenticado, adicione um produto já presente no carrinho: a consolidação e os valores exibidos devem vir da resposta do serviço; o badge deve mostrar unidades totais.
4. Na rota `/cart`, confira preço por unidade, quantidade e subtotal de cada linha contra o snapshot/resposta, além do total oficial. Confirme que nenhum UUID aparece.
5. Simule resposta com preço indisponível e produto indisponível: não deve aparecer `R$ 0,00`; total indisponível deve ser claro; produto indisponível continua removível.
6. Aumente/diminua uma quantidade e teste conflito de estoque/limite: durante o request os controles ficam pendentes; em erro a quantidade confirmada anterior permanece e a mensagem é amigável.
7. Remova uma linha: após `204`, a linha e contador devem refletir a remoção e um GET deve atualizar o resumo. Simule falha nesse GET: a remoção continua aplicada, total fica indisponível e retry faz apenas GET.
8. Limpe o carrinho, confirme `DELETE /api/cart` e o GET subsequente; a resposta vazia atualiza resumo oficial e badge zero. Simule falha do GET: estado vazio permanece, total não é inventado e retry não repete DELETE.
9. Cancele a confirmação de limpeza e confirme que nenhuma requisição foi enviada.
10. Verifique `401`, `404`, `409`, `5xx` e falha de rede, além de navegação por teclado e layouts mobile/tablet/desktop sem overflow horizontal.

Os casos de preço desconhecido, falha de GET posterior a DELETE e status não emitidos atualmente pelo Cart Service devem ser validados com fixtures/mocks, sem alterar dados de produção.