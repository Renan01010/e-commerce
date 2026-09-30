# Pesquisa e Decisões: Gerenciamento de Categorias

**Data**: 2026-09-28 | **Branch**: `009-category-management`

## Contexto Verificado

- O Product Service existente já é proprietário do domínio Categoria e do database PostgreSQL usado também pelo catálogo de produtos. A tabela `categories` contém UUID, nome único, descrição, parent, display order, active e auditoria; `products.category_id` referencia essa tabela.
- `CategoryService` implementa listar/consultar categorias ativas, criar, editar nome/descrição/ordem e desativar. Nome duplicado resulta em conflito. Desativação de categoria com produtos vinculados resulta em conflito. Não há reativação, slug ou busca por categorias.
- `CategoryController` expõe as rotas `/api/categories` existentes. `GET` é público e as escritas usam o ADMIN autenticado. `SecurityConfig` atualmente libera GETs em `/api/**`, então o parâmetro de visibilidade administrativa precisa ter autorização explícita antes de retornar linhas inativas.
- `ApiModels.CategoryResponse` não contém slug; `CreateCategoryRequest` e `UpdateCategoryRequest` também não o recebem. A integração atual do catálogo espera lista de categorias ativas e usa nome, descrição, parent e display order.
- Flyway está habilitado no Product Service; `V1__create_catalog_schema.sql` cria `categories` com `name UNIQUE`, pai auto-referencial e FK de produto com `ON DELETE RESTRICT`. `hibernate.ddl-auto` é `validate`.
- O backend é Java 21, Spring Boot 3.3.13, Maven multi-módulo; Springdoc 2.6.0 já documenta a API. Testes usam JUnit 5, Mockito, Spring MockMvc e Testcontainers PostgreSQL.
- `specs/001-foundation-product-catalog/contracts/product-service-api.openapi.yaml` é o contrato versionado atual para categorias; deve ser ampliado, não duplicado.

## Decisões

### 1. Completar o Product Service e preservar APIs/armazenamento

**Decisão**: manter `Category` no Product Service, evoluir o recurso `/api/categories`, a tabela `categories` e o contrato OpenAPI de 001. Nenhum novo microsserviço, módulo Maven, database, tabela ou CRUD paralelo será criado.

**Racional**: Product Service já possui autoridade, entidades, repositório, regras e rota Gateway para categorias. Duplicar a capacidade criaria fontes de verdade e incompatibilidade com `products.category_id`.

**Alternativas consideradas**: criar Category Service, schema separado ou API sob novo contexto. Rejeitadas porque duplicariam o domínio existente e exigiriam novas integrações sem valor adicional.

### 2. Separar leitura pública de visibilidade administrativa

**Decisão**: manter `GET /api/categories` público e active-only por padrão, acrescentando busca opcional. `includeInactive=true` permite consulta de gestão somente para ADMIN, inclusive na leitura por ID. A resposta pública jamais inclui categorias inativas.

**Racional**: preserva a navegação pública do catálogo e viabiliza localizar categorias desativadas para manutenção/reativação.

**Alternativas consideradas**: tornar toda listagem autenticada (quebraria o catálogo público) ou criar outro CRUD/serviço admin completo (duplicaria endpoints). A autorização condicional da opção administrativa atende ambos os casos mantendo o recurso existente.

### 3. Slug canônico e unicidade sem alteração silenciosa

**Decisão**: manter slug ASCII minúsculo com palavras separadas por hífens. Slug informado é normalizado; omitido na criação é derivado do nome. Colisão em operações normais retorna 409. Slugs são únicos globalmente, inclusive para categorias inativas. No update, omitir slug preserva o valor existente.

**Racional**: gera identificadores adequados a URLs, evita reciclar endereços após soft delete e mantém slug estável ao corrigir apenas o nome.

**Alternativas consideradas**: sobrescrever slug em toda alteração de nome (quebraria referências futuras) ou adicionar sufixos automaticamente (resultado imprevisível e contra a clarificação aprovada). Ambas rejeitadas.

### 4. Preflight/migration com mapa externo obrigatório em colisões históricas

**Decisão**: criar uma migration Flyway versionada Java para evoluir o schema e o slug backfill, reutilizando o Flyway já presente. Antes de qualquer DDL/DML que persista slugs ou imponha constraints, carregar categorias de todos os estados, derivar candidatos com a mesma normalização e verificar unicidade do conjunto final.

