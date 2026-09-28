# Modelo de Dados: Gerenciamento de Categorias

Esta feature evolui a entidade e a tabela `categories` existentes no Product Service. Não cria outra tabela de categorias nem altera os identificadores/relacionamentos atuais. O contrato de origem é `specs/001-foundation-product-catalog/data-model.md`.

## Categoria Persistida

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| `id` | UUID | Sim | Identidade existente; preservada na migration e em toda atualização. |
| `name` | texto até 100 caracteres | Sim | Não pode ser vazio/branco; unicidade global preserva a comparação atual do valor persistido. |
| `slug` | texto canônico até 120 caracteres | Sim | Novo campo; minúsculo ASCII, caracteres alfanuméricos e hífen simples. Único globalmente, inclusive quando `isActive=false`. |
| `description` | texto até 500 caracteres | Não | Mantém limite existente. |
| `parentCategoryId` | UUID | Não | Relação auto-referencial existente; criação exige pai ativo, quando informado. |
| `displayOrder` | inteiro | Sim | Mantém ordenação e default existentes. |
| `isActive` | boolean | Sim | Ativação lógica; default existente `true`. |
| `createdAt` / `updatedAt` | timestamp UTC | Sim | Mantém auditoria existente; updatedAt muda em edição/ativação. |
| `createdBy` / `updatedBy` | identificador do principal | Sim | Mantém auditoria existente, preenchida a partir do administrador autenticado nas escritas. |

## Regras de Slug

- Na criação, se slug não vier informado, derivar do nome: transliterar diacríticos, converter para minúsculas, substituir sequências de caracteres não alfanuméricos por um hífen e remover hífens das extremidades.
- Slug informado segue a mesma normalização; resposta/persistência contêm o valor canônico. Resultado vazio ou acima de 120 caracteres é `400`.
- No update, slug omitido preserva o slug atual mesmo que o nome mude. Slug explícito permite alteração administrativa.
- Conflito de slug retorna `409`; nenhuma categoria é parcialmente atualizada. Slug de registro inativo continua reservado.
- O backfill só pode persistir um conjunto completo e único. Nenhum slug pode receber sufixo automaticamente para desempatar.

## Mapa de Estado

| Estado atual | Operação | Estado seguinte | Observação |
|---|---|---|---|
| Não existe | Criar por ADMIN | Ativa | UUID novo, slug informado ou derivado, auditoria inicial. |
| Ativa | Atualizar por ADMIN | Ativa | Slug omitido preservado; conflitos não gravam nada. |
| Ativa sem produtos | DELETE por ADMIN | Inativa | Soft delete existente; linha e slug permanecem. |
| Ativa com produtos | DELETE por ADMIN | Ativa | Rejeita com `409`, regra atual preservada. |
| Inativa | PATCH activation por ADMIN, sem body | Ativa | Preserva ID, slug, parent e referências. |
| Inativa | Consulta pública | Não visível | Retorna 404 por ID e não aparece na lista pública. |
| Inativa | Consulta administrativa explícita | Inativa | Requer ADMIN e `includeInactive=true`; slug continua reservado. |

## Migration de Slug Legado

1. Ler todos os registros, ativos e inativos, e calcular slug candidato com a mesma normalização da criação.
2. Aplicar o mapa externo aprovado às categorias indicadas por `categoryId`.
3. Antes de gravar qualquer slug ou constraint, verificar cobertura de todas as colisões, formato canônico, ausência de slug vazio e unicidade global do conjunto resultante.
4. Se houver colisão sem mapa suficiente, slug inválido, ID desconhecido ou conflito residual, abortar transacionalmente antes de DDL/DML de persistência e reportar os IDs, nomes e candidatos conflitantes.
5. Se válido, adicionar/preencher `slug`, estabelecer `NOT NULL` e unicidade. Preservar UUIDs, nomes, estado ativo, `parent_category_id`, `products.category_id` e todas as linhas existentes.
6. A migration não deve criar tabela auxiliar permanente nem alterar o conteúdo de nome para resolver slug.

`CATEGORY_SLUG_OVERRIDES_JSON` contém um objeto JSON `categoryId -> slug` temporário de deployment. Para cada grupo de colisão, o mapa deve listar explicitamente todos os IDs, mesmo quando um deles conservar o slug candidato original. Após migração bem-sucedida, a configuração pode ser removida: os valores ficam persistidos e Flyway registra a versão aplicada.

## Projeção REST

A resposta atual da categoria mantém `id`, `name`, `description`, `parentCategoryId`, `displayOrder`, `isActive`, `createdAt` e `updatedAt`, adicionando `slug`. Campos de auditoria internos `createdBy`/`updatedBy` permanecem conforme o contrato atual e não devem ser expostos apenas por esta feature.

A busca é transitória: parâmetro opcional `search` aplica correspondência parcial sem distinção de caixa sobre nome, descrição e slug; não constitui entidade persistida.
