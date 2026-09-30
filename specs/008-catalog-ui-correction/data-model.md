# Modelo de Dados de Apresentação: Correção Responsiva

Nenhuma entidade funcional nova é criada. O documento registra os estados transitórios necessários
para a composição responsiva.

## Produto e Categoria

Usam exatamente os tipos do catálogo atual. A UI apresenta nome, preço, quantidade,
imagem/substituto, marca/SKU e categoria já retornados, sem recalcular ou inventar dados.

## Consulta do catálogo

Busca, filtros, ordenação e paginação permanecem no `catalogStore`. A correção não cria cópia
local desses valores.

## Estado visual

- **Sucesso**: hero compacta, controles e grade com dados reais.
- **Carregamento**: skeletons com dimensões equivalentes aos cards.
- **Erro**: mensagem existente em aviso integrado, com retry existente.
- **Vazio/sem resultados**: mensagem e ação para limpar/ajustar critérios.
- **Filtros fechados**: valores continuam ativos no store e são resumidos visualmente.
- **Filtros abertos**: superfície transitória para interação por toque/teclado.
- **Menu mobile aberto**: navegação temporária sem alterar rota ou autenticação.

## Regras de apresentação

Container, filtros, grid e cards devem responder à largura disponível. Nenhum estado visual pode
alterar o significado do produto, do preço, da quantidade ou dos critérios da consulta.
