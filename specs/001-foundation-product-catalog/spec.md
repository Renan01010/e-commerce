# Especificação: Catálogo de Produtos - Fundação

**Data**: 2026-09-26 | **Versão**: 1.0 | **Status**: APROVADO

**Referência de Feature**: `001-foundation-product-catalog`

**Branch**: `001-foundation-product-catalog`

---

## Visão Geral da Feature

O Catálogo de Produtos Fundacional é o primeiro incremento do TechStore. Fornece aos clientes a capacidade de navegar, pesquisar e visualizar um catálogo de produtos de comércio eletrônico, com suporte administrativo para gerenciamento de produtos e categorias.

### Escopo

**Incluído**:
- API REST de Serviço de Produtos (CRUD)
- Busca full-text em produtos
- Navegação por categorias
- Hierarquia de categorias
- Autenticação e autorização baseadas em JWT
- Interface de catálogo do lado do cliente (React)
- API Gateway unificado
- Documentação OpenAPI
- Testes abrangentes

**Excluído** (Phase 3+):
- Funcionalidade de carrinho de compras (será integrada em incremento futuro)
- Processamento de pedidos
- Pagamento
- Faturamento
- Gerenciamento de estoque em tempo real

---

## Histórias de Usuário

### US-001 (P1): Listar Produtos do Catálogo

**Prioridade**: P1 (Crítico)

**Como** um cliente,
**Quero** visualizar uma lista paginada de produtos com detalhes básicos,
**Para que** eu possa navegar e explorar o catálogo disponível.

**Critérios de Aceitação**:
- [ ] Endpoint GET /api/products retorna lista de produtos (20 por página por padrão)
- [ ] Cada produto mostra: ID, nome, preço, marca, imagem, descrição resumida
- [ ] Resposta inclui paginação: página atual, total de elementos, total de páginas, hasMore
- [ ] Produtos inativos (isActive=false) não são retornados
- [ ] Requisição sem autenticação é aceita (acesso público)

**Notas Técnicas**:
- Implementar em Product Service
- Usar Spring Data JPA com Pageable
- Adicionar índice no campo isActive

---

### US-002 (P1): Buscar Produtos por Termo

**Prioridade**: P1 (Crítico)

**Como** um cliente,
**Quero** pesquisar produtos por nome, marca ou descrição,
**Para que** eu possa encontrar rapidamente o que procuro.

**Critérios de Aceitação**:
- [ ] Parâmetro query ?query=termo realiza busca full-text
- [ ] Busca é sensível a relevância (nome > marca > descrição)
- [ ] Resultados retornados ordenados por relevância por padrão
- [ ] Busca é case-insensitive
- [ ] Tempo de resposta < 300ms p95 para queries normais

**Notas Técnicas**:
- Usar PostgreSQL tsvector com GIN index
- Implementar mapeamento de pesos: nome (3x), marca (2x), descrição (1x)
- Adicionar índice idx_products_search_fts

---

### US-003 (P1): Filtrar Produtos por Categoria

**Prioridade**: P1 (Crítico)

**Como** um cliente,
**Quero** filtrar produtos por categoria,
**Para que** eu possa navegar categorias relacionadas.

**Critérios de Aceitação**:
- [ ] Parâmetro ?categoryId=uuid filtra por categoria específica
- [ ] Listar todas as categorias via GET /api/categories
- [ ] Cada categoria mostra ID, nome, descrição, parent (se houver)
- [ ] Categorias com products não podem ser deletadas (ON DELETE RESTRICT)
- [ ] Navegação hierárquica de categorias funciona

**Notas Técnicas**:
- Criar índice em categoryId (foreign key)
- Implementar índice em parent_category_id
- Usar Soft delete para categorias (isActive flag)

---

### US-004 (P1): Filtrar Produtos por Atributos

**Prioridade**: P1 (Crítico)

**Como** um cliente,
**Quero** filtrar produtos por preço, marca e disponibilidade,
**Para que** eu possa refinar a busca.

