# Especificação da Feature: Serviço de Usuários e Autenticação

**Branch da Feature**: `003-user-auth`
**Criado em**: 2026-09-27
**Status**: Rascunho — clarificações consolidadas, aguardando aprovação final

## Cenários de Usuário e Testes

### História de Usuário 1 - Login do Usuário (Prioridade: P1)

Como um usuário cadastrado, quero me autenticar usando e-mail e senha para que eu possa acessar os recursos protegidos da TechStore.

**Cenários de Aceitação**:

1. Dado um usuário cadastrado, ativo e com credenciais válidas, quando enviar e-mail e senha para `POST /api/auth/login`, então o sistema deve retornar um JWT válido e sua duração.
2. Dado um usuário com senha incorreta, e-mail não cadastrado ou conta inativa, quando tentar autenticar, então o sistema deve responder `401 Unauthorized` com mensagem genérica idêntica, sem revelar qual condição ocorreu.

### História de Usuário 2 - Criar Usuário Administrador (Prioridade: P1)

Como um administrador, quero criar usuários com diferentes funções para que as operações protegidas possam ser controladas.

**Cenários de Aceitação**:

1. Dado um ADMIN autenticado, quando criar um usuário com dados válidos, então o usuário deve ser persistido com a role solicitada.
2. Dado um ADMIN autenticado, quando atualizar um usuário, então somente sua role e/ou estado ativo poderão ser alterados.
3. Dado um e-mail já cadastrado, quando um usuário for criado com esse e-mail, então a solicitação deve retornar `409 Conflict`.
4. Dado um usuário ADMIN, quando autenticar com sucesso, então o JWT deve conter a claim `roles` com `ADMIN`.
5. Dado que uma alteração removeria ou desativaria o último ADMIN ativo, então a operação deve ser rejeitada.

### História de Usuário 3 - Validar JWT (Prioridade: P1)

Como um serviço protegido, quero validar tokens JWT para que somente usuários autenticados possam acessar operações protegidas.

**Cenários de Aceitação**:

1. Dado um JWT válido, quando um endpoint protegido for acessado, então o serviço deve validar a assinatura, identidade e role do usuário.
2. Dado um JWT expirado, inválido ou com assinatura incompatível, quando um endpoint protegido for acessado, então a solicitação deve retornar `401 Unauthorized`.
3. Dado um JWT válido sem role ADMIN, quando uma operação administrativa for acessada, então a solicitação deve retornar `403 Forbidden`.

## Requisitos Funcionais

- O sistema DEVE armazenar os usuários no banco de dados do User Service.
- O User Service DEVE possuir e controlar seu próprio banco de dados.
- As senhas DEVEM ser armazenadas utilizando hash BCrypt.
- A autenticação DEVE utilizar JWT.
- O JWT DEVE conter a identidade do usuário na claim `sub` e sua função na claim `roles`.
- O JWT DEVE ser assinado com HS256 e expirar após 24 horas.
- As funções suportadas DEVEM incluir USER e ADMIN.
- O e-mail DEVE ser único.
- E-mails DEVEM ser normalizados com `trim` e letras minúsculas antes de consultar ou persistir.
- Senhas DEVEM possuir entre 8 e 72 caracteres.
- Endpoints protegidos DEVEM exigir um JWT válido.
- Endpoints administrativos DEVEM exigir a função ADMIN.
- O API Gateway DEVE encaminhar as requisições de autenticação para o User Service.
- O Product Service DEVE continuar realizando o controle de autorização das operações administrativas de produtos.
- Informações sensíveis, como senhas e chaves secretas do JWT, NÃO DEVEM ser registradas nos logs.

## Fora do Escopo

- Login com OAuth2/redes sociais
- Recuperação de senha
- Verificação de e-mail
- Autenticação multifator (MFA)
- Refresh tokens
- Gerenciamento de perfil do usuário além da autenticação e gerenciamento de funções

## Clarificações e Decisões

### P0.1 — Bootstrap do primeiro ADMIN

O primeiro usuário ADMIN poderá ser criado automaticamente pelo User Service utilizando as variáveis de ambiente:

- `INITIAL_ADMIN_EMAIL`
- `INITIAL_ADMIN_PASSWORD`

O bootstrap será idempotente. O usuário somente será criado caso o e-mail normalizado ainda não exista. Se o usuário já existir, o processo não deverá sobrescrever sua senha, role ou demais dados. Se ambas as variáveis estiverem ausentes, o serviço inicia sem criar o ADMIN inicial; se somente uma estiver definida, a inicialização falha sem revelar seu valor.

### P0.2 — Cadastro e atribuição de roles

O cadastro público estará disponível através de:

`POST /api/auth/register`

Usuários criados através do cadastro público receberão obrigatoriamente a role `USER`.

A role `ADMIN` somente poderá ser atribuída por um usuário ADMIN autenticado.

O cliente não poderá definir ou alterar sua própria role através do cadastro público.

### P0.3 — API Gateway

O API Gateway será o ponto de entrada público para autenticação.

Rotas públicas:

- `POST /api/auth/register`
- `POST /api/auth/login`

Rotas administrativas:

- `POST /api/users`
- `PUT /api/users/{id}`

O Gateway encaminhará as requisições `/api/auth/**` e `/api/users/**` para o User Service através da rede privada.

O prefixo público `/api` será removido antes da requisição chegar ao User Service.

### P0.4 — Validação do JWT

O User Service será responsável por autenticar usuários e emitir JWTs.

