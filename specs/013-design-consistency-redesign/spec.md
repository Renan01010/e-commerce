# Feature Specification: Consistência Visual e Redesign das Telas Existentes

**Feature Branch**: `013-design-consistency-redesign`

**Created**: 2026-10-03

**Status**: Draft

**Input**: User description: Feature 013 — aplicar o design system dark/tech da Feature 012 às demais telas existentes, exclusivamente no frontend, preservando integralmente os fluxos e contratos atuais.

## Contexto e escopo

A Home da Feature 012 é a referência visual principal. Esta feature adapta as demais telas destinadas ao usuário para que navegação, tipografia, superfícies, cores, controles e estados formem uma experiência coesa.

A Feature 013 é exclusivamente de apresentação frontend. A implementação não altera APIs, contratos, backend, serviços, persistência, autenticação, autorização ou regras de negócio. Não acrescenta capacidades de cadastro, checkout, pagamento, pedidos, avaliações, promoções ou outras funcionalidades.

O catálogo está explicitamente incluído no redesenho visual: sua apresentação deve adotar o padrão dark da Home, preservando busca, filtros, ordenação, paginação, estados e listagem existentes.

## Clarifications

### Session 2026-10-03

- Q: Como o login deve combinar com o header compartilhado da loja? → A: Manter o layout de autenticação próprio, integrar cores e tipografia da Home e não exibir o header da loja no login nem no estado de cadastro indisponível.
- Q: A Feature 013 deve redesenhar também toda a tela de catálogo para o padrão dark da Home, preservando filtros e listagem atuais? → A: Sim; incluir o catálogo e adaptar toda a apresentação visual ao padrão dark da Home, mantendo comportamento e controles.

### Inventário de superfícies e componentes existentes

| Superfície | Implementação atual | Reuso previsto |
|---|---|---|
| Home, catálogo, detalhe e carrinho | Rotas em `frontend/src/App.tsx` dentro de `StoreLayout` | Preservar o mesmo shell e header da Feature 012; evitar headers por página |
| Header e navegação | `StoreLayout` em `frontend/src/App.tsx` | Compartilhar entre Home, catálogo, detalhe, carrinho e fallback; autenticação mantém seu layout standalone e não exibe este header |
| Detalhe do produto | `ProductDetailPage` em `frontend/src/pages/ProductDetailPage.tsx` | Reusar tokens, breadcrumb, botões, superfícies e padrão de imagem/fallback já aplicados aos produtos |
| Autenticação | `LoginPage`, `LoginForm`, `PasswordField` e `CreateAccountLink` | Manter composição standalone de autenticação, aplicar cores e tipografia da Home e preservar componentes, validação, eventos, mensagens, carregamento e integração existentes |
| Cadastro | `RegisterUnavailablePage` em `frontend/src/pages/RegisterUnavailablePage.tsx` | Aplicar a identidade visual ao estado standalone existente; manter a rota indisponível, sem header da loja, campos ou fluxo de registro |
| Carrinho | `CartPage`, `CartItem` e `ClearCartConfirmation` | Reestilizar componentes existentes; preservar integralmente estado, ações, feedback, diálogo e valores confirmados pelo serviço |
| Tokens e estilos | `frontend/src/styles.css` e `frontend/src/pages/LoginPage.css` | Reutilizar fontes, cores dark/tech e acentos definidos pela Feature 012; não manter uma paleta concorrente de autenticação |

Antes de adicionar componentes visuais compartilhados, a implementação deve avaliar o que já pode ser aplicado diretamente. Uma extração de componente reutilizável de imagem/fallback é aceitável apenas se evitar duplicação entre detalhe, card e carrinho sem mudar seus dados ou comportamento.

## User Scenarios & Testing

### User Story 1 - Navegar pela loja com identidade visual consistente (Priority: P1)

Como visitante ou cliente, quero que as telas da TechStore compartilhem a mesma linguagem visual, com header e navegação comuns nas rotas da loja e layouts standalone para Login e cadastro indisponível, para reconhecer todas como partes do mesmo produto.

**Why this priority**: A navegação entre telas é o objetivo principal da feature e deve parecer contínua sem remover ou alterar destinos existentes.

**Independent Test**: Com APIs mockadas, navegar por `/`, `/catalog`, `/products/:id`, `/login` e `/cart`; confirmar header compartilhado nas rotas da loja, catálogo dark com controles funcionais, identidade visual integrada e ausência de regressão das rotas.

