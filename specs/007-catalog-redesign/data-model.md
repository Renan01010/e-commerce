# Modelo de Dados de Apresentação: Redesign do Catálogo

O redesign não cria entidades persistidas nem altera os tipos funcionais do catálogo. Este
modelo registra apenas como dados existentes serão priorizados visualmente.

## Produto

- `imageUrl`: mídia principal do card; quando ausente ou inválida, usa o substituto atual.
- `categoryId`: contexto visual da categoria já carregada; não cria categoria nova.
- `name`: título principal, com espaço estável para nomes longos.
- `description`: apoio secundário quando a composição escolhida o comportar.
- `price`: valor atual em BRL, sem recálculo.
- `quantity`: disponibilidade visual derivada do modelo atual.
- `brand` e `sku`: metadados secundários existentes.
- `rating`, `previousPrice`, `discount`: campos opcionais apenas se existirem em fonte confiável;
  o modelo atual não os fornece e a implementação não os simulará.

## Categoria

Categoria existente com `id`, `name` e possível `parentCategoryId`, utilizada para filtro,
breadcrumb ou contexto visual. O redesign não altera sua hierarquia ou persistência.

## Consulta do catálogo

Estado funcional existente contendo pesquisa, filtros, ordenação e paginação. A UI pode refletir
seus estados selecionado/limpo, mas não muda seus parâmetros nem regras.

## Estados visuais

- **Sucesso**: hero, toolbar, filtros e cards com dados retornados.
- **Carregamento**: skeletons com dimensões equivalentes à grade final.
- **Vazio**: catálogo sem produtos, com mensagem e ação de recuperação.
- **Sem resultados**: busca/filtros sem correspondências, distinguida de falha técnica.
- **Erro**: aviso seguro, critérios preservados e recuperação existente.
- **Indisponível**: carrinho futuro sem contador ou ação de mutação.

## Regras de apresentação

A apresentação não pode criar fonte de verdade local, alterar preço, inferir desconto, inventar
avaliação ou sugerir estoque diferente de `quantity`.
