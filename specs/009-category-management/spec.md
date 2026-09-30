# Especificação da Feature: Gerenciamento de Categorias

**Branch da Feature**: `009-category-management`  
**Criado em**: 2026-09-28  
**Status**: Draft  
**Entrada**: Completar o gerenciamento administrativo de categorias do TechStore com pesquisa, slug único e ativação, evoluindo a capacidade existente do Product Service.

## Clarificações

### Sessão 2026-09-28

- P: Como a migration deve tratar categorias existentes que gerem o mesmo slug canônico? → R: Deve abortar antes de gravar os slugs conflitantes ou ativar a constraint de unicidade, informando IDs, nomes e slug candidato. A implantação só prossegue após fornecer um mapeamento explícito de `categoryId` para slug único para todos os registros conflitantes. Não gerar sufixos automaticamente, não alterar nomes/IDs/hierarquia/vínculos e manter a unicidade global incluindo categorias inativas.
- P: Como deve funcionar o request de reativação em `PATCH /api/categories/{id}/activation`? → R: Sem body; a rota representa a ação específica de ativar e retorna `200` com a categoria atualizada.

## Visão Geral

Esta feature completa a gestão de categorias já pertencente ao Product Service. Administradores poderão criar, localizar, consultar, editar, ativar e desativar categorias com slugs únicos. O catálogo continuará podendo consultar publicamente somente categorias ativas; dados e operações administrativos sobre categorias inativas exigirão autenticação ADMIN.

A feature evolui o contrato e a persistência existentes. Não cria outro serviço, tabela ou conjunto duplicado de endpoints CRUD. Categorias continuam sendo a referência usada pelos produtos existentes e futuros.

## Relação com o Catálogo Existente

A fundação `001-foundation-product-catalog` já define e implementa categoria hierárquica no Product Service, com `name`, `description`, `parentCategoryId`, `displayOrder`, `isActive` e auditoria. A tabela `categories`, o CRUD parcial e o contrato OpenAPI já existem.

Contrato de origem: [OpenAPI do Product Service](../001-foundation-product-catalog/contracts/product-service-api.openapi.yaml).

No estado atual:

- `GET /api/categories` lista categorias ativas e é público; `GET /api/categories/{id}` consulta categoria ativa.
- `POST /api/categories`, `PUT /api/categories/{id}` e `DELETE /api/categories/{id}` exigem ADMIN. DELETE é soft delete e desativa a categoria.
- Nomes são globalmente únicos, a criação valida categoria pai ativa e categorias que já possuem produtos não podem ser desativadas.
- Não existe slug, pesquisa de categorias, leitura administrativa de inativas ou operação de reativação documentada/implementada.

Esta feature preserva essas rotas e regras existentes, completa as lacunas listadas e atualiza o contrato OpenAPI de `001-foundation-product-catalog` em vez de criar um contrato concorrente.

## Cenários de Usuário e Testes

### História de Usuário 1 - Criar categoria identificável por slug (Prioridade: P1)

Como administrador, quero criar uma categoria com nome e slug único para organizá-la no catálogo e permitir referências estáveis por URL no futuro.

**Por que esta prioridade**: Categorias são necessárias para organizar o catálogo e servirão como referência no cadastro futuro de produtos.

**Teste independente**: Criar categorias com slug informado e sem slug, consultar a resposta e o estado persistido e confirmar slug canônico único; repetir nome ou slug conflitante e verificar `409` sem gravação parcial.

**Cenários de Aceitação**:

1. **Dado** um administrador autenticado e um nome válido, **quando** criar categoria sem informar slug, **então** o serviço gera um slug canônico a partir do nome e retorna a categoria criada com `201`.
2. **Dado** um administrador autenticado e um slug válido, **quando** criar categoria informando esse slug, **então** a resposta e a categoria persistida contêm o slug normalizado.
3. **Dado** nome ou slug já utilizado por qualquer categoria, inclusive inativa, **quando** tentar criar duplicata, **então** recebe `409 Conflict` e nenhum registro parcial é criado.
4. **Dado** nome, descrição, parentCategoryId ou slug inválido, **quando** enviar criação, **então** recebe `400 Bad Request` com o formato de erro comum, sem persistência.
5. **Dado** um chamador sem ADMIN, **quando** tentar criar categoria, **então** recebe `401` sem autenticação ou `403` sem autorização e não altera dados.

### História de Usuário 2 - Listar e pesquisar categorias (Prioridade: P1)

Como administrador, quero localizar categorias por nome, descrição ou slug e incluir categorias inativas na gestão; como visitante, quero continuar recebendo somente categorias ativas no catálogo.

**Por que esta prioridade**: Pesquisa e visibilidade de status permitem administrar catálogos maiores sem expor categorias inativas ao fluxo público.