**Acceptance Scenarios**:

1. **Given** uma rota da loja ou o fallback, **When** a tela é renderizada, **Then** marca e navegação usam o header compartilhado com adaptação responsiva e busca conforme o comportamento existente; login e cadastro indisponível mantêm o layout standalone sem esse header.
2. **Given** um carrinho carregado pela sessão existente, **When** o usuário navega entre telas, **Then** o contador continua derivado do estado confirmado do `cartStore`.
3. **Given** uma rota não encontrada, **When** o fallback é exibido, **Then** sua apresentação usa as superfícies, tipografia, foco e CTA do mesmo sistema visual, mantendo os destinos atuais.

### User Story 2 - Consultar detalhes reais de um produto (Priority: P1)

Como pessoa interessada em um produto, quero ver imagem, identificação, preço, disponibilidade e ação de carrinho em uma tela alinhada à Home, sem informação comercial inventada.

**Why this priority**: A tela de detalhe conclui o fluxo de descoberta iniciado na Home e catálogo e contém a ação de carrinho existente.

**Independent Test**: Renderizar detalhe com respostas mockadas de produto e carrinho; validar conteúdo real, indisponibilidade, carregamento, erro, fallback de imagem e ação de adicionar sem alterar contratos ou estado de negócio.

**Acceptance Scenarios**:

1. **Given** um produto retornado pela API, **When** seu detalhe carrega, **Then** são apresentados somente atributos disponíveis no contrato atual, com breadcrumb e hierarquia visual coerentes com a Home.
2. **Given** `imageUrl` válido, ausente ou com falha de carregamento, **When** a área de mídia é exibida, **Then** é usada a imagem real ou um fallback visual de marca acessível, sem simular produto ou atributos.
3. **Given** o produto está indisponível ou o carrinho ainda não confirmou seus limites, **When** a tela é exibida, **Then** a ação respeita as regras atuais e apresenta indisponibilidade/carregamento sem prometer sucesso.
4. **Given** a busca do produto falha ou está carregando, **When** o estado correspondente é mostrado, **Then** a mensagem é compreensível e a navegação de retorno permanece acessível.

### User Story 3 - Entrar na conta sem alterar autenticação (Priority: P1)

Como usuário, quero fazer login em uma tela visualmente coerente com a loja, mantendo exatamente o fluxo de autenticação existente.

**Why this priority**: O login é necessário para acessar as operações protegidas do carrinho e deve ser parte do mesmo percurso visual.

**Independent Test**: Exercitar o formulário com validações e respostas mockadas de sucesso, erro e submissão pendente; confirmar os mesmos dados submetidos, integração, mensagens e transição de rota.

**Acceptance Scenarios**:

1. **Given** a tela de login, **When** ela é exibida, **Then** sua composição standalone é preservada, seus campos, labels, botão, mensagens, cores e tipografia seguem a identidade da Home, sem o header da loja, com teclado e foco visível.
2. **Given** credenciais inválidas no formulário ou rejeitadas pelo serviço, **When** a submissão acontece, **Then** validações e mensagens atuais são mantidas e anunciadas sem expor dados técnicos desnecessários.
3. **Given** uma submissão em andamento, **When** o formulário aguarda a resposta, **Then** loading e bloqueio contra submissão duplicada permanecem como hoje; após sucesso, a sessão e navegação continuam usando o fluxo existente.

### User Story 4 - Revisar e operar o carrinho com confiança (Priority: P1)

Como cliente, quero que os produtos, quantidades e resumo do carrinho sejam fáceis de compreender em uma interface consistente, sem alterar valores ou decisões do Cart Service.

**Why this priority**: Carrinho contém informações financeiras e mutações que devem permanecer autoritativas e compreensíveis em todas as telas.

**Independent Test**: Com respostas do Cart Service mockadas, validar itens disponíveis e indisponíveis, quantidades, valores conhecidos/desconhecidos, erro, vazio, remoção, limpeza e badge.

**Acceptance Scenarios**:

1. **Given** itens confirmados pelo Cart Service, **When** o carrinho é renderizado, **Then** nome, imagem, SKU quando disponível no resumo atual, quantidade, disponibilidade, preço unitário, subtotal e total são exibidos somente conforme os dados recebidos.
2. **Given** preço, subtotal ou total indisponível, **When** o resumo é mostrado, **Then** o estado é identificado textualmente e nenhum valor estimado é exibido.
3. **Given** uma alteração de quantidade, remoção, limpeza, falha ou carregamento, **When** a operação ocorre, **Then** ações pendentes, mensagens e confirmação atuais permanecem funcionais e visualmente alinhadas aos estados da Home.
4. **Given** o usuário ainda não está autenticado ou o carrinho está vazio, **When** acessa a rota, **Then** a mensagem e o CTA continuam disponíveis e utilizam os componentes/botões visuais compartilhados.

