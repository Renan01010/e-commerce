# Pesquisa: Catálogo de Produtos - Fundação

**Data**: 2026-09-26 | **Estágio**: Phase 0 (Clarificação de Incógnitas Técnicas)

## Objetivos

Este documento resolve incógnitas técnicas que devem ser esclarecidas antes do design Phase 1. Cada seção representa um ponto de decisão que impacta no plano de implementação.

---

## 1. Decisões de Stack de Tecnologia

### 1.1 Framework Backend e Versão

**Decisão**: **Spring Boot 3.3.x** (versão estável alinhada com LTS)

**Justificativa**:
- Suporte nativo a Java 21+
- Melhorias em Spring Security para JWT
- Melhorias em Spring Data JPA para desempenho
- Manutenção ativa e atualizações de segurança
- Alinha com constituição TechStore

**Dependências Confirmadas**:
- spring-boot-starter-web (API REST)
- spring-boot-starter-data-jpa (Persistência)
- spring-boot-starter-security (Autenticação/Autorização)
- spring-boot-starter-actuator (Observabilidade)
- spring-boot-starter-validation (Validação de dados)
- org.springframework.security:spring-security-oauth2-jose (Suporte JWT)
- io.jsonwebtoken:jjwt-* (Biblioteca JJWT para tokens JWT)
- org.postgresql:postgresql (Driver PostgreSQL JDBC 42.7.x)
- org.mapstruct:mapstruct (Mapeamento DTO)

### 1.2 Versão do Banco de Dados

**Decisão**: PostgreSQL 15.x

**Justificativa**:
- Versão estável com compatibilidade comprovada
- Excelente suporte para JSON, full-text search e recursos avançados
- Desempenho adequado para workloads de e-commerce
- Timeline de suporte longo alinha com uso empresarial

### 1.3 Framework Frontend e Versão

**Decisão**: React 18.2.x com TypeScript 5.x e Vite 5.x

**Justificativa**:
- React 18 oferece recursos concorrentes e desempenho melhorado
- TypeScript 5 fornece segurança de tipos forte
- Vite 5 oferece experiência de desenvolvimento rápida
- Alinha com constituição TechStore

### 1.4 Frameworks de Testes

**Decisão**: 
- **Backend**: JUnit 5 (Jupiter), Mockito, Testcontainers, AssertJ
- **Frontend**: Vitest, React Testing Library

**Justificativa**:
- Stack de testes moderno e bem integrado
- Testcontainers permite testes de integração com PostgreSQL real
- Vitest é otimizado para projetos Vite
- Alinha com constituição TechStore

---

## 2. Decisões de Arquitetura e Design

### 2.1 Arquitetura do API Gateway

**Decisão**: **Spring Cloud Gateway**

**Configuração**:
- Rota `/api/products/**` → `http://product-service:8080`
- Rota `/api/categories/**` → `http://product-service:8080`
- Filtro de requisição: Validação de token JWT
- Filtro de resposta: Adiciona correlation IDs para observabilidade

### 2.2 Estrutura de Token JWT e Autorização

**Decisão**:

**Tipo de Token**: Bearer token no header `Authorization`

