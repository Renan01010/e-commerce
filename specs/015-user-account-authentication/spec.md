# Feature Specification: Conta do Usuário e Autenticação

**Feature Branch**: `015-user-account-authentication`

**Created**: 2026-10-03

**Status**: Draft

**Input**: User description: Feature 015 — User Account & Authentication

## Contexto e escopo

Esta feature completa a experiência de conta da TechStore, uma vitrine comercial cujo percurso é Home → Catálogo → Produto → Carrinho → Interesse/Aquisição → Contato comercial. O carrinho permanece associado à conta autenticada e continua sendo responsabilidade do Cart Service.

O escopo inclui cadastro, confirmação de e-mail, login, logout, recuperação e alteração de senha, consulta e edição do perfil, proteção de recursos de conta e continuidade do carrinho entre sessões. As telas devem seguir o design system dark/tech estabelecido pelas Features 012 e 013.

Esta feature não transforma a vitrine em um fluxo de compra transacional. Checkout, pagamentos, pedidos, envio, rastreamento e reserva de estoque permanecem fora do escopo.

## Levantamento inicial do repositório

Este inventário registra o estado observado durante a especificação; a fase de planejamento deve confirmar os contratos e configurações completos antes de propor alterações.

### O que já existe

- O User Service atual já oferece cadastro público e login. O cadastro cria usuários com a role `USER`; as operações administrativas existentes continuam distintas.
- O User Service já usa BCrypt para senhas e emite JWT pelo mecanismo existente. A especificação aprovada da Feature 003 documenta HS256, claim `roles`, validade de 24 horas e `TECHSTORE_JWT_SECRET`.
- A tabela `users`, criada pela migration V1, contém `id`, `email`, `password_hash`, `role`, `active`, `created_at` e `updated_at`. A entidade atual não contém nome nem estado de verificação de e-mail.
- O Gateway já encaminha `/api/auth/**` e `/api/users/**` ao User Service, removendo o prefixo `/api` antes do encaminhamento.
- O frontend já possui login, serviço de autenticação e um estado de sessão mantido em memória. A rota `/register` ainda apresenta indisponibilidade de cadastro.
- O frontend já carrega o carrinho quando há sessão autenticada; o estado existente do Cart Service/store é a fonte do badge e dos itens.
- O projeto tem migrations versionadas com Flyway; a V1 do User Service não deve ser modificada.

### O que precisa ser alterado ou criado

- Evoluir o User Service existente para os fluxos que ainda não estão presentes, preservando cadastro, login, claims e compatibilidade JWT existentes.
- Acrescentar ao modelo de conta apenas os dados necessários para nome, verificação e perfil, após confirmar o schema efetivamente usado.
- Criar os fluxos e telas ausentes de verificação de e-mail, recuperação e redefinição de senha, perfil, alteração de senha e proteção das rotas de conta.
- Integrar o envio dos e-mails de verificação e recuperação através de um provedor externo configurável, sem servidor de e-mail próprio.
- Atualizar a navegação para distinguir visitante e usuário autenticado e permitir logout. Reutilizar o estado e os componentes já existentes do carrinho, sem duplicar a lógica de quantidade.
- Atualizar contratos e documentação somente onde forem necessários; não duplicar endpoints existentes nem introduzir mudanças incompatíveis sem decisão explícita.

### Migrations, configuração, riscos e testes a tratar no plano

- **Migrations**: confirmar migrations e schema no ambiente de destino. Se os dados ainda não existirem, criar migrations Flyway novas e versionadas para os campos e registros de tokens necessários; nunca alterar migrations já aplicadas.
- **Configuração**: manter `TECHSTORE_JWT_SECRET` e as configurações de conexão atuais fora do código. Documentar as variáveis de ambiente do provedor de e-mail, incluindo host, porta, usuário, senha, remetente e nome do remetente quando aplicável. Não versionar credenciais.
- **Riscos**: uma sessão JWT sem mecanismo de revogação continua válida até sua expiração depois que o frontend executa logout; tokens de uso único e respostas uniformes são necessários para reduzir replay e enumeração de contas; falha ou configuração incorreta de e-mail pode impedir a confirmação/recuperação; alterações no schema do usuário devem preservar o cadastro e os tokens atuais.
- **Testes necessários**: cobrir cadastro e validação; login e compatibilidade JWT; confirmação, expiração, reuso e reenvio de tokens; recuperação e redefinição; alteração de senha; perfil e autorização; logout; isolamento entre usuários no carrinho; rotas frontend protegidas e públicas; estados de erro/loading; adapter de e-mail simulado, sem depender de SMTP real.

