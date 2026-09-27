# Modelo de Dados: Catálogo de Produtos - Fundação

**Data**: 2026-09-26 | **Estágio**: Phase 1 (Design)

## Visão Geral

Este documento define o modelo de domínio, schema de banco de dados e estratégia de persistência para o Catálogo de Produtos Fundacional.

O Product Service gerencia:
- **Produtos**: SKUs no catálogo com detalhes, preço e disponibilidade
- **Categorias**: Organização hierárquica de produtos
- **Imagens de Produtos**: Referências baseadas em URL a imagens armazenadas externamente

Todos os dados são armazenados em banco PostgreSQL propriedade exclusiva do Product Service.

---

## Modelo de Domínio

### 1. Entidade Product

**Responsabilidade**: Representa um produto no catálogo TechStore.

**Campos de Domínio**:

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-----------|-----------|
| `id` | UUID | Sim | Chave primária (gerada pelo banco) |
| `name` | String | Sim | Nome do produto (máx 255 caracteres), único dentro de categoria |
| `description` | String | Não | Descrição completa (máx 5000 caracteres) |
| `price` | BigDecimal | Sim | Preço atual em moeda padrão (>= 0) |
| `cost` | BigDecimal | Não | Custo interno (para cálculos de margem) |
| `brand` | String | Não | Nome da marca (máx 100 caracteres) |
| `sku` | String | Sim | Stock Keeping Unit (máx 50 caracteres), globalmente único |
| `categoryId` | UUID | Sim | Chave estrangeira para Category |
| `quantity` | Integer | Sim | Quantidade atual em estoque (>= 0) |
| `imageUrl` | String | Não | URL da imagem primária (máx 2000 caracteres) |
| `isActive` | Boolean | Sim | Flag de soft-delete (padrão: true) |
| `createdAt` | LocalDateTime | Sim | Timestamp de criação (UTC) |
| `updatedAt` | LocalDateTime | Sim | Timestamp de última atualização (UTC) |
| `createdBy` | String | Sim | ID do usuário que criou (máx 100 caracteres) |
| `updatedBy` | String | Sim | ID do usuário que atualizou por último (máx 100 caracteres) |

**Restrições**:
- Chave Primária: `id`
- Chaves Únicas: `sku` (globalmente), `(name, categoryId)` (dentro de categoria)
- Chave Estrangeira: `categoryId` → `categories(id)`
- Checks: `price >= 0`, `quantity >= 0`

**Regras de Negócio**:
- Um produto pertence a exatamente uma categoria
- SKU deve ser globalmente único
- Preço não pode ser negativo
- Produtos podem ser desativados via `isActive = false` (soft delete)

---

### 2. Entidade Category

**Responsabilidade**: Representa uma categoria de produtos para organizar o catálogo.

**Campos de Domínio**:

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-----------|-----------|
| `id` | UUID | Sim | Chave primária (gerada pelo banco) |
| `name` | String | Sim | Nome da categoria (máx 100 caracteres), deve ser único |
| `description` | String | Não | Descrição da categoria (máx 500 caracteres) |
| `parentCategoryId` | UUID | Não | Chave estrangeira para Category pai (para hierarquia) |
| `displayOrder` | Integer | Sim | Ordem de sort dentro do pai (padrão: 0) |
| `isActive` | Boolean | Sim | Flag de soft-delete (padrão: true) |
| `createdAt` | LocalDateTime | Sim | Timestamp de criação (UTC) |
| `updatedAt` | LocalDateTime | Sim | Timestamp de última atualização (UTC) |
| `createdBy` | String | Sim | ID do usuário que criou (máx 100 caracteres) |
| `updatedBy` | String | Sim | ID do usuário que atualizou por último (máx 100 caracteres) |

**Restrições**:
- Chave Primária: `id`
- Chave Única: `name`
- Chave Estrangeira: `parentCategoryId` → `categories(id)` (auto-referencial, nullable)

**Regras de Negócio**:
- Categorias podem ter estrutura hierárquica
- Categorias raiz têm `parentCategoryId = NULL`
- Nomes de categorias são globalmente únicos
- Categorias podem ser desativadas via `isActive = false`

---

## Schema do Banco de Dados

### DDL: Comandos CREATE TABLE

```sql
-- Tabela categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    parent_category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT chk_category_name_not_empty CHECK (name IS NOT NULL AND length(name) > 0)
);

-- Índices para categories
CREATE INDEX idx_categories_parent_category_id 
    ON categories(parent_category_id) WHERE parent_category_id IS NOT NULL;
CREATE INDEX idx_categories_name_active ON categories(name) WHERE is_active = true;

-- Tabela products
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    cost NUMERIC(12, 2),
    brand VARCHAR(100),
    sku VARCHAR(50) NOT NULL UNIQUE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL DEFAULT 0,
    image_url VARCHAR(2000),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT chk_product_name_not_empty CHECK (name IS NOT NULL AND length(name) > 0),
    CONSTRAINT chk_product_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_product_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT uk_product_name_category UNIQUE (name, category_id)
);

-- Índices para full-text search
ALTER TABLE products ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('english', COALESCE(name, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(brand, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'C')
) STORED;

CREATE INDEX idx_products_search_fts ON products USING GIN(search_vector);

-- Outros índices para performance
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_sku ON products(sku) WHERE is_active = true;
CREATE INDEX idx_products_active ON products(is_active, created_at DESC);
CREATE INDEX idx_products_price ON products(price ASC) WHERE is_active = true;
CREATE INDEX idx_products_created_at ON products(created_at DESC) WHERE is_active = true;
```