**Claims do Token**:
```json
{
  "sub": "admin-user-id",
  "roles": ["ADMIN", "USER"],
  "permissions": ["product:read", "product:write"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Escopos de Autorização**:
- `ADMIN` role: Acesso completo a gerenciamento de produtos/categorias
- `USER` role: Acesso read-only a produtos/categorias
- Não autenticado: Acesso read-only a catálogo público

**Configuração JWT**:
- Emissor: TechStore Product Service
- Algoritmo: HS256 (chave simétrica)
- Expiração: 24 horas (configurável)
- Chave secreta: Externalizada em `application.yml` (variável ambiente)

### 2.3 Gerenciamento de Estado Frontend

**Decisão**: **Zustand**

**Justificativa**:
- Boilerplate mínimo comparado a Redux
- Integração fácil com React hooks
- Curva de aprendizado menor
- Suficiente para complexidade de catálogo de produtos

**Estrutura de Store**:
- Produtos, categorias, filtros de busca, paginação
- Ações: loadProducts, searchProducts, getProductDetails, setSearchFilters

### 2.4 Estratégia de Busca de Produtos

**Decisão**: Full-text search nativa PostgreSQL com tsvector

**Campos de Busca**: 
- Nome do produto (peso 3x)
- Marca (peso 2x)
- Descrição (peso 1x)

**Otimização de Query**:
- Índice GIN em tsvector para desempenho
- Paginação: Limite 20 produtos por página
- Opções de sort: Relevância (padrão), Preço (ASC/DESC), Nome, Mais novo

**Meta de Desempenho**: Queries de busca completas em <300ms p95

### 2.5 Imagens de Produtos

**Decisão**: Armazenamento apenas como URLs (sem armazenamento binário)

**Campo de Imagem**:
- `Product.imageUrl: String` (imagem primária)
- Validação: URL HTTP/HTTPS válida
- Frontend responsável por exibir imagens de URLs fornecidas
- Sem upload de imagem neste incremento (deferred para futuro)

### 2.6 Auditoria e Rastreamento

**Decisão**: Implementar campos de auditoria em todas as entidades de domínio

**Campos de Auditoria**:
- `createdAt: LocalDateTime` (timestamp de criação)
- `updatedAt: LocalDateTime` (timestamp de modificação)
- `createdBy: String` (ID do usuário que criou)
- `updatedBy: String` (ID do usuário que último atualizou)
- `isActive: Boolean` (padrão soft-delete)

---

## 3. Estratégia de Ambiente de Desenvolvimento

### 3.1 Docker e Containerização

**Decisão**: 
- **Dev Local**: Docker Compose para PostgreSQL + serviços opcionais
- **Imagem de Desenvolvimento**: Multi-stage Dockerfile
- **Runtime**: Imagens baseadas em Alpine para tamanho mínimo

**Serviços docker-compose.yml**:
- postgres: PostgreSQL 15-alpine
- api-gateway: Spring Cloud Gateway
- product-service: Product Service
- frontend: React dev server

### 3.2 Estratégia de Build e Deployment

**Backend**:
- Maven 3.9.x
- Compilador Java 21+
- Empacotamento JAR com Spring Boot Maven plugin

**Frontend**:
- Node.js 20.x LTS
- npm 10.x
- Vite build para produção: `npm run build`

---

## 4. Logging e Observabilidade

### 4.1 Logging Estruturado

**Formato**: JSON com Logback + JSON encoder

**Campos de Log**:
- timestamp (ISO 8601)
- level (INFO, WARN, ERROR, DEBUG)
- service (nome do serviço)
- correlationId (ID único de requisição)
- userId (ID do usuário do JWT)
- message (mensagem de log)
- context (contexto adicional)

### 4.2 Health e Métricas

**Endpoints Actuator** (Spring Boot):
- `GET /actuator/health` → Liveness probe
- `GET /actuator/metrics` → Métricas de aplicação
- `GET /actuator/prometheus` → Métricas para Prometheus (futuro)

**Métricas Rastreadas**:
- Contagem de requisições HTTP
- Tempo de resposta percentis (p50, p95, p99)
- Uso de pool de conexão de banco
- Queries de busca ativas

---

## 5. Implementação de Segurança

### 5.1 Gerenciamento de Secrets

**Em Desenvolvimento**:
- Arquivo `.env.local` (não commitado ao repo)
- Variáveis de ambiente carregadas por Spring Boot

**Em Produção** (futuro):
- AWS Secrets Manager ou HashiCorp Vault

### 5.2 Hashing de Senhas (para autenticação futura)

**Decisão**: BCrypt com strength 12

### 5.3 Prevenção de SQL Injection

**Estratégia**: 
- Queries parametrizadas JPA (automático com Spring Data)
- Nunca concatenar input do usuário em SQL
- Validar e sanitizar input na camada de API

---

## Conclusão e Próximos Passos

### Status de Conclusão Phase 0

✅ **Todas as incógnitas técnicas maiores resolvidas**:
- Stack de tecnologia confirmada
- Padrões de arquitetura decididos
- Estratégia de banco de dados estabelecida
- Abordagem JWT e autorização definida
- State management frontend selecionado
- Estratégia de testes esboçada
- Plano de observabilidade definido

### Ações Restantes (Phase 1)

Os seguintes artefatos devem ser concluídos em Phase 1:

1. **data-model.md** - Definições completas de entidades, schema DDL
2. **contracts/product-service-api.openapi.yaml** - Especificação OpenAPI 3.1 completa
3. **contracts/product-service-events.json** - Definições de schema de eventos
4. **quickstart.md** - Guia passo-a-passo de setup local

### Gate: Pronto para Phase 1 Design

✅ **Phase 0 COMPLETO** - Proceder para Phase 1