**Critérios de Aceitação**:
- [ ] Parâmetro ?minPrice=x&maxPrice=y filtra por faixa de preço
- [ ] Parâmetro ?brand=xyz filtra por marca específica
- [ ] Parâmetro ?inStock=true filtra apenas produtos com quantidade > 0
- [ ] Múltiplos filtros são combinados com AND
- [ ] Campos de filtro são opcionais (padrão: sem filtros)

**Notas Técnicas**:
- Adicionar índice em price (com filtro isActive=true)
- Adicionar índice em quantity
- Usar Spring Specification ou CriteriaBuilder para queries dinâmicas

---

### US-005 (P1): Ordenar Resultados

**Prioridade**: P1 (Crítico)

**Como** um cliente,
**Quero** ordenar produtos por relevância, preço, nome ou data,
**Para que** eu possa ver resultados na ordem que me interessa.

**Critérios de Aceitação**:
- [ ] Parâmetro ?sortBy=relevance|price|name|newest
- [ ] Parâmetro ?sortOrder=asc|desc
- [ ] Padrão é sortBy=relevance, sortOrder=desc (para buscas)
- [ ] Ordenação funciona com paginação
- [ ] Índices de banco otimizam cada estratégia de sort

**Notas Técnicas**:
- Criar índice em createdAt DESC (para newest)
- Criar índice em price ASC (para ordenação de preço)
- Usar Spring Data Sort

---

### US-006 (P1): Visualizar Detalhes de Produto

**Prioridade**: P1 (Crítico)

**Como** um cliente,
**Quero** ver todos os detalhes de um produto específico,
**Para que** eu possa tomar uma decisão de compra informada.

**Critérios de Aceitação**:
- [ ] Endpoint GET /api/products/{id} retorna todos os campos do produto
- [ ] Inclui: ID, nome, descrição completa, preço, custo (se admin), brand, SKU, quantidade, imagem, datas
- [ ] Produto inativo retorna 404
- [ ] Acesso sem autenticação é permitido

**Notas Técnicas**:
- Mapear Product entity para ProductDTO
- Usar MapStruct para mapeamento
- Retornar 404 se não encontrado ou inativo

---

### US-007 (P2): Criar Produtos Administrativos

**Prioridade**: P2 (Alto)

**Como** um administrador,
**Quero** criar novos produtos no catálogo,
**Para que** eu possa adicionar novos itens à venda.

**Critérios de Aceitação**:
- [ ] Endpoint POST /api/products requer autenticação (token JWT) e role ADMIN
- [ ] Campos obrigatórios: name, price, sku, categoryId, quantity
- [ ] SKU deve ser globalmente único (constraint unique)
- [ ] Nome+categoria deve ser único (constraint unique)
- [ ] Preço e quantidade devem ser >= 0
- [ ] Produto é criado com isActive=true por padrão
- [ ] Retorna 201 Created com produto completo

**Notas Técnicas**:
- Validar com @Valid e Bean Validation
- Usar Spring Security @PreAuthorize
- Implementar callbacks JPA @PrePersist para createdBy, createdAt
- Retornar ProductDTO com todos os campos

---

### US-008 (P2): Atualizar Produtos Administrativos

**Prioridade**: P2 (Alto)

**Como** um administrador,
**Quero** atualizar informações de produtos existentes,
**Para que** eu possa corrigir dados ou refletir mudanças.

**Critérios de Aceitação**:
- [ ] Endpoint PUT /api/products/{id} requer autenticação (token JWT) e role ADMIN
- [ ] Todos os campos são atualizáveis (exceto ID, createdAt, createdBy)
- [ ] Validação igual a criação (preço >= 0, etc.)
- [ ] Nome+categoria deve ser único (excluindo produto atual)
- [ ] Retorna 200 OK com produto atualizado
- [ ] Campo updatedAt e updatedBy são atualizados automaticamente

**Notas Técnicas**:
- Usar callbacks JPA @PreUpdate para updatedAt, updatedBy
- Validar unicidade de SKU e (name, categoryId)
- Retornar 404 se produto não encontrado

---

### US-009 (P2): Deletar Produtos Administrativos