Quando houver colisão, a migration lança falha com `categoryId`, nome e slug candidato de cada registro conflitante; por estar em transação no PostgreSQL, o schema/data da migration deve permanecer sem alteração. Para reexecutar, deployment fornece `CATEGORY_SLUG_OVERRIDES_JSON`, um JSON `categoryId -> slug` externo à imagem. O mapa deve cobrir todos os IDs de cada grupo conflitante. A migration valida formato e unicidade do resultado completo (incluindo registros inativos e slugs sem colisão) antes de gravar. Se estiver ausente, incompleto, inválido ou ainda conflitante, falha novamente sem sufixar nomes/slugs. Depois do sucesso, a variável pode ser removida: o slug passa a estar persistido e a migration fica registrada pelo Flyway.

**Racional**: não é possível adivinhar qual slug de URL o negócio deseja para duas categorias históricas com o mesmo candidato. Falhar antes de escrever é reversível, protege relações e permite resolução explícita por ID sem renomear categorias.

**Alternativas consideradas**: sufixos UUID/numéricos (rejeitados pela decisão do usuário); descartar/renomear categoria (perde estabilidade ou altera dados); ignorar unicidade (viola contrato). Executar atualização parcial antes de detectar todas as colisões (risco de inconsistência) também é rejeitado.

**Operação no deploy**:

1. Fazer backup e executar o deploy do Product Service com `CATEGORY_SLUG_OVERRIDES_JSON` ausente/vazio. A migration realiza a verificação antes da gravação. Sem colisões, aplica o backfill e constraints normalmente.
2. Se houver conflito, startup/deploy falha e o diagnóstico lista ID, nome e slug candidato; nenhuma categoria é alterada e nenhuma constraint parcial é instalada.
3. Responsável pelo catálogo escolhe slug canônico único para cada ID reportado, incluindo todas as linhas do grupo de colisão. Fornece o mapa JSON pelo mecanismo de configuração de deployment, sem commit de dado específico de produção no repositório.
4. Reiniciar/reexecutar o deploy com o mapa. A migration recalcula todos os candidatos e valida que cada registro tenha slug não vazio, formato canônico e valor único entre ativos/inativos antes de qualquer escrita.
5. Após o Flyway concluir com sucesso, remover o mapa temporário da configuração; manter os valores persistidos. Verificar API, schema e referências de produto.

### 5. Autorização e erros no padrão já existente

**Decisão**: reutilizar JWT/roles do Product Service; toda escrita permanece ADMIN-only. Leituras públicas retornam active-only. `includeInactive=true` exige ADMIN. Usar `ErrorResponse` e mapeamentos atuais: 400 validação, 401 não autenticado, 403 sem role, 404 ausente/oculto, 409 nome/slug duplicado ou categoria com produtos impedindo desativação.

**Racional**: mantém compatibilidade com a configuração e as respostas atuais e impede exposição de estado administrativo.

### 6. Contrato documentado no OpenAPI de 001

**Decisão**: atualizar a especificação OpenAPI versionada existente e a documentação Springdoc do Product Service. Incluir slug em response/request, busca/visibilidade, ativação sem body e cenários de erro.

**Racional**: há uma fonte de contrato existente usada por serviços e testes. Um segundo arquivo OpenAPI independente poderia divergir.

### 7. Testes no padrão já utilizado

**Decisão**: ampliar testes de domínio/serviço para slug, duplicidade e ativação; testar autorização, filtros e erros por MockMvc; testar migration/backfill/rollback por Testcontainers PostgreSQL, incluindo colisão sem mapa, mapa válido, mapa incompleto, colisão com inativos e preservação de FK; atualizar integração de catálogo e assertivas OpenAPI.

**Racional**: migration, autorização e contrato são fronteiras críticas; testes de unidade apenas não comprovam constraint real nem transação de rollback.

## Pendência Técnica para Planejamento

A migration Java deve usar a normalização compartilhada pelo serviço e ler o mapa pelo placeholder Flyway configurado a partir de `CATEGORY_SLUG_OVERRIDES_JSON`. O plano/tarefas devem fixar como essa propriedade externa será ligada ao `spring.flyway.placeholders` e como o relatório de colisões é apresentado no log de startup, mantendo o requisito de não gravar antes da validação integral.