**Teste independente**: Preparar categorias ativas/inativas com nomes, descrições e slugs diferentes; verificar busca parcial sem distinção de maiúsculas, resultados públicos ativos e resultados administrativos quando a opção de incluir inativas for autorizada.

**Cenários de Aceitação**:

1. **Dado** categorias ativas, **quando** visitante ou cliente consultar `GET /api/categories`, **então** recebe somente categorias ativas, na ordenação já usada pelo catálogo.
2. **Dado** um termo de busca, **quando** consultar categorias, **então** a busca parcial, sem distinção entre maiúsculas/minúsculas, corresponde a `name`, `description` ou `slug` e preserva a regra de visibilidade do chamador.
3. **Dado** um administrador autenticado, **quando** listar com `includeInactive=true` ou consultar ID com essa opção, **então** pode localizar categorias ativas e inativas.
4. **Dado** um usuário não autenticado ou sem role ADMIN, **quando** solicitar `includeInactive=true`, **então** recebe `401` ou `403`, respectivamente, sem dados de categorias inativas.
5. **Dado** uma busca válida sem correspondências, **quando** a consulta terminar, **então** recebe sucesso com lista vazia, não erro `404`.
6. **Dado** um UUID inexistente ou inativo consultado sem privilégio/indicação administrativa, **quando** solicitar `GET /api/categories/{id}`, **então** recebe `404` sem revelar dados da categoria inativa.

### História de Usuário 3 - Editar, ativar e desativar categorias (Prioridade: P1)

Como administrador, quero manter os dados das categorias e controlar sua disponibilidade no catálogo sem apagar permanentemente os registros.

**Por que esta prioridade**: A manutenção do nome, slug e status preserva a integridade das referências e permite corrigir ou recuperar categorias existentes.

**Teste independente**: Atualizar nome/descrição/ordem/slug, verificar unicidade e auditoria, desativar e confirmar que a categoria sai da listagem pública; reativar e confirmar que retorna ao catálogo.

**Cenários de Aceitação**:

1. **Dado** uma categoria existente e um administrador, **quando** atualizar seus campos permitidos, **então** recebe `200` com os dados atualizados e `updatedAt` atualizado.
2. **Dado** uma atualização sem slug, **quando** o nome mudar, **então** o slug existente é preservado para não alterar a referência de URL silenciosamente.
3. **Dado** uma atualização com nome ou slug pertencente a outra categoria, **quando** salvar, **então** recebe `409` e a categoria permanece inalterada.
4. **Dado** uma categoria ativa sem produtos vinculados, **quando** o administrador chamar `DELETE /api/categories/{id}`, **então** a categoria é desativada logicamente e retorna `204`, sem remover a linha ou suas referências.
5. **Dado** uma categoria inativa, **quando** o administrador solicitar reativação, **então** a categoria volta a `isActive=true`, retorna `200` e volta a aparecer na listagem pública.
6. **Dado** uma categoria que ainda possui produtos vinculados, **quando** o administrador tentar desativá-la, **então** recebe `409 Conflict`, preservando a regra existente e os vínculos.
7. **Dado** um identificador inexistente, **quando** o administrador editar, ativar ou desativar, **então** recebe `404 Not Found` sem criar ou alterar categoria.
8. **Dado** um chamador sem role ADMIN, **quando** tentar editar, ativar ou desativar, **então** recebe `401` ou `403` e nenhum campo é alterado.

## Casos de Borda

- Categorias inativas continuam ocupando seus nomes e slugs; não podem ser reutilizados por uma categoria diferente.
- A unicidade de nome permanece global e mantém a comparação existente pelo valor persistido; esta feature não introduz normalização de caixa ou espaços para nomes.
- O slug é canônico, minúsculo, limitado a letras ASCII, números e hífens simples, sem hífen no início/fim ou hífens consecutivos. A normalização remove diacríticos e converte sequências de separadores em hífen.
- Se dois nomes gerarem o mesmo slug normalizado, a criação recebe `409` quando não houver slug alternativo informado; não é atribuído sufixo silencioso.
- Na migration dos dados existentes, colisão entre slugs derivados interrompe a migração sem escrever os slugs conflitantes nem instalar a constraint única. O diagnóstico informa `categoryId`, `name` e slug candidato de cada registro. A retomada exige um mapa explícito `categoryId -> slug` para todos os conflitos; cada slug informado deve seguir o formato canônico e ser único globalmente, inclusive contra categorias inativas e slugs não conflitantes já derivados. Nenhum nome, ID, parentCategoryId ou vínculo com produto é alterado para resolver colisões.
- No update, slug omitido significa preservar o valor anterior, não recalcular a partir do novo nome.
- Descrição ausente permanece opcional; limites existentes de tamanho continuam aplicáveis.
- Categoria pai inexistente ou inativa na criação recebe `404`; auto-parenting inválido recebe `400`.
- Busca vazia ou composta somente de espaços é tratada como ausência de filtro.
- A consulta pública nunca inclui registros inativos, mesmo quando existe termo de pesquisa.
- Desativar categoria que contém produtos permanece bloqueado por `409`, conforme comportamento do Product Service existente.
- A migration de slug não pode remover categorias, alterar IDs ou quebrar relações pai/filho e `products.category_id`; slugs sem colisão são preenchidos a partir dos nomes, enquanto conflitos exigem o mapeamento explícito descrito acima antes da conclusão da migration.