**Prioridade**: P2 (Alto)

**Como** um administrador,
**Quero** remover produtos do catálogo,
**Para que** eu possa limpar itens desatualizado ou descontinuados.

**Critérios de Aceitação**:
- [ ] Endpoint DELETE /api/products/{id} requer autenticação (token JWT) e role ADMIN
- [ ] Delete é soft-delete (marca isActive=false, não remove linhas)
- [ ] Produtos deletados não aparecem em listagens (exceto consultas específicas)
- [ ] Retorna 204 No Content em sucesso
- [ ] Produto já deletado retorna 404

**Notas Técnicas**:
- Usar anotação @Where(clause = "is_active = true") no JPA entity
- Não remover dados do banco (preserve audit trail)
- Retornar 404 se produto não encontrado ou já inativo

---

### US-010 (P2): Gerenciar Categorias Administrativas

**Prioridade**: P2 (Alto)

**Como** um administrador,
**Quero** criar, atualizar e deletar categorias,
**Para que** eu possa organizar o catálogo hierarquicamente.

**Critérios de Aceitação**:
- [ ] Endpoint POST /api/categories requer autenticação (token JWT) e role ADMIN
- [ ] Campos obrigatórios: name
- [ ] Nome deve ser globalmente único
- [ ] Pode ter categoria pai (parentCategoryId) para hierarquia
- [ ] Endpoint PUT /api/categories/{id} atualiza categoria
- [ ] Endpoint DELETE /api/categories/{id} soft-deleta categoria
- [ ] Categoria com produtos não pode ser deletada (retorna 409 Conflict)
- [ ] Categorias deletadas não aparecem em listagens

**Notas Técnicas**:
- Usar ON DELETE RESTRICT para manter integridade referencial
- Retornar 409 se tentar deletar categoria com produtos
- Implementar auto-referência para hierarquia (parentCategoryId)

---

### US-011 (P2): Autenticação e Autorização

**Prioridade**: P2 (Alto)

**Como** um operador do sistema,
**Quero** que operações administrativas exijam autenticação e autorização,
**Para que** apenas usuários autorizados possam modificar dados.

**Critérios de Aceitação**:
- [ ] Operações GET são acessíveis sem autenticação
- [ ] Operações POST/PUT/DELETE requerem Bearer token JWT válido
- [ ] Token inválido retorna 401 Unauthorized
- [ ] Token válido mas sem role ADMIN retorna 403 Forbidden
- [ ] Token inclui claims: sub (user ID), roles, iat, exp
- [ ] Expiração padrão: 24 horas

**Notas Técnicas**:
- Usar Spring Security com JWT filter
- Configurar @EnableWebSecurity
- Usar @PreAuthorize("hasRole('ADMIN')")
- Ler JWT secret de variável de ambiente

---

### US-012 (P2): Auditar Mudanças de Dados

**Prioridade**: P2 (Alto)

**Como** um auditor,
**Quero** rastrear quem criou e atualizou cada entidade,
**Para que** eu tenha visibilidade completa das mudanças.

**Critérios de Aceitação**:
- [ ] Cada produto tem createdAt, updatedAt, createdBy, updatedBy
- [ ] Cada categoria tem os mesmos campos de auditoria
- [ ] Timestamps são em UTC (LocalDateTime)
- [ ] User IDs são extraídos do JWT
- [ ] Campos de auditoria são preenchidos automaticamente (não permitem POST/PUT)

**Notas Técnicas**:
- Usar callbacks JPA @PrePersist e @PreUpdate
- Extrair user ID do SecurityContext
- Usar LocalDateTime em UTC

---

### US-013 (P3): Observabilidade e Métricas

**Prioridade**: P3 (Médio)

**Como** um operador,
**Quero** monitorar saúde e performance do serviço,
**Para que** eu possa diagnosticar problemas rapidamente.

**Critérios de Aceitação**:
- [ ] Endpoint /actuator/health retorna status de serviço
- [ ] Endpoint /actuator/metrics fornece métricas (requisições, tempo de resposta)
- [ ] Logs são estruturados em JSON com correlation IDs
- [ ] Erros incluem correlation ID para rastreamento

