# Pesquisa e Decisões: Carrinho no Frontend

**Data**: 2026-09-28 | **Branch da Feature**: `008-frontend-cart`

## Contexto Verificado

- O frontend usa React 18.3.1, TypeScript 5.7.2, React Router 6.28.1, Axios 1.7.9 e Zustand 5.0.2; Vitest/Testing Library e `user-event` já estão disponíveis em `frontend/package.json`.
- `frontend/src/services/apiClient.ts` configura base URL `VITE_API_URL` (padrão `/api`), timeout de 10 segundos e interceptor que anexa o bearer token válido de `useAuthStore`. O mesmo módulo concentra `catalogApi` e o mapeamento atual de erros de catálogo.
- `frontend/src/store/authStore.ts` mantém sessão somente em memória e oferece `setSession`, `clearSession` e `getValidSession`; token expirado é removido ao consultar a sessão válida. Não existe middleware de persistência.
- `frontend/src/store/catalogStore.ts` é o padrão existente para estado global Zustand e já protege contra resposta antiga de consulta sobrescrever uma resposta mais nova, usando identificador de request.
- `frontend/src/App.tsx` registra `/`, `/products/:id`, `/login` e `/register`. `StoreLayout` contém o badge de carrinho placeholder e envolve rotas da loja; não há componente de diálogo compartilhado.
- `ProductDetailPage` já consulta o produto por UUID, mostra preço e estoque informativos e mantém o botão de inclusão desabilitado. O teste atual valida esse placeholder e deve ser substituído por cobertura da inclusão.
- Não existem store, service, types, página ou componentes próprios do carrinho. Não há necessidade confirmada de instalar biblioteca adicional.
- O contrato HTTP e as regras do Cart Service estão em `specs/004-cart/contracts/cart-service-api.openapi.yaml`; todos os endpoints públicos estão expostos no Gateway sob `/api/cart`.
- A sessão é volátil: um reload deixa o usuário sem token até novo login. A decisão clarificada para a rota `/cart` sem sessão é manter a rota e apresentar orientação/link para `/login`; limpar o carrinho requer confirmação acessível.
- A branch Git identificada pelo setup local é `008-catalog-ui-correction`, diferente desta feature. Os artefatos deste plano pertencem explicitamente a `specs/008-frontend-cart/` e não devem ser escritos na pasta da branch atual.

## Decisões

### 1. Reutilizar o contrato do Cart Service pelo Axios existente

**Decisão**: expor `cartApi` no módulo de integração HTTP já existente e chamar os paths relativos `/cart` e `/cart/items...`. `apiClient` continua responsável por base URL, timeout e JWT.

**Racional**: com o `VITE_API_URL` padrão `/api`, paths relativos preservam o Gateway e o interceptor fornece o token. A resposta 204 de DELETE é tratada como sem corpo. Nenhum endpoint novo ou acesso direto ao serviço é introduzido.

**Alternativas consideradas**: Axios separado, chamada HTTP em componentes, URL direta do Cart Service e duplicação da API. Rejeitadas por duplicarem configuração ou contornarem as fronteiras existentes.

### 2. Compartilhar estado em Zustand sem nova dependência

**Decisão**: criar um `cartStore` pequeno com as linhas retornadas, status de carga/operação e erro/feedback; derivar o badge pela soma de `quantity`. Reutilizar o padrão do `catalogStore` e não instalar biblioteca de estado.

**Racional**: estado precisa ser compartilhado entre layout, detalhe e página do carrinho, e o projeto já usa Zustand para esse propósito.

**Alternativas consideradas**: estado local duplicado por rota, contexto adicional ou outra biblioteca. Rejeitadas por fragmentarem estado ou adicionarem dependência sem benefício demonstrado.

### 3. Carregar uma vez por sessão e descartar respostas obsoletas

**Decisão**: não iniciar GET sem sessão válida. Quando o cabeçalho ou a página precisar do badge/linhas, o store faz um GET deduplicado por sessão em memória e reutiliza resultado entre rotas. Uma resposta de carga iniciada antes de mutação confirmada ou troca de sessão é ignorada.

**Racional**: satisfaz contador real sem GET em cada renderização, evita requests duplicados durante navegação e impede vazamento de dados entre sessões. O padrão de request-id do catálogo fornece referência local para respostas antigas.