## Restrições e limites

- Evoluir o User Service existente; não criar outro serviço de usuários.
- Preservar o mecanismo JWT e os contratos existentes sempre que possível.
- O User Service continua responsável por conta, autenticação, credenciais, verificação, recuperação e perfil. O Cart Service continua sendo a fonte de verdade do carrinho. O Product Service não deve ser alterado.
- A identificação do usuário e o isolamento de dados devem ser aplicados no backend; ocultar telas no frontend não substitui autorização.
- Usar envio externo de e-mail configurável. Não criar servidor próprio, newsletter ou mensagens de marketing.
- Nunca armazenar senha em texto puro. Não persistir tokens de verificação/recuperação em texto puro.
- Nunca expor ao frontend hash de senha, hash ou valor interno de token, segredo JWT, credenciais de e-mail ou dados internos desnecessários.
- Não registrar senha, token, JWT, credenciais nem conteúdo sensível em logs; manter o correlationId já utilizado.
- Considerar limitação de tentativas nos fluxos de login, recuperação e reenvio. Se não houver mecanismo existente, documentar a necessidade sem introduzir infraestrutura alheia ao escopo.
- Não criar usuários fictícios para simular contas reais.
- OAuth social só pode ser preservado caso já exista; não faz parte desta feature.

## User Scenarios & Testing

### User Story 1 - Criar uma conta (Priority: P1)

Como visitante, quero criar uma conta com meu nome, e-mail e senha para usar os recursos personalizados da loja.

**Why this priority**: O cadastro é a entrada para os fluxos de conta e para a associação persistente do carrinho.

**Independent Test**: Enviar um cadastro válido e outro inválido; confirmar criação de uma conta USER, retorno seguro, rejeição de e-mail duplicado e início do fluxo de confirmação.

**Acceptance Scenarios**:

1. **Given** os campos obrigatórios preenchidos com nome válido, e-mail válido e senha conforme a política, **When** o visitante envia senhas coincidentes, **Then** uma conta é criada, a senha é armazenada de forma segura, o e-mail começa como não verificado, a interface confirma o cadastro e informa que o acesso depende da confirmação do e-mail.
2. **Given** um e-mail já utilizado, **When** o visitante tenta cadastrar outra conta com esse e-mail, **Then** o cadastro é recusado sem criar outra conta.
3. **Given** campos ausentes, e-mail inválido, senha fora da política ou confirmação divergente, **When** o visitante envia o formulário, **Then** os erros são apresentados junto aos campos e nenhum cadastro é criado.
4. **Given** um cadastro público, **When** o cliente envia campos adicionais para escolher role ou estado da conta, **Then** não pode atribuir privilégios ou contornar os controles do serviço.

### User Story 2 - Confirmar e-mail (Priority: P1)

Como usuário recém-cadastrado, quero confirmar meu e-mail e poder pedir outro link quando necessário para validar o endereço associado à minha conta.

**Why this priority**: A confirmação valida o canal necessário para comunicações de conta e reduz o uso de endereços incorretos.

**Independent Test**: Usar um e-mail de teste capturado por um adapter simulado; concluir uma confirmação válida e verificar rejeição de token expirado ou reutilizado e reenvio controlado.

**Acceptance Scenarios**:

1. **Given** uma conta ainda não verificada, **When** o cadastro ou um reenvio é aceito, **Then** o usuário recebe instruções com um link temporário de confirmação e uma mensagem visual que não revela dados internos.
2. **Given** um token válido e não utilizado, **When** o usuário confirma o endereço, **Then** a conta passa a indicar e-mail verificado e o token deixa de ser utilizável.
3. **Given** token expirado, inválido ou já utilizado, **When** o usuário tenta confirmar o endereço, **Then** nenhuma conta é alterada e a interface oferece orientação segura para solicitar novo link.
4. **Given** qualquer token de confirmação armazenado, **When** os dados persistidos são inspecionados, **Then** não é possível recuperar o token original a partir do valor armazenado.

### User Story 3 - Entrar na conta (Priority: P1)

Como usuário cadastrado, quero entrar com e-mail e senha para acessar minha conta e o carrinho associado.

