# Feature Specification: Login do Cliente

**Feature Branch**: `005-frontend-login`

**Created**: 2026-09-27

**Status**: Draft

**Input**: Solicitação para criar a tela de login do frontend TechStore, usando o protótipo visual anexado.

## Clarifications

### Session 2026-09-27

- Q: Por quanto tempo o `accessToken` deve permanecer disponível depois do login? → A: Somente no estado compartilhado em memória; disponível entre navegações da aplicação e removido ao recarregar ou fechar a página.
- Q: A identidade azul/navy do protótipo deve valer apenas para o login ou também deve substituir a paleta atual do catálogo nesta feature? → A: Aplicar navy/azul somente ao login nesta feature; manter o catálogo atual inalterado e deixar telas futuras adotarem essa direção em features próprias.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entrar na conta (Priority: P1)

Como cliente com uma conta TechStore, quero entrar usando meu e-mail e senha para acessar a loja autenticado.

**Why this priority**: A autenticação habilita o acesso às jornadas protegidas da loja e é a finalidade primária da tela.

**Independent Test**: Com credenciais válidas do serviço existente, enviar o formulário, confirmar que a sessão do frontend recebe o token e que o cliente chega à página inicial do e-commerce.

**Acceptance Scenarios**:

1. **Given** e-mail e senha válidos, **When** o cliente enviar o formulário, **Then** as credenciais são enviadas ao endpoint de login existente e a resposta de sucesso estabelece a sessão autenticada.
2. **Given** uma resposta de sucesso com `accessToken`, **When** a sessão for estabelecida, **Then** o cliente é redirecionado para `/` e o estado autenticado fica disponível para navegação nas demais telas.
3. **Given** que a requisição de login está em andamento, **When** o cliente tentar enviar novamente, **Then** o envio permanece desabilitado e um estado de carregamento fica visível.

### User Story 2 - Corrigir entrada e entender falhas (Priority: P1)

Como cliente, quero receber orientação clara quando os campos estiverem incorretos ou o login falhar para poder tentar novamente sem expor meus dados.

**Why this priority**: Validação e feedback claros evitam submissões inúteis e tornam falhas de autenticação compreensíveis sem revelar se uma conta existe.

**Independent Test**: Submeter formulário vazio, e-mail malformado, credenciais inválidas e cenário sem conexão; confirmar bloqueio local quando aplicável, mensagens apropriadas e ausência de autenticação falsa.

**Acceptance Scenarios**:

1. **Given** e-mail ou senha ausentes, **When** o cliente tentar enviar, **Then** o frontend aponta os campos obrigatórios e não chama o serviço de autenticação.
2. **Given** um e-mail em formato básico inválido, **When** o cliente tentar enviar, **Then** o frontend informa a correção e não envia a requisição.
3. **Given** credenciais rejeitadas, **When** o serviço responder com falha de autenticação, **Then** uma mensagem clara e genérica informa que e-mail ou senha são inválidos, sem confirmar se o e-mail está cadastrado.
4. **Given** falha de rede ou indisponibilidade do serviço, **When** o cliente enviar credenciais válidas, **Then** uma mensagem informa que o login não pôde ser concluído e permite nova tentativa sem exibir detalhes internos.
5. **Given** resposta de sucesso malformada ou sem `accessToken`, **When** o frontend processar a resposta, **Then** não estabelece sessão nem redireciona e apresenta falha segura.
6. **Given** a senha digitada está oculta, **When** o cliente ativar o controle de visibilidade, **Then** a senha alterna entre visível e oculta sem apagar o valor e o controle permanece acessível por teclado e leitor de tela.

### User Story 3 - Ir para criação de conta (Priority: P2)

Como visitante sem conta, quero encontrar a navegação para criação de conta para iniciar esse fluxo quando estiver disponível.

**Why this priority**: A navegação evita bloquear novos clientes, enquanto a implementação do cadastro pertence a uma feature separada.

**Independent Test**: Abrir a tela de login, navegar por teclado até o link de criação de conta e ativá-lo; confirmar que `/register` apresenta um estado informativo não funcional, sem formulário, request de cadastro ou alteração da sessão.

**Acceptance Scenarios**:

1. **Given** a tela de login aberta, **When** o visitante ativar “Criar conta”, **Then** a aplicação navega para `/register`.
2. **Given** a funcionalidade de cadastro ainda não foi entregue, **When** o visitante acessar `/register`, **Then** a aplicação exibe um estado informativo “Cadastro indisponível no momento”, sem formulário, request de cadastro ou alteração da sessão autenticada.

### Edge Cases

- Cliques repetidos no botão de entrar durante uma requisição não criam requisições concorrentes.
- Espaços no início/fim do e-mail não devem fazer a validação básica rejeitar um endereço otherwise válido; a normalização final é responsabilidade do serviço de autenticação.
- Falhas do serviço não apagam o e-mail digitado; a senha não é incluída em mensagens ou logs e não é retida como dado de sessão.
- A resposta HTTP indica sucesso, mas o payload não contém token utilizável: manter o visitante não autenticado.
- O layout em largura móvel não pode esconder o formulário, o botão principal, mensagens de erro ou a navegação de criação de conta.
- Preferência de mostrar/ocultar senha não altera o valor enviado nem o estado de carregamento.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A aplicação DEVE disponibilizar a tela de login em `/login` e manter `/` como destino após autenticação bem-sucedida.
- **FR-002**: A tela DEVE fornecer campos identificados para e-mail e senha; ambos são obrigatórios.
- **FR-003**: A senha DEVE iniciar oculta e possuir controle para alternar sua visibilidade sem alterar seu conteúdo.
- **FR-004**: O frontend DEVE validar presença dos campos e formato básico do e-mail antes de iniciar uma requisição.
- **FR-005**: O frontend DEVE enviar as credenciais ao endpoint existente `POST /api/auth/login`, sem simular autenticação nem alterar o contrato do backend.
- **FR-006**: A requisição DEVE usar o corpo de credenciais definido pelo serviço existente:

