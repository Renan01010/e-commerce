# Modelo de Dados: User Service

**Feature**: `003-user-auth`  
**Data**: 2026-09-27

## Entidade User

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| `id` | UUID | Sim | Chave primária gerada pelo serviço |
| `email` | varchar(254) | Sim | `trim` + lowercase antes de persistir; único no database |
| `passwordHash` | varchar(60) | Sim | Hash BCrypt; senha em texto nunca persistida |
| `role` | enum/varchar | Sim | Uma role: `USER` ou `ADMIN`; novos registros públicos recebem `USER` |
| `active` | boolean | Sim | Padrão `true`; conta inativa não pode autenticar |
| `createdAt` | timestamptz | Sim | UTC, definido na criação |
| `updatedAt` | timestamptz | Sim | UTC, atualizado em mudanças administrativas |

## Restrições e validação

- `id` é UUID e primary key.
- E-mail é validado por formato, aparado, convertido para lowercase e limitado a 254 caracteres. A coluna tem unique constraint; a normalização no caso de uso torna a unicidade determinística.
- `role` aceita exclusivamente `USER` e `ADMIN`, protegida por enum de domínio e constraint do banco.
- `passwordHash` recebe apenas resultado do encoder BCrypt (cost 12); nunca deve aparecer em DTO ou log.
- Senha de entrada tem 8 a 72 caracteres e não pode exceder 72 bytes UTF-8 para evitar truncamento pelo BCrypt.
- `createdAt` não muda após criação. `updatedAt` avança em atualizações administrativas.
- A operação pública de registro força `role=USER` e `active=true`, ignorando/rejeitando campos não definidos no request.

## Regras de ciclo de vida

- Bootstrap administrativo: quando as duas variáveis `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD` estiverem presentes, criar um ADMIN ativo caso o e-mail normalizado não exista. Se ambas estiverem ausentes, ignorar bootstrap; se apenas uma estiver presente, falhar startup. Nunca sobrescrever usuário existente.
- POST administrativo pode criar `USER` ou `ADMIN`; atribuição de role depende de autorização ADMIN no User Service.
- PUT administrativo pode alterar `role` e/ou `active`, mas não e-mail nem senha.
- Não permitir que um administrador se remova/desative como último ADMIN ativo.
- Login exige usuário existente, ativo e senha BCrypt válida.
- Exclusão de conta não está no escopo; contas são desativadas via `active=false`.

## Persistência

Tabela única `users` no database lógico PostgreSQL exclusivo `techstore_user_db`, propriedade do User Service. Flyway mantém migrations e Hibernate deve validar o schema, nunca criá-lo automaticamente. Nenhum outro serviço acessa diretamente essa tabela.