**Notas Técnicas**:
- Usar Spring Boot Actuator
- Adicionar Spring Cloud Sleuth para correlation IDs
- Configurar Jackson para logs JSON

---

## Testes Requeridos

### Cobertura Esperada

- **Backend**: Mínimo 80% cobertura de testes unitários
- **Integração**: Todos os endpoints testados com Testcontainers
- **Frontend**: Testes de componentes com React Testing Library

### Estratégia de Testes

1. **Unit Tests**: Regras de negócio, validators, serviços
2. **Integration Tests**: Endpoints com banco PostgreSQL real (Testcontainers)
3. **Component Tests**: Componentes React com React Testing Library
4. **Contract Tests**: Conformidade com OpenAPI (optional)

### Suites de Testes Requeridas

- ProductRepositoryTest (JUnit 5 + Testcontainers)
- ProductServiceTest (JUnit 5 + Mockito)
- ProductControllerTest (JUnit 5 + MockMvc)
- CategoryRepositoryTest (JUnit 5 + Testcontainers)
- CategoryServiceTest (JUnit 5 + Mockito)
- CategoryControllerTest (JUnit 5 + MockMvc)
- CatalogPageTest (React Testing Library + Vitest)
- ProductDetailTest (React Testing Library + Vitest)

---

## Critérios de Aceitação de Feature

A feature será considerada COMPLETA quando:

1. ✅ Todos os endpoints especificados na OpenAPI estão implementados
2. ✅ Todos os testes especificados acima passam
3. ✅ Cobertura de testes >= 80% (backend)
4. ✅ Interface do cliente (catálogo + busca) funciona conforme especificado
5. ✅ API Gateway roteia corretamente para Product Service
6. ✅ Autenticação e autorização funcionam conforme especificado
7. ✅ Observabilidade (logs, métricas) funciona
8. ✅ Docker Compose local funciona conforme `quickstart.md`
9. ✅ Constituição TechStore é respeitada (10/10 princípios)
10. ✅ Nenhuma violação de segurança (secrets não em repo, etc.)

---

## Mapeamento para Tarefas

As histórias de usuário acima se mapeiam para as seguintes fases de tarefas:

| História | Fase | Status |
|----------|------|--------|
| US-001 até US-006 | Phase 2.1 (Backend Leitura) | Bloqueada por setup |
| US-007 até US-010 | Phase 2.2 (Backend Escrita + Autenticação) | Bloqueada por Phase 2.1 |
| US-011 | Phase 2.3 (Segurança) | Bloqueada por Phase 2.2 |
| US-012 | Phase 2.4 (Auditoria) | Bloqueada por Phase 2.3 |
| US-013 | Phase 2.5 (Observabilidade) | Bloqueada por setup |
| Frontend | Phase 3 (Frontend) | Bloqueada por Phase 2.1 |

---

## Dependências Externas

- PostgreSQL 15+ (fornecido via Docker Compose)
- Java 21+ (deve estar instalado)
- Node.js 20 LTS (deve estar instalado)
- Docker & Docker Compose (deve estar instalado)

---

## Cronograma Estimado

- **Setup**: 1-2 dias
- **Phase 2.1 (Backend Leitura)**: 3-4 dias
- **Phase 2.2 (Backend Escrita)**: 2-3 dias
- **Phase 2.3 (Segurança)**: 1-2 dias
- **Phase 2.4 (Auditoria)**: 1 dia
- **Phase 2.5 (Observabilidade)**: 1 dia
- **Phase 3 (Frontend)**: 4-5 dias
- **Testes e Integração**: 2-3 dias
- **Total Estimado**: 2-3 semanas

---

## Notas

- Spec Kit workflow segue: Specify → Clarify → Plan → Tasks → Analyze → Implement → Converge
- Este documento é a entrada para Phase 2 (Tasks)
- Não fazer implementação de source code nesta fase
- Gerar tasks.md baseado nesta especificação