**Why this priority**: Login estabelece a identidade autenticada necessária para todos os recursos privados e para recuperar o carrinho do próprio usuário.

**Independent Test**: Submeter credenciais válidas e inválidas, confirmar a sessão frontend com o JWT atual, verificar acesso autorizado e garantir que falhas não autenticam nem revelam se o e-mail existe.

**Acceptance Scenarios**:

1. **Given** credenciais válidas para uma conta ativa e com e-mail verificado, **When** o usuário entra, **Then** o mecanismo JWT existente autentica a conta e a sessão fica disponível durante a navegação da aplicação.
2. **Given** e-mail inexistente, senha incorreta, conta inativa ou conta ainda não verificada, **When** o usuário tenta entrar, **Then** recebe uma mensagem genérica equivalente, sem enumeração da condição da conta.
3. **Given** um JWT ausente, inválido ou expirado, **When** o usuário acessa recurso protegido, **Then** o backend nega a solicitação sem retornar dados privados.
4. **Given** uma falha de rede ou resposta inválida, **When** o formulário de login é enviado, **Then** nenhuma sessão é criada, os campos não sensíveis são preservados e uma nova tentativa é possível.

### User Story 4 - Sair da conta (Priority: P1)

Como usuário autenticado, quero sair da minha conta neste dispositivo sem apagar meu carrinho persistido.

**Why this priority**: O usuário precisa controlar a sessão local sem perder dados de compra já associados à conta.

**Independent Test**: Estabelecer uma sessão e carrinho, executar logout e confirmar remoção do estado autenticado e dependente; entrar novamente com a mesma conta e verificar o carrinho original.

**Acceptance Scenarios**:

1. **Given** uma sessão autenticada, **When** o usuário escolhe “Sair”, **Then** o estado de autenticação e os estados dependentes são limpos e ele retorna a uma área pública.
2. **Given** que o usuário executou logout, **When** a conta é autenticada novamente, **Then** o carrinho persistido no backend permanece associado à mesma conta e é recuperado.
3. **Given** um JWT stateless emitido anteriormente, **When** o frontend executa logout sem um mecanismo de revogação já existente, **Then** o frontend remove o token da sessão local e não afirma que o token foi revogado no servidor.

### User Story 5 - Recuperar e redefinir senha (Priority: P1)

Como usuário que esqueceu a senha, quero solicitar um link de recuperação e definir uma nova senha com segurança.

**Why this priority**: Sem recuperação segura, o usuário pode perder permanentemente o acesso à conta.

**Independent Test**: Solicitar recuperação para conta existente e inexistente, capturar o e-mail pelo adapter simulado e testar token válido, expirado e já utilizado.

**Acceptance Scenarios**:

1. **Given** um e-mail informado para recuperação, **When** o usuário envia a solicitação, **Then** a resposta pública não permite distinguir de forma simples entre conta existente e inexistente.
2. **Given** uma conta elegível, **When** a recuperação é aceita, **Then** o endereço associado recebe um link temporário sem que o token original seja armazenado em texto puro.
3. **Given** token válido e não utilizado e uma nova senha válida, **When** o usuário conclui a redefinição, **Then** a senha é atualizada com hash seguro e o token é invalidado.
4. **Given** token expirado, inválido ou utilizado, **When** o usuário tenta redefinir a senha, **Then** nenhuma senha é alterada e o usuário recebe orientação segura para reiniciar o fluxo.
5. **Given** solicitação de recuperação para e-mail não cadastrado ou indisponibilidade do provedor, **When** o sistema processa o pedido, **Then** a resposta visível preserva a política antienumeração e a falha operacional pode ser diagnosticada sem registrar dados sensíveis.

### User Story 6 - Consultar e editar perfil (Priority: P2)

Como usuário autenticado, quero consultar meus dados básicos e atualizar meu nome para manter minha conta correta.

**Why this priority**: A conta precisa oferecer uma visão pessoal útil sem expor credenciais nem permitir mudanças de identidade não verificadas.

**Independent Test**: Consultar e alterar o perfil autenticado; confirmar persistência do nome, preservação do e-mail/verificação e rejeição de acesso sem autenticação ou à conta de outra pessoa.

**Acceptance Scenarios**:

1. **Given** uma sessão válida, **When** o usuário abre “Minha conta”, **Then** vê pelo menos nome, e-mail, estado de verificação e informações básicas permitidas.
2. **Given** uma sessão válida e um nome aceitável, **When** o usuário salva uma alteração, **Then** somente os campos permitidos são atualizados e o resultado é refletido na interface.
3. **Given** uma tentativa de alterar e-mail, role ou estado administrativo através da edição básica de perfil, **When** a solicitação é processada, **Then** esses campos não são alterados por esse fluxo.
4. **Given** uma solicitação sem sessão válida, **When** o usuário tenta consultar ou alterar perfil, **Then** o backend nega o acesso e nenhum dado de perfil é revelado.

### User Story 7 - Alterar senha autenticado (Priority: P2)

Como usuário autenticado, quero alterar minha senha informando a senha atual para proteger minha conta.

**Why this priority**: O usuário deve conseguir substituir suas credenciais sem depender de uma solicitação de recuperação.

**Independent Test**: Alterar senha com credencial atual correta e tentar com senha atual incorreta; validar nova autenticação e armazenamento sem texto puro.

**Acceptance Scenarios**:

1. **Given** senha atual correta, nova senha válida e confirmação coincidente, **When** o usuário solicita alteração, **Then** o hash da nova senha substitui a credencial anterior.
2. **Given** senha atual incorreta, nova senha inválida ou confirmação divergente, **When** o usuário envia a alteração, **Then** a senha existente permanece inalterada e os erros são apresentados com segurança.
3. **Given** uma alteração de senha concluída, **When** credenciais são usadas posteriormente, **Then** a senha nova é aceita e a senha anterior não autentica mais.

### User Story 8 - Reutilizar o carrinho da conta (Priority: P1)

Como usuário autenticado, quero manter meus itens de carrinho ao sair e voltar, sem expor o carrinho a outros usuários.

**Why this priority**: A continuidade do carrinho é parte central da vitrine e depende de identidade autenticada consistente.

**Independent Test**: Adicionar itens como usuário A, sair e entrar novamente como A; depois entrar como usuário B e verificar que nenhum item de A é visível.

**Acceptance Scenarios**:

1. **Given** o usuário A autenticado e um carrinho persistido, **When** A sai e autentica novamente, **Then** os mesmos dados de carrinho são recuperados pelo serviço existente.
2. **Given** o usuário B autenticado, **When** B consulta ou altera o próprio carrinho, **Then** nenhum dado ou item de A é lido ou alterado.
3. **Given** que o header exibe quantidade de itens, **When** a sessão e o carrinho mudam, **Then** o badge continua derivado do estado confirmado existente, sem cálculo paralelo ou sucesso presumido.

### User Story 9 - Navegar e acessar recursos conforme a sessão (Priority: P1)

Como visitante ou usuário autenticado, quero ver opções de navegação adequadas à minha sessão e ter recursos privados protegidos.

**Why this priority**: A navegação deve tornar os fluxos de conta encontráveis e impedir acesso anônimo ou entre contas.

**Independent Test**: Navegar pelas rotas públicas e privadas sem sessão, com sessão válida e após expiração; verificar header, redirecionamento, destino público de retorno e autorização também no backend.

**Acceptance Scenarios**:

1. **Given** um visitante, **When** uma tela pública é exibida, **Then** o header oferece entrada e criação de conta e as páginas públicas da loja continuam acessíveis.
2. **Given** um usuário autenticado, **When** uma tela da loja é exibida, **Then** o header oferece identidade da conta, acesso a “Minha conta” e logout.
3. **Given** um visitante ou uma sessão expirada, **When** tenta abrir uma rota privada de conta, **Then** não vê conteúdo privado e recebe encaminhamento para autenticar-se.
4. **Given** uma requisição direta a um endpoint protegido sem JWT válido, **When** chega ao serviço responsável, **Then** é negada no backend mesmo que o frontend seja contornado.
5. **Given** as rotas públicas atuais de Home, catálogo, detalhe, login e cadastro, **When** um usuário autenticado ou visitante navega entre elas, **Then** elas permanecem acessíveis conforme as regras atuais.

## Edge Cases