---

## Relacionamentos

### Category para Product (1:N)

```
Category (1) ───────────── (*) Product
```

- Uma categoria tem zero ou muitos produtos
- Um produto pertence a exatamente uma categoria
- Comportamento em cascata: ON DELETE RESTRICT
- Soft deletes: Desativar categoria não afeta products

### Category para Category (Hierarquia)

```
Category (1) ───────────── (*) Category
   (pai)                      (filho)
```

- Uma categoria pode ter zero ou um pai
- Uma categoria pode ter zero ou muitos filhos
- Comportamento em cascata: ON DELETE SET NULL

---

## Auditoria e Rastreamento Temporal

### Campos de Auditoria

Toda entidade de domínio inclui campos de auditoria para rastrear criação e modificação:

| Campo | Propósito | Gerenciamento |
|-------|-----------|--------|
| `createdAt` | Timestamp de criação | Definido por callback JPA `@PrePersist` |
| `updatedAt` | Timestamp de modificação | Atualizado por callback JPA `@PreUpdate` |
| `createdBy` | ID do usuário que criou | Definido por `@PrePersist`, extraído de JWT |
| `updatedBy` | ID do usuário que atualizou | Atualizado por `@PreUpdate`, extraído de JWT |

---

## Capacidades de Busca e Filtragem

### Full-Text Search

**Campos Suportados**:
- Nome do produto (peso A = 3x)
- Marca (peso B = 2x)
- Descrição (peso C = 1x)

### Opções de Filtragem

| Filtro | Parâmetro | Tipo | Exemplo |
|--------|-----------|------|---------|
| Categoria | `categoryId` | UUID | `categoryId=550e8400...` |
| Faixa de Preço | `minPrice`, `maxPrice` | BigDecimal | `minPrice=100.00&maxPrice=500.00` |
| Disponibilidade | `inStock` | Boolean | `inStock=true` |
| Marca | `brand` | String | `brand=Samsung` |

### Opções de Ordenação

| Chave | Campo | Direção |
|-------|-------|---------|
| `relevance` | `ts_rank(search_vector)` | DESC (padrão para busca) |
| `price` | `price` | ASC ou DESC |
| `name` | `name` | ASC ou DESC |
| `newest` | `created_at` | DESC |

### Paginação

- Tamanho de página: 20 itens (padrão, configurável)
- Parâmetros: `page` (0-based), `pageSize` (1-100)
- Resposta inclui: `totalElements`, `totalPages`, `hasMore`

---

## Estratégia de Soft Delete

### Justificativa

Soft deletes (deleção lógica via `is_active = false`) são usados para:
- Preservar audit trail e dados históricos
- Permitir recuperação de dados se necessário
- Suportar conformidade e requisitos regulatórios
- Manter integridade referencial

### Implementação

**Comportamento padrão de Query**:
- Todas as queries excluem `is_active = false` por padrão
- Uso de anotação JPA `@Where(clause = "is_active = true")`

---

## Dados de Exemplo

### Inserir Categorias

```sql
INSERT INTO categories (name, description, created_by, updated_by)
VALUES
  ('Eletrônicos', 'Dispositivos eletrônicos e gadgets', 'system', 'system'),
  ('Computadores', 'Laptops, desktops, tablets', 'system', 'system');
```

### Inserir Produtos de Exemplo

```sql
INSERT INTO products (name, description, price, brand, sku, category_id, quantity, image_url, created_by, updated_by)
VALUES
  ('MacBook Pro 16', 'Laptop de alta performance', 2499.99, 'Apple', 'SKU-001', 
   (SELECT id FROM categories WHERE name = 'Computadores'), 50, 
   'https://example.com/macbook.jpg', 'admin', 'admin');
```

---

## Considerações de Desempenho

### Estimativas de Tamanho de Banco

Com 10.000 produtos e 50 categorias:
- Tabela categories: ~50 linhas, ~20 KB
- Tabela products: ~10.000 linhas, ~5-10 MB
- Índice FTS: ~2-5 MB
- **Total**: ~10-15 MB (muito gerenciável)

### Índices para Otimização

| Índice | Tabela | Colunas | Propósito |
|--------|--------|---------|----------|
| `idx_products_search_fts` | products | `search_vector` (GIN) | Full-text search otimizado |
| `idx_categories_parent_category_id` | categories | `parent_category_id` | Navegação de hierarquia |
| `idx_products_category_id` | products | `category_id` | Queries de categoria |
| `idx_products_sku` | products | `sku` | Lookups por SKU |
| `idx_products_active_created_at` | products | `is_active, created_at DESC` | Listagem de produtos recentes |
| `idx_products_price_active` | products | `price, is_active` | Filtragem por preço |

---

## Conclusão

O modelo de dados do Catálogo de Produtos Fundacional é limpo, normalizado e otimizado para:
- Queries read-heavy (navegação de catálogo)
- Operações write (administração de produtos)
- Busca full-text em múltiplos campos
- Auditoria completa de mudanças
- Escalabilidade a 10k+ produtos