### User Story 5 - Reconhecer o estado indisponível de cadastro (Priority: P2)

Como visitante, quero que a rota de cadastro informe claramente sua indisponibilidade atual em uma tela integrada à loja.

**Why this priority**: A rota existe, mas não há fluxo de registro; sua apresentação deve ser consistente sem sugerir uma capacidade que o sistema não fornece.

**Independent Test**: Abrir `/register` e confirmar mensagem standalone de indisponibilidade, navegação de retorno ao login e ausência do header da loja, sem novos campos ou chamadas de API.

**Acceptance Scenarios**:

1. **Given** a rota `/register`, **When** ela é aberta, **Then** o usuário vê o estado standalone atual de indisponibilidade com identidade visual TechStore, sem o header da loja e com ação de retorno ao login.
2. **Given** a rota não possui formulário ou integração de cadastro, **When** esta feature é aplicada, **Then** nenhum fluxo, campo ou endpoint de cadastro é introduzido.

## Edge Cases

- O produto pode não ter imagem ou a URL de imagem pode falhar; o fallback deve permanecer compreensível e acessível.
- Os dados opcionais de produto, preço ou resumo do carrinho podem estar indisponíveis; não preencher lacunas com valores fictícios.
- Um produto pode ficar indisponível entre a navegação do catálogo e o detalhe/carrinho; preservar a resposta e os bloqueios existentes.
- O carrinho pode falhar ao carregar, atualizar ou limpar; manter retries e mensagens atuais sem exibir stack traces.
- Login pode estar validando, submetendo, falhar por credenciais/rede ou concluir com sucesso; nenhum estado deve ser perdido no redesign.
- Um viewport de 375 px pode exigir empilhamento de mídia, formulário, linhas de carrinho e resumo; não permitir overflow horizontal da página.
- O header pode ter poucas categorias disponíveis ou estar em rota com busca própria da Home; preservar sua lógica de dados e comportamento por rota.
- A rota de cadastro permanece indisponível, independentemente de existir link a partir do login.

## Requirements

### Functional Requirements