- E-mail com espaços ou letras maiúsculas deve continuar seguindo a normalização existente antes de comparação e persistência.
- Solicitações repetidas de cadastro, confirmação ou recuperação não devem criar tokens simultâneos reutilizáveis nem revelar se um e-mail existe.
- Um token temporário não pode ser aceito após expirar, após uso, nem após outro token vigente substituí-lo, conforme a política decidida no planejamento.
- Falha do provedor de e-mail não pode produzir uma resposta que afirme falsamente que a mensagem foi entregue; o usuário deve receber orientação segura para tentar novamente.
- Um erro durante a redefinição não pode invalidar uma senha correta sem concluir a operação de forma consistente.
- Campos desconhecidos no cadastro ou perfil não podem conceder role, ativar contas, trocar e-mail ou alterar dados de outra conta.
- Expiração de sessão durante consulta, edição de perfil ou carrinho deve limpar o estado local e não preservar conteúdo privado visível.
- Logout nunca deve apagar o carrinho armazenado no backend.
- Layout responsivo e acessibilidade por teclado devem manter campos, mensagens, estados de carregamento e ações essenciais disponíveis nas telas de conta.

## Requirements

### Functional Requirements

- **FR-001**: O sistema DEVE evoluir o User Service existente e preservar o cadastro, login, roles, contratos compatíveis e o mecanismo JWT já utilizado.
- **FR-002**: O cadastro público DEVE aceitar nome, e-mail, senha e confirmação de senha; validar obrigatoriedade, formato, política de senha e coincidência da confirmação; e criar somente uma conta USER.
- **FR-003**: O sistema DEVE recusar e-mail já cadastrado sem criar outra conta e sem revelar hashes, senhas ou dados internos.
- **FR-004**: O sistema DEVE manter senha apenas em forma de hash seguro em repouso e comparar credenciais sem expor a senha em respostas, logs ou dados do frontend.
- **FR-005**: O sistema DEVE indicar inicialmente que o e-mail da conta não foi verificado e oferecer confirmação por link temporário e solicitação de reenvio.
- **FR-006**: Tokens de confirmação e recuperação DEVEM ser temporários, de uso único, invalidados após uso e armazenados sem persistir seus valores originais em texto puro.
- **FR-007**: O sistema DEVE permitir login com e-mail e senha através do mecanismo JWT existente somente para contas ativas com e-mail verificado e manter a sessão no frontend de acordo com o comportamento atual, sem persistir senha.
- **FR-008**: Falhas de autenticação DEVEM apresentar resposta que não diferencie e-mail inexistente, senha incorreta ou estado de conta não elegível.
- **FR-009**: O sistema DEVE permitir logout no dispositivo removendo sessão e estado dependente no frontend sem apagar o carrinho persistido; não deve afirmar revogação de JWT stateless se não existir mecanismo atual de revogação.
- **FR-010**: O sistema DEVE oferecer solicitação de recuperação, emissão de instruções por e-mail e redefinição com token de uso único e senha válida.
- **FR-011**: Respostas de solicitação de recuperação DEVEM evitar enumeração de usuários independentemente da existência da conta solicitada.
- **FR-012**: O sistema DEVE permitir que o usuário autenticado altere sua senha mediante validação da senha atual, política da nova senha e confirmação.
- **FR-013**: O perfil autenticado DEVE permitir consulta de nome, e-mail, estado de verificação e demais informações básicas não sensíveis explicitamente aprovadas.
- **FR-014**: O usuário autenticado DEVE poder editar inicialmente apenas o nome por meio do fluxo de perfil. Alteração de e-mail, role e estado administrativo não faz parte da edição básica.
- **FR-015**: Recursos de conta DEVEM verificar identidade e autorização no backend; rotas privadas de conta DEVEM exigir sessão válida no frontend e no serviço correspondente.
- **FR-016**: Dados e operações do carrinho DEVEM permanecer isolados pela identidade autenticada e continuar sob responsabilidade do Cart Service e de seus contratos existentes.
- **FR-017**: O header DEVE apresentar opções de entrada/cadastro para visitantes e opções de conta/logout para usuários autenticados, preservando o badge existente do carrinho e sua fonte de estado.
- **FR-018**: As telas de conta DEVEM seguir a identidade dark/tech das Features 012 e 013, ser responsivas e acessíveis e apresentar estados de carregamento, sucesso, erro, validação, sessão expirada e usuário não autenticado quando aplicáveis.
- **FR-019**: O envio de confirmação, reenvio e recuperação DEVE usar um provedor externo configurável através de variáveis de ambiente, com substituto de teste que não dependa de servidor SMTP real.
- **FR-020**: A solução DEVE manter a separação de responsabilidades atual: o User Service controla dados da conta, o Cart Service controla carrinhos, o Product Service mantém o catálogo e o Gateway permanece como entrada externa.
- **FR-021**: Contratos existentes DEVEM ser analisados e reutilizados antes de criar endpoints; caminhos e payloads novos DEVEM seguir os contratos reais e não duplicar nem quebrar operações existentes.
- **FR-022**: Migrations já aplicadas NÃO DEVEM ser alteradas; mudanças de schema necessárias DEVEM ser aditivas, versionadas e compatíveis com os dados existentes.
- **FR-023**: Segredos JWT, credenciais do provedor de e-mail e demais segredos DEVEM ser externos ao código e documentados como configuração de ambiente, sem valores reais em documentação versionada.
- **FR-024**: Logs DEVEM manter correlationId e registrar apenas eventos úteis de diagnóstico, sem senha, token, JWT, credenciais ou conteúdo sensível.
- **FR-025**: Login, recuperação e reenvio DEVEM ser avaliados quanto à limitação de tentativas. Se não houver solução existente, a necessidade DEVE ser registrada para decisão sem criar infraestrutura desnecessária.
- **FR-026**: A implementação DEVE incluir testes automatizados para fluxos críticos de backend e frontend, incluindo autorização, isolamento de usuários, estados de falha e envio por adapter simulado.