**Alternativas consideradas**: buscar somente ao entrar em `/cart` (badge do header ficaria sem dado até essa visita) ou buscar em todo render/foco de rota (requests redundantes). Ambas rejeitadas pela experiência e requisitos.

### 4. Vincular estado à sessão sem armazenar credenciais

**Decisão**: observar mudanças do objeto de sessão em `authStore`; em `clearSession`, expiração ou troca da sessão, descartar linhas e requests em andamento. Em resposta 401 do Cart Service, limpar sessão pela ação existente e o estado do carrinho, mantendo a rota aberta com link para login.

**Racional**: evita exibir carrinho anterior após logout/expiração/relogin e não adiciona persistência de token nem identidade enviada pelo cliente. O comportamento de rota mantém a clarificação aprovada (sem redirect automático).

**Alternativas consideradas**: manter as linhas após 401 (risco de informação de outra sessão), armazenar token/userId no cart store ou redirecionar à força para login. Rejeitadas por segurança e por conflito com a especificação.

### 5. Usar respostas confirmadas e sem otimização especulativa

**Decisão**: POST/PUT substituem as linhas compartilhadas pelo `CartResponse`; DELETE de linha remove localmente somente após 204; limpeza esvazia localmente somente após 204. Falhas preservam o último estado confirmado. Desabilitar o mesmo controle durante sua operação pendente e deduplicar o GET inicial.

**Racional**: os corpos de POST/PUT são o estado autoritativo mais recente e os DELETE não retornam corpo. Evitar atualização otimista reduz necessidade de rollback e mantém o backend como autoridade da regra atômica.

**Alternativas consideradas**: somar ou alterar quantidades independentemente no frontend, ou reconstruir a resposta de DELETE por GET em toda operação. Rejeitadas por duplicarem regra do backend ou gerarem requests sem necessidade.

### 6. Tratar erro no contexto do carrinho

**Decisão**: manter mensagens e classificação para 401/404/503/rede na camada de carrinho sem alterar textos atuais do catálogo; falha de mutation não marca sucesso nem modifica linhas confirmadas.

**Racional**: o helper compartilhado atual usa mensagens próprias de catálogo. Reutilizar esse texto para carrinho produziria feedback incorreto; um mapeamento específico evita alterar comportamento fora do escopo.

### 7. Confirmação de limpeza sem infraestrutura de modal nova

**Decisão**: implementar estado de confirmação acessível dentro da experiência da página, com ações explícitas confirmar/cancelar; cancelar não chama API. Manter suporte por teclado e foco visível.

**Racional**: o projeto não possui padrão de diálogo reutilizável e a operação exige confirmação conforme clarificação. Uma interação focada evita dependência ou sistema global de modal.

**Alternativas consideradas**: `window.confirm` (não segue os estilos e feedback do produto) ou nova infraestrutura genérica de modal (complexidade além desta feature).

### 8. Dados e disponibilidade pertencem ao contrato do backend

**Decisão**: tipos refletem literalmente `CartResponse`, incluindo `product: null`. O resumo atual é apresentado somente quando presente; indisponibilidade usa o campo `available`, não o estoque `quantity` do catálogo. Preço é informativo por linha e nunca somado.

**Racional**: preserva o contrato 004-cart, evita acesso inválido a resumo ausente e não desloca para o frontend regras autoritativas de disponibilidade ou valor financeiro.

## Estratégia de Testes

- Testes de serviço verificam métodos, paths, corpos, aceitação de POST 200/201 e DELETE 204 sem corpo.
- Testes do store cobrem GET deduplicado, badge, sucesso/falha de mutação, sessão substituída/expirada e descarte de respostas antigas.
- Testes de componentes/páginas cobrem sessão ausente, vazio, item disponível/indisponível, quantidade, confirmação/cancelamento, loading, erro, retorno ao catálogo e chamada de inclusão a partir do detalhe.
- Testes do App verificam badge e navegação para `/cart`.
- Os gates são `npm test` e `npm run build` executados em `frontend/`; a validação manual cobre desktop/tablet/mobile. Nenhum framework E2E novo é necessário nesta fase.

## Pendências

Nenhuma decisão técnica bloqueante identificada. A branch Git ativa não corresponde à feature; preservar o destino documental `specs/008-frontend-cart/` ao gerar tasks.