Cada serviço protegido será responsável por validar os JWTs recebidos e aplicar suas próprias regras de autorização.

O Product Service continuará responsável por validar JWTs e exigir a role `ADMIN` nas operações administrativas.

O API Gateway será responsável pelo roteamento e não assumirá as regras específicas de autorização dos serviços.

### P0.5 — Assinatura do JWT

O User Service emitirá JWT utilizando o algoritmo HS256.

A chave será fornecida através da variável de ambiente:

`TECHSTORE_JWT_SECRET`

O Product Service utilizará a mesma chave para validar os tokens.

O JWT deverá utilizar a claim:

`roles`

A chave secreta não poderá ser armazenada no código-fonte, versionada no Git ou registrada nos logs.

### P0.6 — Banco de dados

O User Service terá seu próprio banco lógico PostgreSQL, separado do banco do Product Service.

O User Service será o único responsável pelos dados dos usuários.

O schema será gerenciado através do Flyway.

As credenciais e informações de conexão serão fornecidas através de variáveis de ambiente.

O banco poderá utilizar a mesma instância PostgreSQL da infraestrutura atual, desde que o database lógico `techstore_user_db` permaneça separado do database do Product Service. A conexão será configurada por `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.

### P1.1 — Modelo do usuário

A entidade de usuário terá os seguintes campos:

- `id`: UUID, chave primária.
- `email`: obrigatório, único e normalizado para minúsculas.
- `passwordHash`: obrigatório, armazenado utilizando BCrypt.
- `role`: obrigatório, podendo ser `USER` ou `ADMIN`.
- `active`: obrigatório, com valor padrão `true`.
- `createdAt`: timestamp em UTC.
- `updatedAt`: timestamp em UTC.

Cada usuário terá uma única role.

O modelo não incluirá inicialmente nome, telefone, CPF, endereço ou outros dados de perfil.

### P1.2 — Cadastro de usuário

O endpoint público será:

`POST /api/auth/register`


O request não aceita role nem estado ativo. O User Service normaliza o e-mail e cria a conta ativa com role `USER`. Em sucesso, responde `201 Created` com `id`, `email`, `role`, `active`, `createdAt` e `updatedAt`; senha e hash nunca são retornados. Formato de e-mail ou senha inválido retorna `400 Bad Request`; e-mail duplicado retorna `409 Conflict`.

### P1.3 — Login e resposta JWT

O login público será `POST /api/auth/login`, com o seguinte request:

```json
{
  "email": "usuario@email.com",
  "password": "SenhaForte123"
}
```

Em sucesso, responde `200 OK`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

O JWT terá `sub` com o UUID do usuário, `roles` como array de roles, `iat` e `exp`. A duração é de 24 horas. Não haverá refresh token. As claims `iss` e `aud` não são exigidas nesta versão.

### P1.4 — Criação e atualização administrativa

`POST /api/users` exige JWT válido com role `ADMIN`. O request contém e-mail, senha inicial e role (`USER` ou `ADMIN`):

```json
{
  "email": "novo.usuario@email.com",
  "password": "SenhaForte123",
  "role": "USER"
}
```

O endpoint responde `201 Created` com os dados públicos do usuário, sem senha ou hash. Somente ADMIN pode atribuir a role `ADMIN`.

`PUT /api/users/{id}` também exige ADMIN e aceita somente `role` e/ou `active`, alterando apenas os campos fornecidos. E-mail e senha não podem ser alterados por esse endpoint. Pelo menos um campo atualizável deve ser enviado. Um ADMIN não pode remover a própria role ADMIN nem desativar a última conta ADMIN ativa; operações que violariam essa regra são rejeitadas com `409 Conflict`.

### P1.5 — Validação e respostas de erro

E-mail é validado por formato e normalizado com `trim` e lowercase antes da verificação de unicidade. Senhas devem ter entre 8 e 72 caracteres e ser armazenadas exclusivamente com BCrypt.

E-mail inexistente, senha incorreta e conta inativa produzem a mesma resposta genérica `401 Unauthorized`, sem permitir enumeração de contas. Token ausente, inválido, expirado ou com assinatura inválida retorna `401`; usuário autenticado sem ADMIN em operação administrativa retorna `403`; request inválido retorna `400`; usuário-alvo inexistente retorna `404`; e-mail duplicado ou tentativa de remover/desativar o último ADMIN ativo retorna `409`.

Senhas, hashes, JWTs completos e chaves secretas não podem aparecer em logs ou respostas. A aplicação deverá respeitar o limite de entrada do BCrypt sem truncar silenciosamente senhas.

### P1.6 — Configuração e isolamento do banco

O User Service utiliza database lógico PostgreSQL próprio denominado `techstore_user_db`, gerenciado pelo Flyway. A instância física PostgreSQL pode ser compartilhada com outros serviços, mas cada serviço mantém seu database lógico e credenciais/datasource configurados separadamente. O User Service usa `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.

`TECHSTORE_JWT_SECRET` é obrigatório para assinar tokens, tem pelo menos 32 bytes e é compartilhado com o Product Service para validação HS256. `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD` são um par opcional: ambas ausentes desabilitam o bootstrap; somente uma definida impede a inicialização. Secrets não podem ser versionados ou registrados.

O User Service e o Product Service validam localmente tokens e autorização. O API Gateway encaminha as rotas públicas de autenticação e administrativas de usuários pela rede privada, removendo o prefixo `/api`, mas não aplica autorização específica de domínio.
