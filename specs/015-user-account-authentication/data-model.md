# Data Model: Conta do Usuário e Autenticação

## User (aggregate root)

Mantido no database exclusivo do User Service. A representação de domínio não depende de JPA/Spring.

| Field | Type/concept | Required | Rules |
|---|---|---:|---|
| id | UUID | Yes | Identificador atual; permanece estável. |
| name | text, max 100 | New public accounts: yes; legacy: no | Remover espaços externos; contas legadas permanecem sem nome até o usuário informar um, sem valor fictício. |
| email | text, max 254 | Yes | Único e normalizado por trim + lowercase conforme regra atual. Edição de e-mail não faz parte do perfil desta feature. |
| password_hash | BCrypt string | Yes | Senha nunca é persistida em texto puro; manter encoder/política existentes. |
| role | USER/ADMIN | Yes | Role padrão pública continua USER; atribuição ADMIN permanece restrita às operações existentes. |
| active | boolean | Yes | Preservar semântica atual da conta. |
| email_verified | boolean | Yes | Novas contas false; contas anteriores à migration também false por decisão de produto. |
| auth_version | integer/long | Yes | Iniciar em 0; incrementar atomicamente após redefinição de senha; JWT novo carrega a versão. |
| created_at / updated_at | timestamp UTC | Yes | Preservar formato/padrão atuais. |

### User lifecycle

```text
REGISTERED_UNVERIFIED -- valid verification token --> ACTIVE_VERIFIED
REGISTERED_UNVERIFIED -- mail delivery failure --> REGISTERED_UNVERIFIED
ACTIVE_VERIFIED -- password reset --> ACTIVE_VERIFIED (auth_version + 1)
Any active account -- inactive administrative state --> INACTIVE (introspection rejects)
```

- Somente conta ativa e verificada pode autenticar.
- Contas antigas que não possuem confirmação verificável são não verificadas, não sendo marcadas artificialmente como confirmadas.
- Falha de envio não desfaz conta ou token; o reenvio emite novo token e invalida os anteriores daquele tipo.

## EmailVerificationToken

Persistido em tabela nova do User Service.

| Field | Type/concept | Rules |
|---|---|---|
| id | UUID | Primary key. |
| user_id | UUID | FK para User; cascade/retention conforme padrão de persistência do serviço. |
| token_hash | fixed-length digest | Unique lookup; nunca o token original. |
| expires_at | timestamp UTC | `created_at + 24 hours`. |
| used_at | nullable timestamp UTC | Null até consumo único. |
| created_at | timestamp UTC | Instante de emissão. |

## PasswordResetToken

Persistido em tabela nova do User Service.

| Field | Type/concept | Rules |
|---|---|---|
| id | UUID | Primary key. |
| user_id | UUID | FK para User. |
| token_hash | fixed-length digest | Unique lookup; nunca o token original. |
| expires_at | timestamp UTC | `created_at + 1 hour`. |
| used_at | nullable timestamp UTC | Null até consumo único. |
| created_at | timestamp UTC | Instante de emissão. |

## Issued JWT

JWT de acesso continua HS256 e mantém a compatibilidade das claims atuais.

| Claim | Meaning |
|---|---|
| sub | UUID da conta (claim existente). |
| roles | Roles atuais (claim existente). |
| iat, exp | Emissão e expiração atuais; validade atual de 24 horas. |
| auth_version | Versão atual da conta no instante da emissão; claim nova requerida para validade central. |

Um token só está ativo se sua assinatura, expiração e estrutura forem válidas, a conta continuar ativa e sua `auth_version` coincidir com a versão atual armazenada para `sub`. Reset de senha incrementa a versão; a validação central invalida imediatamente os tokens antigos.

## Relationships and invariants

- `User 1 — 0..N EmailVerificationToken`; somente token vigente não consumido pode verificar e-mail.
- `User 1 — 0..N PasswordResetToken`; token válido é consumido no mesmo fluxo transacional que altera o hash e incrementa `auth_version`.
- Dados do carrinho não pertencem a este modelo: Cart Service permanece autoridade e associa recursos ao subject autenticado.
- Nunca retornar `password_hash`, `token_hash`, token original, `auth_version` interno, segredo JWT ou credenciais SMTP em DTOs públicos.
- Operações de token devem rejeitar expirado/consumido/substituído e tratar concorrência para não permitir dois usos bem-sucedidos.
