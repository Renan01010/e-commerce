# Plano de Implementação: Catálogo de Produtos - Fundação

**Branch**: `001-foundation-product-catalog` | **Data**: 2026-09-26 | **Especificação**: [spec.md](spec.md)

**Entrada**: Especificação do recurso em `/specs/001-foundation-product-catalog/spec.md`

**Observação**: Este plano descreve a abordagem técnica para implementar o Serviço de Produtos e a interface de catálogo de produtos voltada ao cliente como fundação para TechStore.

## Resumo

O Catálogo de Produtos Fundacional é o primeiro incremento da plataforma TechStore. Estabelece:

- **Backend**: Um microsserviço Product Service (Java/Spring Boot) expondo uma API REST para gerenciamento de produtos, categorias e busca de produtos
- **Banco de Dados**: Banco PostgreSQL propriedade exclusiva do Product Service
- **API Gateway**: Ponto de entrada para requisições do frontend voltado ao cliente
- **Frontend**: Aplicação React/TypeScript fornecendo descoberta de produtos, navegação e visualização de detalhes com placeholder não funcional "Adicionar ao Carrinho"
- **Arquitetura**: Arquitetura Hexagonal (Portas e Adaptadores) com design orientado ao domínio, contratos REST documentados via OpenAPI, cobertura de testes abrangente
- **Segurança**: Autenticação e autorização baseadas em JWT para operações administrativas
- **Observabilidade**: Verificações de saúde Spring Boot Actuator e logging estruturado

Este incremento foca em visibilidade do catálogo e capacidades de administração, adiando funcionalidades de carrinho/pedidos para incrementos futuros.

## Contexto Técnico

**Linguagem/Versão**: Java 21+

**Dependências Primárias**: 
- Spring Boot (3.3.x)
- Spring Security + JWT
- Spring Data JPA
- Spring Boot Actuator
- MapStruct (mapeamento DTO)
- React 18+ (frontend)
- TypeScript (frontend)
- Axios (cliente HTTP frontend)

**Armazenamento**: PostgreSQL 15 (propriedade do Product Service)

**Testes**: 
- JUnit 5 (testes unitários backend)
- Mockito (mocking)
- Testcontainers (testes de integração PostgreSQL)
- Vitest ou Jest (testes unitários frontend)

**Plataforma Alvo**: Servidor Linux (microsserviço) + Navegador web (frontend)

**Tipo de Projeto**: Microsserviço + Single Page Application (SPA)

**Metas de Desempenho**: 
- Carregamento da página de catálogo em <500ms p95
- Consultas de busca completas em <300ms p95
- Suporte a 100+ usuários simultâneos

**Restrições**: 
- Todos os dados de produto persistidos no banco Product Service (sem DB compartilhado)
- Operações de carrinho fora de escopo (UI placeholder apenas)
- Operações de admin API-only (sem UI de painel admin)
- Todas as operações administrativas requerem autorização
- Imagens de produtos armazenadas como URLs (sem armazenamento binário)

**Escala/Escopo**: 
- Catálogo com 10k+ produtos
- 50+ categorias
- 2-3 unidades de deployment (API Gateway, Product Service, Frontend)

## Verificação da Constituição

**GATE: Deve passar antes da pesquisa Phase 0. Re-verificar após design Phase 1.**

| Princípio | Status | Notas |
|-----------|--------|-------|
| **I. Microsserviços Orientados ao Domínio** | ✅ PASSOU | Product Service é bounded context com responsabilidade clara. Isolamento de BD enforçado. Contratos REST para comunicação. |
| **II. Arquitetura Hexagonal** | ✅ PASSOU | Camada de domínio isolada de adaptadores Spring Boot/JPA/HTTP. Dependências apontam para dentro. |
| **III. Código Limpo e SOLID** | ✅ PASSOU | Single Responsibility aplicado a serviços, repositórios e casos de uso. Sem duplicação de lógica. Soluções simples. |
| **IV. Qualidade Test-First** | ✅ PASSOU | Regras de negócio e casos de uso com cobertura de testes (JUnit/Mockito/Testcontainers). |
| **V. Disciplina de API e Contrato** | ✅ PASSOU | API REST explícita documentada via OpenAPI. Métodos HTTP e status codes convencionais. Mudanças quebrantes identificadas. |
| **VI. Segurança por Padrão** | ✅ PASSOU | Autenticação JWT para operações admin. Autorização enforçada. Senhas hasheadas (BCrypt). Dados sensíveis não em logs. |
| **VII. Observabilidade** | ✅ PASSOU | Spring Boot Actuator com health checks e métricas. Logging estruturado com correlation IDs. |
| **VIII. Arquitetura Frontend** | ✅ PASSOU | Componentes React separam apresentação de comunicação com API. Regras de negócio no backend. Carrinho é placeholder. |
| **IX. Infraestrutura e Reprodutibilidade** | ✅ PASSOU | Docker + Docker Compose para ambientes reproduzíveis. Configuração externalizada. Sem secrets no repo. |
| **X. Desenvolvimento Orientado a Especificações** | ✅ PASSOU | Feature segue workflow Spec Kit (Specify → Clarify → Plan → Tasks → Implement → Converge). |

