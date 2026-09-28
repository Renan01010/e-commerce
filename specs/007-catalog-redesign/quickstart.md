# Quickstart de Validação: Redesign do Catálogo

## Pré-requisitos

- Node.js 20+ e npm 10+.
- Dependências instaladas em `frontend/`.
- API Gateway e Product Service disponíveis para validar dados reais, ou mocks já usados nos testes.

## Execução

```powershell
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`.

## Validação automatizada

```powershell
cd frontend
npm test
npm run build
```

Os testes devem confirmar que o redesign não altera rotas, chamadas, filtros, ordenação,
paginação, estados funcionais ou o botão de carrinho desabilitado.

## Matriz visual manual

1. **Desktop (1440 px)**: conferir header completo, hero, pesquisa destacada, filtros compactos,
   quantidade, ordenação e grade de quatro colunas.
2. **Tablet (900 px)**: conferir redução da grade, filtros legíveis, sem overflow horizontal e
   header ainda navegável.
3. **Mobile (320 px)**: conferir header compacto, hero sem excesso de altura, filtros utilizáveis,
   pesquisa acessível, grade sem texto cortado e mensagens visíveis.
4. **Teclado**: percorrer header, pesquisa, filtros, cards, paginação e ações; confirmar foco
   visível e ordem lógica.
5. **Estados**: validar loading, erro, catálogo vazio e busca sem resultados; cada estado deve
   ser visualmente distinto e acionável.
6. **Dados incompletos**: produto sem imagem, nome longo, quantidade zero e ausência de campos
   opcionais; nenhum card pode quebrar ou inventar informação.
7. **Movimento**: habilitar redução de movimento no sistema e confirmar que transições essenciais
   não causam desconforto nem removem feedback.
8. **Regressão**: pesquisar, filtrar, ordenar, paginar, abrir detalhe e confirmar que nenhuma
   chamada de carrinho, checkout ou backend administrativo é iniciada.

## Referências

- [Contrato de invariantes](contracts/catalog-ui-invariants.md)
- [Modelo de apresentação](data-model.md)
- [Spec funcional do catálogo](../006-frontend-product-catalog/spec.md)