## Requisitos

### Requisitos Funcionais

- **FR-001**: O Product Service DEVE continuar sendo a autoridade única para categorias; a feature NÃO DEVE criar outro serviço, outra tabela de categorias ou outro conjunto duplicado de endpoints CRUD.
- **FR-002**: A feature DEVE completar o recurso existente `/api/categories` e manter compatibilidade dos campos atuais de categoria, acrescentando o campo `slug` às respostas e contratos de criação/edição.
- **FR-003**: Um administrador autenticado DEVE poder criar categorias com `name` obrigatório, `description` opcional, `parentCategoryId` opcional e `slug` opcional.
- **FR-004**: Quando `slug` for omitido na criação, o serviço DEVE gerar um slug normalizado a partir do nome. Quando informado, DEVE normalizá-lo para a forma canônica. Slug vazio após normalização ou acima do limite de 120 caracteres DEVE ser rejeitado com `400`.
- **FR-005**: Cada categoria DEVE possuir slug globalmente único entre categorias ativas e inativas. Conflito de slug DEVE resultar em `409` e não pode deixar gravação parcial.
- **FR-006**: Nome de categoria DEVE permanecer globalmente único segundo a comparação pelo valor persistido já usada pelo serviço e pela constraint existente; duplicidade DEVE resultar em `409`, sem alterar a normalização atual do nome.
- **FR-007**: Nome DEVE continuar limitado a 1–100 caracteres não brancos e descrição a no máximo 500 caracteres, preservando as validações existentes.
- **FR-008**: A listagem pública `GET /api/categories` DEVE continuar retornando somente categorias ativas e DEVE aceitar busca opcional por trecho de nome, descrição ou slug, sem distinção entre maiúsculas/minúsculas.
- **FR-009**: A consulta de administração DEVE permitir incluir categorias inativas por `includeInactive=true`; essa opção DEVE exigir role ADMIN e nunca expor registros inativos a visitante ou usuário comum.
- **FR-010**: A consulta individual por ID DEVE continuar pública para categorias ativas. Categoria inativa só pode ser retornada quando a consulta administrativa solicitar explicitamente `includeInactive=true`.
- **FR-011**: Administradores DEVEM poder editar os campos já suportados (nome, descrição e ordem de exibição) e informar slug opcional. Slug omitido em update DEVE ser preservado.
- **FR-012**: A operação existente `DELETE /api/categories/{id}` DEVE continuar sendo soft delete/desativação e responder `204` em sucesso; a linha e suas referências permanecem no banco.
- **FR-013**: A feature DEVE expor operação REST autenticada ADMIN para reativar categoria inativa. A reativação DEVE preservar UUID, slug e relações existentes e retornar a categoria atualizada.
- **FR-014**: Requisições administrativas sem JWT válido DEVEM retornar `401`; JWT válido sem role ADMIN DEVE retornar `403`; nenhum dado pode ser alterado nesses casos.
- **FR-015**: Erros de validação devem retornar `400`, categoria não encontrada `404` e conflito de nome, slug ou regra de desativação `409`, no ErrorResponse comum do Product Service.
- **FR-016**: Nome, slug, descrição e status devem permanecer persistidos na tabela `categories`; a evolução do banco DEVE usar migration aditiva, preencher slugs existentes e preservar IDs, hierarquia e vínculos com produtos. Slugs derivados sem colisão podem ser preenchidos a partir do nome. Se houver colisão após normalização, a migration DEVE abortar antes de gravar os slugs conflitantes ou instalar a unicidade, reportar `categoryId`, nome e slug candidato, e exigir mapa explícito `categoryId -> slug` para cada conflito antes de ser reaplicada; é proibido adicionar sufixos automaticamente.
- **FR-017**: O contrato OpenAPI existente do Product Service DEVE ser atualizado com slug, parâmetros de pesquisa/visibilidade administrativa, reativação, validações e respostas de erro. Não criar contrato concorrente.
- **FR-018**: A feature DEVE acrescentar testes unitários e de integração para slug, unicidade, pesquisa, autorização, status ativo/inativo, migration, preservação dos vínculos e contrato documentado.
- **FR-019**: Esta feature NÃO DEVE implementar cadastro, busca ou atualização de produtos, checkout, pedidos, pagamento ou estoque.

### Contrato REST Existente e Evoluções

