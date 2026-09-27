# Início Rápido: Catálogo de Produtos - Fundação

**Data**: 2026-09-26 | **Estágio**: Phase 1 (Design)

Este guia fornece instruções passo-a-passo para configurar o Catálogo de Produtos Fundacional localmente para desenvolvimento e testes.

---

## Pré-requisitos

Antes de começar, certifique-se de ter instalado:

- **Java 21+**: Download de [Oracle JDK](https://www.oracle.com/java/) ou [OpenJDK](https://adoptium.net/)
- **Maven 3.9+**: Download de [Apache Maven](https://maven.apache.org/)
- **Docker & Docker Compose**: [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- **Node.js 20 LTS & npm 10+**: Download de [Node.js](https://nodejs.org/)
- **Git**: Para clonar o repositório

**Verificar instalações**:
```bash
java -version          # Java 21+
mvn -version           # Maven 3.9+
docker --version       # Docker 20.10+
docker-compose --version  # Docker Compose 2.0+
node --version         # Node.js 20.x
npm --version          # npm 10.x
```

---

## Parte 1: Configuração do Ambiente de Desenvolvimento Local

### 1.1 Clonar Repositório

```bash
git clone https://github.com/seuorg/techstore.git
cd techstore
```

### 1.2 Criar Arquivos de Ambiente

Copie `.env.example` para `.env` na raiz do projeto e substitua as credenciais locais (NÃO fazer commit ao repositório):

```bash
# Banco de Dados
POSTGRES_DB=techstore_product_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=dev-password-change-in-production
POSTGRES_PORT=5432

# Product Service
PRODUCT_SERVICE_PORT=8081
PRODUCT_SERVICE_LOG_LEVEL=INFO

# API Gateway
API_GATEWAY_PORT=8080
API_GATEWAY_LOG_LEVEL=INFO

# JWT Configuration
TECHSTORE_JWT_SECRET=dev-secret-key-change-in-production
TECHSTORE_JWT_EXPIRATION_HOURS=24

# Frontend
VITE_API_URL=http://localhost:8080/api
VITE_API_TIMEOUT=30000
```

### 1.3 Iniciar Serviços Docker Compose

```bash
cd techstore

# Iniciar serviços em background
docker compose up -d --build

# Verificar que os serviços estão rodando
docker compose ps
```

**Saída esperada**:
```
NAME                COMMAND                  SERVICE      STATUS      PORTS
techstore-postgres-1   "docker-entrypoint..."   postgres     Up 2 min    0.0.0.0:5432->5432/tcp
```

---

## Parte 2: Setup Backend e Execução

### 2.1 Construir Backend

```bash
cd techstore/backend

# Construir todos os módulos backend
mvn clean install -DskipTests

# Deve mostrar: BUILD SUCCESS
```

### 2.2 Iniciar Product Service

**Opção A: Via Maven** (para desenvolvimento com auto-reload):

```bash
cd techstore/backend/product-service
mvn spring-boot:run
```

**Opção B: Via JAR** (após construção):

```bash
java -jar target/product-service-1.0.0.jar
```

**Verificar que o serviço está rodando**:
```bash
# Em outro terminal
curl http://localhost:8081/actuator/health
```

### 2.3 Iniciar API Gateway

**Em outro terminal**:

```bash
cd techstore/backend/api-gateway
mvn spring-boot:run
```

**Verificar gateway rodando**:
```bash
curl http://localhost:8080/actuator/health
```

### 2.4 Carregar Dados de Exemplo (Opcional)

```bash
# Conectar ao banco de dados
docker exec -it techstore-postgres-1 psql -U postgres -d techstore_product_db

# Inserir categorias e produtos (SQL do data-model.md)
INSERT INTO categories (name, description, created_by, updated_by)
VALUES ('Eletrônicos', 'Dispositivos eletrônicos', 'system', 'system');

INSERT INTO products (name, price, sku, category_id, quantity, created_by, updated_by)
VALUES ('MacBook Pro', 2499.99, 'SKU-001', 
        (SELECT id FROM categories WHERE name = 'Eletrônicos'), 50, 'admin', 'admin');

# Sair
\q
```

---

## Parte 3: Setup Frontend e Execução

### 3.1 Instalar Dependências Frontend

```bash
cd techstore/frontend

npm install
```

### 3.2 Iniciar Servidor de Desenvolvimento

```bash
npm run dev

# Saída:
#   VITE v5.x.x  ready in xxx ms
#   ➜  Local:   http://localhost:5173/
```

**Acessar frontend**: Abra [http://localhost:5173](http://localhost:5173) no navegador

### 3.3 Construir para Produção

```bash
npm run build

# Saída mostra:
# ✓ ... files built successfully
# dist/                    # Arquivos estáticos prontos para produção
```

---

## Parte 4: Executar Testes

### 4.1 Testes Unitários Backend

```bash
cd techstore/backend

# Executar todos os testes backend
mvn test
```

### 4.2 Testes de Integração Backend

```bash
cd techstore/backend/product-service

# Testcontainers spinará PostgreSQL automaticamente
mvn verify
```

### 4.3 Testes Unitários Frontend

```bash
cd techstore/frontend

# Executar todos os testes
npm run test

# Executar com cobertura
npm run test:coverage
```

---

## Parte 5: Testes de API

### 5.1 Listar Produtos (GET)

```bash
# Listar com paginação
curl -X GET "http://localhost:8080/api/products?page=0&pageSize=10"

# Buscar produtos
curl -X GET "http://localhost:8080/api/products?query=laptop"

# Filtrar por categoria
curl -X GET "http://localhost:8080/api/products?categoryId=550e8400..."

# Filtrar por faixa de preço
curl -X GET "http://localhost:8080/api/products?minPrice=100&maxPrice=3000"

# Ordenar por preço
curl -X GET "http://localhost:8080/api/products?sortBy=price&sortOrder=asc"
```

### 5.2 Obter Detalhes de Produto (GET)

```bash
curl -X GET "http://localhost:8080/api/products/650e8400..."
```

### 5.3 Listar Categorias (GET)

```bash
curl -X GET "http://localhost:8080/api/categories"
```

### 5.4 Criar Produto (POST) - Requer Token JWT

**Passo 1: Obter Token JWT**:

```bash
# Para desenvolvimento, use um token de teste
JWT_TOKEN="seu-token-jwt-aqui"
```

**Passo 2: Criar Produto**:

```bash
curl -X POST "http://localhost:8080/api/products" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dell XPS 15",
    "description": "Laptop premium",
    "price": 1999.99,
    "brand": "Dell",
    "sku": "SKU-XPS15-001",
    "categoryId": "550e8400...",
    "quantity": 25,
    "imageUrl": "https://example.com/dell-xps-15.jpg"
  }'
```

### 5.5 Atualizar Produto (PUT) - Requer Token JWT

```bash
curl -X PUT "http://localhost:8080/api/products/650e8400..." \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro 16 (2026)",
    "price": 2799.99,
    "quantity": 40
  }'
```

### 5.6 Desativar Produto (DELETE) - Requer Token JWT

```bash
curl -X DELETE "http://localhost:8080/api/products/650e8400..." \
  -H "Authorization: Bearer $JWT_TOKEN"

# Retorna 204 No Content em sucesso
```

### 5.7 Documentação OpenAPI

**Ver documentação via Swagger UI**:

```
http://localhost:8081/swagger-ui.html
```

**Ver OpenAPI JSON**:

```
http://localhost:8081/v3/api-docs
```

---

## Parte 6: Observabilidade e Monitoramento

### 6.1 Health Checks

**API Gateway**:
```bash
curl http://localhost:8080/actuator/health
```

**Product Service**:
```bash
curl http://localhost:8081/actuator/health
```

### 6.2 Métricas de Aplicação

```bash
# Ver todas as métricas disponíveis
curl http://localhost:8081/actuator/metrics

# Ver métrica específica (requisições HTTP)
curl http://localhost:8081/actuator/metrics/http.server.requests
```

---

## Parte 7: Comandos de Referência Úteis

### Gerenciamento de Banco de Dados

```bash
# Conectar ao PostgreSQL
docker exec -it techstore-postgres-1 psql -U postgres -d techstore_product_db

# Ver todas as tabelas
\dt

# Ver estrutura de tabela
\d products

# Contar produtos
SELECT COUNT(*) FROM products;

# Sair
\q

# Reset do banco (CUIDADO: Deleta todos os dados)
docker exec -it techstore-postgres-1 psql -U postgres \
  -c "DROP DATABASE IF EXISTS techstore_product_db; CREATE DATABASE techstore_product_db;"
```

### Gerenciamento de Docker

```bash
# Ver logs de um serviço
docker-compose logs -f postgres

# Reiniciar um serviço
docker-compose restart postgres

# Parar todos os serviços
docker-compose down

# Parar e remover volumes (AVISO: deleta dados)
docker-compose down -v
```

### Comandos Maven

```bash
# Limpeza e construção
mvn clean install

# Pular testes
mvn clean install -DskipTests

# Executar teste específico
mvn test -Dtest=ProductRepositoryTest

# Construir sem testes
mvn clean package -DskipTests
```

### Comandos npm

```bash
# Instalar dependências
npm install

# Iniciar servidor de desenvolvimento
npm run dev

# Construir para produção
npm run build

# Executar testes
npm test

# Executar testes com cobertura
npm run test:coverage

# Formatar código
npm run format

# Lint
npm run lint
```

---

## Parte 8: Troubleshooting

### Backend não inicia

**Erro**: `Connection refused to database`

**Solução**:
```bash
# Verificar que Docker está rodando e postgres está up
docker-compose ps

# Reiniciar postgres
docker-compose restart postgres
```

**Erro**: `Port 8080 ou 8081 já em uso`

**Solução**:
```bash
# Encontrar processo usando porta 8080
lsof -i :8080

# Matar processo (substituir PID)
kill -9 <PID>
```

### Servidor frontend não inicia

**Erro**: `Port 5173 já em uso`

**Solução**:
```bash
# Matar processo na porta 5173
lsof -i :5173
kill -9 <PID>

# Ou especificar porta diferente
npm run dev -- --port 5174
```

### Queries de banco retornam sem resultados

**Problema**: Dados não inseridos ou queries não encontram linhas

**Solução**:
```bash
# Verificar se soft-delete filtering está funcionando
SELECT COUNT(*) FROM products WHERE is_active = true;
```

---

## Parte 9: Próximos Passos

Após completar este início rápido:

1. **Ler documentação de API**: Revisar `contracts/product-service-api.openapi.yaml`
2. **Explorar modelo de dados**: Estudar `data-model.md` para relacionamentos de entidades
3. **Revisar estrutura de código**: Seguir padrão de Arquitetura Hexagonal
4. **Executar suite de testes**: Entender comportamento do código
5. **Implementar features**: Seguir `tasks.md` para tarefas de implementação (Phase 2)

---

## Recursos e Suporte

- **Contrato de API**: Ver `contracts/product-service-api.openapi.yaml`
- **Schema de Banco**: Ver `data-model.md`
- **Plano de Implementação**: Ver `plan.md`
- **Pesquisa Técnica**: Ver `research.md`
- **Constituição TechStore**: Ver `.specify/memory/constitution.md`