- **FR-001**: A feature MUST limitar suas alterações ao frontend e MUST NOT alterar backend, APIs, contratos, banco, migrations, JWT, OAuth, autenticação, autorização ou regras de negócio.
- **FR-002**: As rotas existentes `/`, `/catalog`, `/products/:id`, `/login`, `/register`, `/cart` e o fallback MUST preservar seus destinos e comportamento funcional.
- **FR-003**: Home, catálogo, detalhe, carrinho e fallback MUST reutilizar o header e navegação compartilhados da Feature 012, sem implementar headers duplicados por página; login e cadastro indisponível MUST preservar seu layout standalone e MUST NOT exibir o header da loja.
- **FR-004**: O badge do carrinho MUST continuar exibindo somente a quantidade derivada do estado confirmado do `cartStore`, respeitando sessão e estado de carregamento existentes.
- **FR-005**: O detalhe do produto MUST apresentar somente dados reais retornados pelas APIs existentes e MUST preservar navegação, loading, falha, indisponibilidade e integração de carrinho atuais.
- **FR-006**: As telas de detalhe, card e carrinho MUST usar mídia real quando disponível e fallback de marca quando ausente ou com falha, sem inventar representação de produto. O fallback MUST ter alternativa textual acessível apropriada.
- **FR-007**: O login MUST preservar integralmente campos, validação, estados, mensagens, submissão, sessão e navegação existentes; a alteração MUST ser apenas visual e de apresentação.
- **FR-008**: A rota de cadastro MUST continuar apresentando o estado de indisponibilidade existente; nenhum formulário, validação ou integração nova de cadastro poderá ser criado.
- **FR-009**: O carrinho MUST manter fontes e comportamento atuais para quantidades, limites, disponibilidade, preço unitário, subtotal, total, operações, erros, estado vazio e confirmação de limpeza.
- **FR-010**: Valores ou atributos ausentes MUST ser apresentados como indisponíveis quando aplicável; o frontend MUST NOT estimar, calcular ou inventar preços, totais, disponibilidade, categorias, avaliações, descontos ou benefícios comerciais.
- **FR-011**: Os estados atuais de carregamento, erro, vazio, indisponibilidade, sucesso e mutação pendente MUST manter distinção semântica e mensagens acessíveis em todas as telas adaptadas.
- **FR-012**: As telas de catálogo, detalhe e carrinho MUST adotar a apresentação dark/tech da Home e MUST reutilizar as fontes DM Sans, Space Grotesk e DM Mono; MUST NOT introduzir uma paleta concorrente. Login e cadastro indisponível MUST preservar a composição standalone com as mesmas cores e tipografia de marca.
- **FR-013**: A apresentação visual do catálogo MUST ser adaptada ao padrão dark da Home sem alterar busca, filtros, ordenação, paginação, listagem, estados ou rotas. A implementação MUST reutilizar botões, inputs, superfícies, bordas, foco, espaçamentos, ícones, tokens e breakpoints existentes sempre que adequados, evitando duplicação de componentes e CSS.
- **FR-014**: Marca, busca, navegação, Conta, Carrinho, contador e menu responsivo MUST continuar presentes de acordo com o comportamento definido para as rotas da loja na Feature 012; as telas standalone de autenticação preservam seus links de marca e retorno sem incorporar o header da loja.
- **FR-015**: As telas MUST permanecer utilizáveis e legíveis nos viewports de 1280 px, 1024 px, 768 px e 375 px, sem overflow horizontal do documento.
- **FR-016**: Controles MUST preservar labels, semântica de botões e links, foco visível, navegação por teclado, anúncios apropriados de loading/erro/sucesso e contraste compatível com os estados apresentados.
- **FR-017**: A suíte de testes MUST cobrir renderização, sucesso, validação, falha e loading de autenticação; dados e estados de detalhe; operações, totais disponíveis/indisponíveis e vazio de carrinho; e rotas, header e badge.
- **FR-018**: Testes MUST usar mocks dos clientes e estados existentes e MUST NOT depender de serviços externos reais.
- **FR-019**: Antes de introduzir qualquer novo componente visual, a implementação MUST identificar e avaliar o reuso de `StoreLayout`, `ProductCard`, `CartItem`, `ClearCartConfirmation`, `LoginForm`, `PasswordField` e `CreateAccountLink`.

### Key Entities

Esta feature não introduz entidades nem estado de negócio. Reutiliza a sessão, os produtos e as linhas/resumos de carrinho fornecidos pelos stores e serviços existentes, sem alterar seus formatos ou contratos.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Todas as rotas principais definidas nesta especificação apresentam a identidade visual da Home; rotas de loja compartilham marca/header/navegação, catálogo usa apresentação dark e telas de autenticação permanecem standalone, sem cabeçalhos duplicados por tela.
- **SC-002**: Nos viewports 1280 px, 1024 px, 768 px e 375 px, nenhuma tela adaptada causa overflow horizontal do documento e todas as ações principais permanecem acessíveis.
- **SC-003**: Todos os estados testados de login, detalhe e carrinho preservam o mesmo resultado funcional anterior, inclusive valores vindos do serviço, mensagens, sessão e mutações confirmadas.
- **SC-004**: Nenhuma tela adaptada apresenta preço estimado, total calculado localmente, disponibilidade presumida, desconto, avaliação, categoria ou benefício comercial inventado.
- **SC-005**: Todos os testes frontend relacionados à autenticação, detalhe, carrinho e navegação passam com serviços externos mockados.
- **SC-006**: O build TypeScript/Vite termina sem erros e os estilos continuam usando as fontes e direção visual estabelecidas pela Feature 012.

## Assumptions

- A Home da Feature 012 é a referência visual vigente; esta feature não redefine sua identidade, conteúdo ou regras.
- O shell `StoreLayout` e seu header são a fonte compartilhada de navegação para as rotas da loja; login e cadastro indisponível preservam layouts standalone, sem incorporar esse header, mas reutilizam a identidade visual da Home.
- A categoria listada no detalhe e os dados exibidos no carrinho continuam sujeitos às respostas dos serviços e aos contratos frontend atuais.
- `/register` não oferece criação de conta atualmente; a indisponibilidade é comportamento intencional a preservar nesta feature.
- Alterações são restritas à apresentação e devem evitar mudanças em stores, clientes, estado de autenticação ou carrinho, salvo correção estritamente necessária e sem alteração de semântica.