### Key Entities

- **Conta de usuário**: Identidade da pessoa na loja; contém identificador, nome, e-mail, hash de senha, role, estado ativo, estado de verificação e datas de criação/atualização.
- **Token de verificação de e-mail**: Credencial temporária vinculada a uma conta, com valor protegido em repouso, validade, momento de uso e criação.
- **Token de redefinição de senha**: Credencial temporária vinculada a uma conta, com valor protegido em repouso, validade, momento de uso e criação.
- **Sessão autenticada**: Estado temporário no dispositivo baseado no JWT atual e sua validade; o logout remove a sessão local conforme o mecanismo existente.
- **Carrinho**: Conjunto persistido de itens associado à identidade autenticada e mantido pelo Cart Service.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Todos os fluxos definidos de cadastro, login, logout, verificação e recuperação podem ser concluídos em testes de aceitação sem autenticação simulada nem dependência de servidor de e-mail real.
- **SC-002**: Em 100% dos testes de autorização, visitantes e usuários autenticados não acessam dados privados de outra conta.
- **SC-003**: Em 100% dos testes de ciclo de token, tokens expirados ou já utilizados são recusados e tokens originais não são encontrados nos dados persistidos.
- **SC-004**: Em 100% dos testes de respostas públicas de login e recuperação, a resposta não revela se um e-mail existe ou qual credencial falhou.
- **SC-005**: Após logout e novo login da mesma conta, o carrinho persistido reaparece sem perda; contas diferentes não compartilham itens em todos os testes de isolamento.
- **SC-006**: Todas as telas de conta previstas apresentam estados de carregamento, sucesso, erro e validação aplicáveis e permanecem utilizáveis em viewport móvel e desktop nos testes de interface.
- **SC-007**: Nenhum teste, resposta pública ou log inspecionado contém senha em texto puro, hash de senha, token temporário, credencial de e-mail ou segredo JWT.

## Assumptions

- A role padrão de cadastro público continua sendo USER; a administração de roles permanece fora dos fluxos de perfil.
- Contas recém-criadas não podem iniciar sessão até que o e-mail seja confirmado.
- O escopo inicial de edição de perfil é somente o nome. Edição de e-mail fica adiada até que um fluxo explícito de nova verificação seja aprovado.
- O frontend mantém o comportamento atual de sessão em memória. Logout limpa o estado local; revogação imediata de JWT não é presumida sem mecanismo já existente.
- Políticas e duração exatas de senha e tokens temporários serão alinhadas ao User Service e documentadas no planejamento sem enfraquecer a segurança ou quebrar a compatibilidade existente.
- O provedor e o formato de entrega de e-mail serão escolhidos no planejamento a partir das opções suportadas no ambiente de implantação; credenciais serão fornecidas exclusivamente pelo ambiente.
- Checkout, pagamento, pedidos, rastreamento, reserva de estoque, newsletter, marketing, fidelidade, avaliações, favoritos e OAuth social novo estão fora do escopo.