**Resultado do Gate**: ✅ **PASSOU** - Sem violações. Implementação pode proceder.

## Estrutura do Projeto

### Documentação (esta feature)

```text
specs/001-foundation-product-catalog/
├── spec.md              # Especificação e esclarecimentos da feature
├── plan.md              # Este arquivo (plano de implementação)
├── research.md          # Phase 0: Pesquisa técnica e resolução de incógnitas
├── data-model.md        # Phase 1: Modelo de domínio, schema de banco, entidades
├── quickstart.md        # Phase 1: Guia de início rápido para desenvolvimento
├── contracts/           # Phase 1: Contratos de API e schemas
│   ├── product-service-api.openapi.yaml
│   └── product-service-events.json
└── tasks.md             # Phase 2 (não gerado por este plano)
```

### Código-Fonte (raiz do repositório) - Estrutura Planejada

```text
backend/
├── api-gateway/
│   ├── src/
│   │   ├── main/java/com/techstore/gateway/
│   │   │   ├── config/
│   │   │   │   └── RouteConfig.java
│   │   │   └── filter/
│   │   │       └── AuthenticationFilter.java
│   │   └── resources/
│   │       └── application.yml
│   ├── Dockerfile
│   └── pom.xml
│
└── product-service/
    ├── src/
    │   ├── main/java/com/techstore/product/
    │   │   ├── domain/
    │   │   ├── application/
    │   │   └── adapter/
    │   ├── test/java/com/techstore/product/
    │   └── resources/
    ├── Dockerfile
    └── pom.xml

frontend/
├── src/
│   ├── pages/
│   ├── components/
│   ├── services/
│   ├── hooks/
│   ├── types/
│   └── App.tsx
├── tests/
├── Dockerfile
├── vite.config.ts
├── package.json
└── tsconfig.json

docker-compose.yml
```

**Decisão de Estrutura**: 
Estrutura multi-projeto (Opção 2: Aplicação Web) com serviços backend e frontend separados. Product Service é microsserviço standalone seguindo Arquitetura Hexagonal. API Gateway fornece ponto de entrada unificado. Frontend React é unidade de deployment separada. Alinha com estratégia de microsserviços TechStore permitindo scaling e deployment independentes.

## Rastreamento de Complexidade

> Sem violações da Constituição que requerem justificação.

---

## Status Phase 0: Pesquisa

*A ser concluído e documentado em `research.md`*

### Objetivos de Pesquisa

1. Confirmar dependências técnicas e versões
2. Definir configuração de roteamento do API Gateway
3. Especificar estrutura de token JWT e escopos de autorização
4. Determinar estratégia de busca de produtos
5. Esclarecer state management frontend
6. Definir networking Docker e configuração de ambiente

---

## Status Phase 1: Design

*A ser concluído e documentado em `data-model.md`, `contracts/`, e `quickstart.md`*

### Entregas de Design

1. **data-model.md** - Entidades de domínio, schema de banco e relacionamentos
2. **contracts/product-service-api.openapi.yaml** - Endpoints e schemas OpenAPI
3. **contracts/product-service-events.json** - Schemas de eventos (Phase 3+)
4. **quickstart.md** - Guia de setup e desenvolvimento local

---

## Re-avaliação Pós-Phase 1

*Verificação da Constituição será re-avaliada após conclusão do design Phase 1.*
