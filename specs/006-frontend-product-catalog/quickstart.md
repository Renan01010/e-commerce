# Quickstart de Validação: Catálogo de Produtos no Frontend

## Pré-requisitos

- Node.js 20+ e npm 10+.
- API Gateway, Product Service e PostgreSQL disponíveis, ou mocks equivalentes nos testes.
- Dependências instaladas em `frontend/`.

## Instalação e execução

Na raiz do repositório:

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

Os testes devem cobrir a listagem, card, detalhe, cliente HTTP e estados sem depender de um
servidor externo. O build deve concluir sem erros de TypeScript.

## Cenários manuais

1. Abrir `/` sem sessão e confirmar listagem, preço, imagem/substituto e disponibilidade.
2. Pesquisar por nome, marca ou descrição; combinar categoria, preço, marca e estoque; mudar a
   ordenação; confirmar retorno à primeira página.
3. Limpar pesquisa e filtros e confirmar que o catálogo padrão retorna.
4. Abrir um card, conferir detalhes e categoria, e voltar ao catálogo.
5. Abrir um UUID inexistente ou inativo e confirmar estado de não encontrado.
6. Simular carregamento lento, resposta vazia e falha do Gateway; confirmar estados distintos,
   mensagem segura e ação de recuperação.
7. Testar viewport de 320 px e desktop; navegar pelos controles usando somente teclado.
8. Confirmar que o botão de carrinho permanece desabilitado e nenhuma operação de carrinho,
   checkout ou pedido é iniciada.

## Referências

- [Contrato consumido](contracts/catalog-gateway-consumption.md)
- [Modelo de dados](data-model.md)
- [Contrato OpenAPI do Product Service](../001-foundation-product-catalog/contracts/product-service-api.openapi.yaml)
