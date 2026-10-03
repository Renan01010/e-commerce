# Quickstart de Validação: Home e Catálogo de Produtos

## Pré-requisitos

- Node.js e npm compatíveis com o projeto.
- Dependências frontend instaladas em `frontend/`.
- Gateway/Product Service para conferência manual integrada; testes automatizados usam mocks e não dependem desses serviços.

## Execução local

Na raiz do repositório:

```powershell
Set-Location frontend
npm run dev
```

Abrir `http://localhost:5173`.

## Validação automatizada

Na pasta `frontend/`:

```powershell
npm test
npm run build
```

Os testes de Home, catálogo, produto, stores e carrinho devem mockar os clients existentes. O build deve terminar sem erros de TypeScript.

## Cenários manuais

1. Antes de implementar layout da Home, consultar `references/TechStore_ Tecnologia sem limites.png` como referência visual desktop. O nome `references/home-desktop-reference.png` inicialmente indicado não está presente; usar o arquivo fornecido existente.
2. Comparar header, Hero, benefícios, categorias, grid de produtos, banners, hierarquia, espaçamento e cores com a referência, sem copiar textos comerciais, ofertas/percentuais, produtos, marcas, imagens, avaliações, categorias ou preços.
3. Abrir `/` sem sessão; confirmar Hero, benefícios informativos, categorias reais ou seus estados de loading/erro, e até oito produtos mais recentes.
4. Verificar que a requisição da Home usa `sortBy=newest`, `sortOrder=desc`, `page=0`, `pageSize=8` e que não substitui filtros/página de `/catalog`; navegar Home → catálogo → Home e confirmar reutilização das categorias após resposta bem-sucedida.
5. Abrir uma categoria retornada pelo serviço e confirmar navegação para `/catalog?categoryId=<id>`; verificar que a UI nunca mostra UUID; remover o parâmetro e confirmar que o filtro anterior é limpo.
6. Buscar na Home, confirmar navegação para `/catalog?query=<termo>` e verificar filtros, ordenação, paginação, vazio e retry do catálogo; alterar busca/categoria no catálogo e testar voltar/avançar para confirmar restauração pela URL.
7. Abrir um card e verificar `/products/:id`, imagem/fallback, preço atual e disponibilidade retornados; confirmar ausência de avaliações, descontos ou preço anterior fictícios.
8. Autenticar com a sessão já suportada, aguardar o carrinho carregar, adicionar um produto e confirmar loading, bloqueio de clique repetido, feedback e badge a partir da resposta do carrinho.
9. Testar resposta negativa do carrinho (por exemplo, sessão expirada ou conflito): nenhuma confirmação de sucesso deve aparecer e os valores confirmados não devem ser calculados no navegador.
10. Abrir links de conta e carrinho, confirmar manutenção das rotas existentes, badge condicionado ao estado carregado e comportamento de sessão existente.
11. Inspecionar larguras 1280 px, 768 px e 375 px; confirmar menu, busca, rolagem horizontal apenas nas categorias quando necessária, grid sem overflow horizontal de página e controles confortáveis ao toque.
12. Navegar com teclado; verificar foco visível, nomes acessíveis, labels, alternativa de imagem e anúncios de loading/erro sem depender somente de cor.

## Referências

- [Especificação](spec.md)
- [Plano](plan.md)
- [Pesquisa e decisões](research.md)
- [Modelo de dados](data-model.md)
- [Contrato frontend via Gateway](contracts/catalog-home-gateway-consumption.md)