As operações existentes permanecem em `/api/categories`:

| Método e rota | Acesso | Comportamento da feature |
|---|---|---|
| `GET /api/categories?search={termo}` | Público | Lista somente categorias ativas; `search` é opcional. |
| `GET /api/categories?search={termo}&includeInactive=true` | ADMIN | Lista ativas e inativas para gestão. |
| `GET /api/categories/{id}` | Público | Retorna categoria ativa; inativa ou inexistente resulta em `404`. |
| `GET /api/categories/{id}?includeInactive=true` | ADMIN | Permite consultar categoria ativa ou inativa. |
| `POST /api/categories` | ADMIN | Cria categoria e slug; retorna `201`. |
| `PUT /api/categories/{id}` | ADMIN | Atualiza campos existentes e slug opcional; retorna `200`. |
| `DELETE /api/categories/{id}` | ADMIN | Desativa logicamente; retorna `204` ou `409` se houver produtos vinculados. |
| `PATCH /api/categories/{id}/activation` | ADMIN, sem body | Reativa categoria inativa; retorna `200` com categoria atualizada. |

A resposta de categoria mantém os campos existentes (`id`, `name`, `description`, `parentCategoryId`, `displayOrder`, `isActive`, `createdAt`, `updatedAt`) e passa a incluir `slug`. Requests não aceitam identidade do ator: auditoria usa o principal ADMIN autenticado existente.

### Entidades Principais

- **Categoria**: classificação hierárquica do catálogo, mantida pelo Product Service, ativa ou inativa, com identidade estável, nome único, slug único, descrição opcional, categoria pai opcional e ordem de exibição.
- **Slug de categoria**: identificador textual canônico e único, derivado do nome na criação quando não informado, editável por ADMIN e reservado mesmo quando a categoria está inativa.
- **Referência de produto à categoria**: relação já existente entre `products.category_id` e `categories.id`; permanece íntegra e não é alterada por esta feature.

## Critérios de Sucesso

### Resultados Mensuráveis

- **SC-001**: 100% das categorias existentes após a migration possuem slug não vazio, canônico e globalmente único sem alteração de IDs ou perda de vínculos; colisões interrompem a migration com diagnóstico e só são resolvidas por mapa explícito, nunca por sufixo automático.
- **SC-002**: Em testes de criação e edição, nenhum par de categorias compartilha o mesmo nome segundo a regra de igualdade persistida existente ou o mesmo slug, inclusive entre registros ativos e inativos.
- **SC-003**: A listagem pública retorna somente categorias ativas; 100% das consultas de categorias inativas sem ADMIN são rejeitadas sem revelar dados.
- **SC-004**: Administradores conseguem completar criar, listar/pesquisar, consultar, editar, desativar e reativar uma categoria por meio das operações documentadas.
- **SC-005**: Nome/slug inválidos e conflitos retornam `400`/`409` consistentes e deixam o estado persistido inalterado.
- **SC-006**: Desativação de categoria vinculada a produtos mantém o comportamento existente de `409`; nenhum vínculo de produto é apagado ou redirecionado.
- **SC-007**: OpenAPI descreve todos os campos, parâmetros, permissões e respostas usados nas operações administrativas e públicas.

## Premissas

- Categorias continuam no Product Service e no database/schema já administrado por ele; a migration adiciona slug à tabela existente.
- JWT ADMIN é emitido e validado conforme o mecanismo de autenticação existente no TechStore.
- O API Gateway já encaminha `/api/categories/**` para o Product Service; seu roteamento permanece inalterado.
- A leitura pública do catálogo continua filtrando categorias inativas; `includeInactive=true` é exclusivamente administrativo.
- Slugs são recursos estáveis: ao alterar nome sem fornecer slug, o slug atual permanece; um administrador pode alterá-lo explicitamente.
- Slug omitido na criação é gerado do nome por normalização canônica; colisão exige slug alternativo, sem sufixo automático.
- A migration gera slug do nome somente para linhas sem colisão. Para colisões históricas, a implantação fornece mapa aprovado por ID; mapa incompleto, inválido ou não único mantém a migration bloqueada com diagnóstico.
- A regra existente que impede desativar categoria com produtos vinculados continua vigente nesta feature.
- O escopo é backend/API/schema/documentação/testes; não inclui interface administrativa frontend.

## Fora do Escopo

- Criar outro microsserviço, database, tabela de categorias ou CRUD paralelo.
- Cadastro, edição, desativação ou busca de produtos; somente preservar referências existentes.
- Alterar checkout, pedidos, pagamentos, estoque, carrinho ou autenticação/emissão de JWT.
- Interface administrativa frontend para categorias.
- Remoção física de categorias ou alteração automática de produtos quando uma categoria muda de estado.
- Reparenting/hierarquia editável após a criação; relações pai existentes e validações atuais são preservadas.
