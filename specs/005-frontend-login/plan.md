# Plano de Implementação: Login do Cliente

**Branch**: `005-frontend-login` | **Data**: 2026-09-27 | **Spec**: [spec.md](spec.md)

**Entrada**: [Especificação clarificada](spec.md), com duas decisões: token somente em memória e tema navy/azul limitado à tela de login.

## Resumo

Adicionar a rota de login ao SPA TechStore usando o endpoint existente do Gateway. Reutilizar o cliente Axios e os padrões de estado Zustand, introduzindo um serviço de autenticação separado e um estado de sessão não persistido. Credenciais inválidas e falhas de comunicação recebem mensagens seguras; sucesso guarda token/validade em memória e navega para a Home. Nenhum contrato ou serviço backend será alterado.

## Contexto Técnico

**Linguagem/Versão**: TypeScript 5.7.2 e React 18.3.1.

**Dependências Primárias**: Vite 5.4.11, React Router 6.28.1, Axios 1.7.9, Zustand 5.0.2 e `lucide-react` para ícones acessíveis; adicionar somente essa última dependência à implementação.

**Armazenamento**: Sem persistência local. `AuthSession` vive apenas no estado em memória do SPA e é perdida ao recarregar/fechar a página.

**Testes**: Vitest 2.1.5, React Testing Library 16.1.0, `@testing-library/user-event` 14.5.2 e `@testing-library/jest-dom` 6.6.3.

**Plataforma Alvo**: Navegadores desktop e mobile; viewport suportada a partir de 320 px. Desenvolvimento e build no frontend Vite atual.

**Tipo de Projeto**: Single-page web application React/TypeScript existente.

**Metas de Desempenho**: A spec não define SLA; usar o timeout atual do cliente HTTP (10 s) e apresentar estado de carregamento durante a operação.

**Restrições**: Endpoint `POST /api/auth/login` existente e sem alteração; base URL usa `VITE_API_URL` (padrão `/api`). Reutilizar o cliente Axios compartilhado; enviar token somente nas chamadas autenticadas; não persistir token nem senha; sem cadastro, Google OAuth, recuperação de senha, refresh token ou logout nesta feature.

**Escala/Scope**: Uma rota `/login`, serviço de autenticação, estado global de sessão em memória, campo de senha com visibilidade alternável e navegação para `/register` futuro. Home e catálogo existentes não serão redesenhados.

## Constitution Check

**Gate antes da pesquisa**

| Princípio | Estado | Evidência/condição |
|---|---|---|
| I. Microsserviços orientados ao domínio | N/A | Nenhum serviço backend ou database novo; a tela consome o serviço de autenticação existente. |
| II. Arquitetura Hexagonal | PASS | Aplica-se aos serviços backend; no frontend a API fica em serviço separado da apresentação e do estado de aplicação. |
| III. Clean Code/SOLID | PASS | Página, formulário, serviço HTTP e estado de autenticação têm responsabilidades separadas. |
| IV. Test-first | PASS | Validação e transições de sessão terão testes unitários; jornada e acessibilidade básica terão testes de componente. |
| V. API e contratos | PASS | O request/response existente fica documentado; nenhum contrato backend é alterado. |
| VI. Segurança por padrão | PASS | Backend continua autoridade; token fica em memória; senha/token não são registrados nem mostrados em erros. |
| VII. Observabilidade | N/A | Nenhum serviço de produção ou telemetria nova faz parte desta feature. |
| VIII. Arquitetura frontend | PASS | Apresentação, serviço HTTP e estado reutilizável separados; não duplica autenticação no cliente. |
| IX. Reprodutibilidade | PASS | Usa toolchain e comandos existentes do frontend; API base URL permanece externalizada. |
| X. Spec-driven | PASS | Spec clarificada e artefatos de plano/design precedem tasks e implementação. |

**Reavaliação pós-design**: PASS. Sessão volátil corresponde à clarificação e não cria dependência de persistência, pacote de storage ou endpoint backend novo.

## Estrutura do Projeto

### Documentação desta feature

```text
specs/005-frontend-login/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── auth-login-api.openapi.yaml
```

### Código-fonte planejado

```text
frontend/
├── package.json                         # adicionar lucide-react
└── src/
    ├── App.tsx                           # registrar rota /login
    ├── components/auth/
    │   ├── LoginForm.tsx
    │   ├── PasswordField.tsx
    │   └── __tests__/
    ├── pages/
    │   ├── LoginPage.tsx
    │   └── __tests__/LoginPage.test.tsx
    ├── services/
    │   ├── apiClient.ts                  # adicionar interceptor Bearer
    │   ├── authService.ts
    │   └── __tests__/authService.test.ts
    ├── store/
    │   ├── authStore.ts                  # Zustand em memória, sem persist middleware
    │   └── __tests__/authStore.test.ts
    ├── types/auth.ts
    └── assets/login-hero.webp            # fotografia local aprovada para a área visual
```

**Decisão de estrutura**: usar o frontend existente, sem novo projeto. `authService` chama o endpoint por meio do Axios compartilhado; `authStore` não importa o serviço e mantém apenas o estado de sessão; o interceptor do Axios lê o store em tempo de requisição. A página compõe formulário reutilizável, campos e estados visuais. A resposta do login é transformada em sessão com `expiresAt = now + expiresIn`.

## Complexidade

Sem violações constitucionais ou serviços adicionais. `lucide-react` atende aos ícones do formulário; o restante usa os padrões CSS atuais. A imagem anexada define direção artística; o runtime usará uma fotografia local aprovada, não a captura do protótipo como imagem de fundo.