```json
{
  "email": "usuario@exemplo.com",
  "password": "senha"
}
```

- **FR-007**: Uma resposta válida de sucesso contém `accessToken`, `tokenType` e `expiresIn`; o frontend DEVE estabelecer a sessão compartilhada usando o token e a validade retornados, sem guardar a senha nem persistir o token após recarga ou fechamento da página.
- **FR-008**: Depois de estabelecer a sessão, a aplicação DEVE redirecionar o cliente para `/` e disponibilizar a autenticação às telas que fizerem chamadas protegidas.
- **FR-009**: Durante o envio, o frontend DEVE exibir carregamento e impedir submissões duplicadas.
- **FR-010**: Credenciais inválidas DEVEM gerar mensagem compreensível e genérica, sem revelar a existência ou estado da conta.
- **FR-011**: Falhas de rede, timeout, respostas de erro do serviço ou resposta de sucesso inválida DEVEM manter o visitante não autenticado, apresentar feedback seguro e permitir nova tentativa.
- **FR-012**: A tela DEVE incluir link de navegação para a futura rota de criação de conta; cadastro e persistência de uma nova conta NÃO fazem parte desta feature.
- **FR-013**: A tela DEVE permitir uso por teclado, labels associados aos campos e nome acessível para o controle de visibilidade da senha.
- **FR-014**: A tela DEVE adaptar-se a desktop e mobile, mantendo campos, ações e mensagens sem sobreposição ou corte.
- **FR-015**: A composição visual DEVE seguir a referência anexada: no desktop, área visual/branding à esquerda e formulário à direita; em telas estreitas, uma coluna com o formulário como ação primária.
- **FR-016**: A identidade visual desta tela DEVE usar tons navy/escuros e azul como cor primária de ação, com marca TechStore e linguagem visual de e-commerce de tecnologia.
- **FR-017**: A apresentação, a lógica de autenticação e a comunicação com a API DEVEM permanecer separadas; a autenticação deve ser reutilizável por outras telas e não pode ficar espalhada em componentes.

### Contrato de Sucesso do Serviço

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

O serviço de autenticação é a autoridade para validar credenciais e emitir tokens. O frontend limita-se a validar formato/presença para experiência de uso; não replica regras de autenticação do backend.

### Key Entities *(include if feature involves data)*

- **Credenciais de login**: e-mail e senha fornecidos temporariamente para uma tentativa de autenticação; a senha não integra o estado autenticado nem mensagens de erro.
- **Sessão autenticada**: estado transitório estabelecido após resposta válida, contendo o token de acesso, tipo e validade para uso pelas telas protegidas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos envios com credenciais válidas chegam ao destino inicial `/` e disponibilizam o token de acesso ao estado compartilhado da aplicação.
- **SC-002**: 100% dos formulários com campo obrigatório vazio ou e-mail em formato inválido são bloqueados antes de qualquer chamada de login.
- **SC-003**: Credenciais inválidas e falhas de comunicação nunca autenticam o cliente nem exibem senha, token ou detalhes internos do serviço.
- **SC-004**: Todos os controles da tela podem ser alcançados e acionados por teclado, e os campos são identificados por labels acessíveis.
- **SC-005**: Em larguras de viewport de 320 px a 1440 px, o formulário, a ação principal, erros e link de criação de conta permanecem visíveis e sem sobreposição.
- **SC-006**: Enquanto o serviço processa o login, existe indicação visual de carregamento e nenhuma segunda submissão é enviada.

## Assumptions

- O endpoint `POST /api/auth/login` e seu emissor JWT já existem e são disponibilizados pelo API Gateway; esta feature não os cria nem os altera.
- A branch atual não contém um estado de autenticação frontend existente; esta feature estabelece o estado de sessão em memória para uso entre rotas.
- O link “Criar conta” aponta para `/register`; esta feature cria somente um placeholder informativo nessa rota, sem formulário, serviço ou persistência de cadastro.
- A imagem anexada é a referência visual primária desta tela. A composição usa hero fotográfico de tecnologia com marca, chamada e benefícios à esquerda, e formulário navy à direita; no mobile, o formulário tem prioridade visual.
- O protótipo também mostra recuperação de senha e login Google, mas esses fluxos não foram solicitados e não serão simulados nem incluídos sem contratos próprios.
- Nesta feature, a identidade navy/azul aplica-se somente ao login; a paleta existente do catálogo permanece inalterada e telas futuras poderão adotar a nova direção em features próprias.

## Out of Scope

- Criação de conta, ativação de conta e cadastro de usuário.
- Recuperação ou redefinição de senha.
- Login social, incluindo Google OAuth.
- Renovação de token, logout remoto, persistência de sessão após recarga ou opção “lembrar de mim”.
- Alterações no User Service, API Gateway ou contrato de autenticação existente.
- Redesign das páginas atuais de catálogo e produto.