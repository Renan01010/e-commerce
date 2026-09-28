# Contrato de UI: Invariantes da Correção Responsiva

Este contrato descreve invariantes de apresentação e regressão. Não altera APIs ou contratos do
Product Service.

## Invariantes funcionais

- `/` continua sendo o catálogo público.
- `/products/:id` continua abrindo o detalhe correto.
- A UI usa as mesmas consultas, parâmetros, filtros, ordenação, paginação e estados existentes.
- Abrir/fechar/aplicar/limpar filtros não cria uma regra de negócio nova.
- Menu mobile, conta e carrinho não iniciam checkout ou mutações de carrinho.

## Invariantes responsivos

- `scrollWidth` não ultrapassa a largura útil em 375, 390, 430, 768, 1024, 1280, 1440 e 1920 px.
- Hero, busca, categorias e início do catálogo aparecem em sequência sem vazio vertical desproporcional.
- Header desktop não é simplesmente reduzido: mobile possui navegação apropriada.
- Desktop usa sidebar proporcional; tablet usa filtros recolhíveis; mobile usa superfície própria.
- Cards preservam área de mídia, altura, texto e link em todas as faixas.

## Invariantes de acessibilidade

- Todos os controles possuem nome acessível e foco visível.
- Filtros abertos podem ser fechados, aplicados e limpos por teclado.
- Imagens usam `alt` ou substituto identificável.
- Movimento respeita `prefers-reduced-motion`.
