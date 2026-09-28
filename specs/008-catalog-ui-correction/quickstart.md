# Quickstart de Validação: Correção Responsiva do Catálogo

## Pré-requisitos

- Node.js 20+ e npm 10+.
- Dependências instaladas em `frontend/`.
- API Gateway/Product Service disponíveis para dados reais ou mocks dos testes.

## Execução

```powershell
cd frontend
npm install
npm run dev -- --port 5174
```

Abra `http://localhost:5174/`.

## Validação automatizada

```powershell
cd frontend
npm test
npm run build
```

Os testes devem confirmar que chamadas, parâmetros, resultados, rotas, filtros, ordenação,
paginação e estados funcionais não foram alterados.

## Matriz visual

Validar cada viewport: `375`, `390`, `430`, `768`, `1024`, `1280`, `1440` e `1920` px.

Para cada largura:

1. Medir `document.documentElement.scrollWidth` e confirmar que não ultrapassa a largura útil.
2. Confirmar sequência header, breadcrumb, hero compacta, busca, categorias e início do catálogo.
3. Confirmar que título, descrição e busca não criam espaço vertical desproporcional.
4. Conferir header: desktop completo; mobile com logo, conta, carrinho e menu.
5. Conferir filtros: sidebar proporcional no desktop, recolhíveis no tablet e drawer/superfície no
   mobile, com aplicar, fechar e limpar acessíveis.
6. Conferir grid e cards com imagem, sem imagem, texto longo, preço zero e estoque zero.
7. Simular loading, erro, vazio e sem resultados; confirmar mensagens e ações existentes.
8. Navegar por teclado e ativar redução de movimento; confirmar foco, labels, `alt` e ausência de
   dependência exclusiva de hover.
9. Pesquisar, filtrar, ordenar, paginar e abrir detalhes; comparar chamadas e resultados com a
   baseline funcional.

## Referências

- [Invariantes de UI](contracts/catalog-ui-invariants.md)
- [Modelo de apresentação](data-model.md)
- [Spec funcional](../006-frontend-product-catalog/spec.md)
- [Referência visual anterior](../007-catalog-redesign/spec.md)
