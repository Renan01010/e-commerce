# Pesquisa e Decisões: Login do Cliente

**Data**: 2026-09-27 | **Branch**: `005-frontend-login`

## Contexto verificado

- O frontend existente é React 18.3.1 + TypeScript 5.7.2 + Vite 5.4.11, com React Router 6.28.1, Axios 1.7.9 e Zustand 5.0.2 em `frontend/package.json`.
- `frontend/src/services/apiClient.ts` já cria um cliente Axios usando `VITE_API_URL` (padrão `/api`) e timeout padrão de 10 s. O serviço atual é só de catálogo e não injeta token.
- `frontend/src/store/catalogStore.ts` usa Zustand; não existe store, rota ou serviço de autenticação no frontend.
- `frontend/src/App.tsx` tem Home em `/`, detalhe `/products/:id` e fallback 404; `/login` ainda não existe.
- Vite não configura proxy; a API deve estar acessível pelo `VITE_API_URL` configurado ou pelo Gateway publicado.
- Não há fotografia local no frontend. A imagem anexada é referência de composição/art direction, não um asset de runtime.
- O solicitante fornece o contrato de login e informa que o endpoint já existe. Esta feature não cria nem altera o User Service/API Gateway.

## Decisões

### Estado e segurança da sessão

**Decisão**: manter `accessToken`, `tokenType` e `expiresAt` em um store Zustand em memória sem persist middleware. A sessão é compartilhada entre rotas, mas desaparece ao recarregar ou fechar a página. Não persistir senha.

**Rationale**: responde à clarificação aceita, aproveita o padrão de estado já usado no SPA e evita expor token em armazenamento durável do navegador. A ausência de refresh token torna explícita a necessidade de autenticar novamente após reload.

**Alternativas consideradas**: `sessionStorage` (sobrevive a reload e aumenta duração da exposição); `localStorage` (persistência além da vida da página sem requisito de “lembrar de mim”); ambas rejeitadas nesta feature.

### Cliente HTTP e serviço de autenticação

**Decisão**: adicionar `authService.login(credentials)` usando o cliente Axios compartilhado e o path relativo `/auth/login`, que resolve para `POST /api/auth/login` com o `VITE_API_URL` padrão. O interceptor de request consulta o Zustand store em tempo de chamada e adiciona `Authorization: Bearer <accessToken>` quando há sessão não expirada.

**Rationale**: evita chamadas HTTP espalhadas pela UI e usa a base URL/timeout atuais. `authStore` não importa `authService`, evitando dependência circular.

**Alternativas consideradas**: criar um segundo cliente Axios com base URL própria (duplica configuração); chamar Axios diretamente na página (mistura apresentação e integração); persistir header em storage (não necessário para a sessão volátil).

### Falhas de login

**Decisão**: classificar `401` como credenciais inválidas e apresentar mensagem genérica; falha sem resposta/timeout/5xx recebe mensagem de indisponibilidade; não exibir texto arbitrário do backend. E-mail permanece para nova tentativa; senha não é incluída em logs ou mensagens.

**Rationale**: comunica ação possível ao usuário e não revela existência/estado de contas nem detalhes internos.

**Alternativas consideradas**: expor `message` completo do backend (rejeitada por segurança e inconsistência); tratar todo erro como credencial inválida (rejeitada por confundir indisponibilidade com senha errada).

### Direção visual e asset

**Decisão**: seguir a imagem anexada como referência da tela, não como bitmap de runtime: marca/copy e foto tech à esquerda, formulário em painel navy à direita, ação azul; mobile em coluna com formulário como prioridade. Manter catálogo atual sem redesign. Implementação deve usar uma fotografia local licenciada/aprovada, em vez de imagem remota no carregamento.

**Rationale**: respeita a clarificação de escopo visual, a tela permanece útil se a rede de imagens falhar e o protótipo fornecido não está presente como arquivo standalone no repositório.

**Alternativas consideradas**: aplicar o tema navy/azul ao catálogo todo (escopo maior); carregar uma URL de imagem de terceiros em runtime (dependência externa e desempenho imprevisível); usar a captura inteira como fundo (texto duplicado e pouco adaptável).

### Testes

**Decisão**: Vitest e Testing Library existentes. Testes de serviço mockam o cliente HTTP; testes de componente cobrem submit, validação, carregamento, erros, senha visível/oculta, navegação e teclado. Build TypeScript/Vite e testes unitários são gates.

**Rationale**: mantém feedback rápido sem depender de User Service para testes de componente; contrato real continua coberto por teste de request/response.

**Alternativas consideradas**: autenticação fake no ambiente de produção (proibida); testes apenas manuais (não reproduzíveis); adicionar framework de teste E2E nesta feature (desnecessário para o risco delimitado).

## Pendências

Nenhuma clarificação bloqueante. Para executar o quickstart é necessário um User Service compatível acessível via Gateway e credenciais válidas; o frontend não simulará esse serviço.