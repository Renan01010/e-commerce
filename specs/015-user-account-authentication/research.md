# Research: Conta do Usuário e Autenticação

## Decisões

### 1. Revogação imediata de JWT em microsserviços

**Decision**: O User Service será a autoridade central de sessão. Cada JWT incluirá uma versão de autenticação (`auth_version`) do usuário. O User Service valida essa versão localmente; Cart e Product Services mantêm a verificação criptográfica e consultam um endpoint de introspecção privado no User Service para cada requisição autenticada. O endpoint retorna somente se o token é ativo, expirado/inválido ou revogado. Redefinir a senha incrementa a versão persistida e revoga imediatamente tokens com a versão anterior; desativar uma conta também faz a introspecção negar os tokens. Clientes internos falham fechados e retornam indisponibilidade quando a autoridade não pode ser consultada.

**Rationale**: Os serviços atuais verificam localmente a assinatura HS256 e não consultam uma fonte de revogação. JWT stateless sem estado compartilhado não pode ser revogado individualmente antes do `exp`. A decisão do usuário exige revogação imediata e autorizou mudanças limitadas nos adapters de segurança de Cart e Product.

**Alternatives considered**:
- Apenas remover JWT do estado frontend no logout: encerra a sessão naquele dispositivo, mas não revoga tokens copiados nem tokens emitidos em outros dispositivos.
- Reduzir `expiresIn`: limita a janela, mas não fornece invalidação imediata.
- Rotacionar `TECHSTORE_JWT_SECRET`: revoga todos os usuários e invalida globalmente todas as sessões, não apenas a conta alterada; exige rotação coordenada operacional.
- Redis/blacklist replicada: permite consulta local, mas acrescenta infraestrutura e estado operacional não existente no projeto.
- Cachear respostas positivas de introspecção: reduz latência, mas permite que tokens recentemente revogados continuem sendo aceitos; incompatível com a exigência de imediatismo.

**Operational constraints**: Endpoint não é encaminhado pelo API Gateway; User Service permanece privado em Railway. A introspecção valida assinatura, expiração, `sub`, conta ativa e `auth_version`, e não retorna perfil, e-mail ou claims sensíveis. Configurar timeout curto, propagar correlation ID sem registrar token, falhar fechada com `503` explícito (sem converter indisponibilidade em autorização nem ocultá-la como JWT inválido) e monitorar disponibilidade/latência. Não adicionar cache de validade. JWTs já emitidos sem `auth_version` precisarão autenticar-se novamente após o rollout coordenado.

### 2. Persistência de usuário e compatibilidade de migração

**Decision**: Preservar a migration V1 e criar migration Flyway aditiva. Introduzir `name` nullable para não inventar nomes de contas legadas, `email_verified` false por padrão e `auth_version` com inicialização uniforme. Criar tabelas próprias para tokens de verificação e reset com hash, expiração, uso e timestamps; indexar hash e usuário/estado conforme as consultas.

**Rationale**: A tabela atual não contém nome nem verificação; contas existentes precisam confirmar antes do próximo login. Migrations imutáveis e propriedade exclusiva do User Service são princípios existentes.

**Alternatives considered**:
- Alterar V1: incompatível com bancos já migrados e proibido pela spec.
- Preencher nomes a partir do e-mail: cria dados fictícios para usuários reais e foi explicitamente vedado.
- Marcar contas existentes como verificadas: contradiz a decisão clarificada.

### 3. Hashes e ciclo de vida de tokens temporários

**Decision**: Gerar tokens de alta entropia, apresentar o valor original somente no link de e-mail e persistir hash criptográfico unidirecional. Confirmação expira em 24 horas; reset expira em 1 hora. Consumo e atualização de conta ocorrem atomicamente; token utilizado, expirado ou substituído é inválido. Uma solicitação nova invalida tokens anteriores do mesmo tipo.

**Rationale**: Tokens são credenciais bearer; a spec proíbe armazenamento em texto puro e exige uso único.

**Alternatives considered**:
- BCrypt para token curto prazo: desnecessário para valores aleatórios de alta entropia e não indexável para lookup. Hash criptográfico sobre segredo aleatório é adequado.
- Reutilizar JWT de acesso para reset/verify: confunde audiência, finalidade e ciclo de vida.

### 4. Email de confirmação e recuperação

**Decision**: Usar uma porta `EmailSenderPort` e adapter SMTP baseado no Spring Boot Mail, com host/porta/login/senha/from configurados no ambiente; manter fake adapter em testes. Configurar também a origem pública da aplicação para montar links sem URL hardcoded. Executar envio após persistência da conta/tokens. Falha de envio não apaga cadastro nem produz confirmação falsa; registra evento sanitizado e orienta reenvio. Respostas de reenvio/recuperação permanecem genéricas para não enumerar contas.

**Rationale**: SMTP é um mecanismo externo já permitido pela solicitação, encaixa no modelo de ports/adapters e não exige fornecedor específico nem infraestrutura adicional.

**Alternatives considered**:
- Servidor próprio: explicitamente fora do escopo.
- API de fornecedor específica: acopla o User Service sem fornecedor escolhido; pode ser outro adapter posterior.
- Broker/outbox/fila: aumenta infraestrutura e escopo sem requisito de entrega assíncrona ou volume conhecido.

**Operational note**: Railway deve receber SMTP/provider suportado por variáveis e saída de rede habilitada. Credenciais reais não entram no repositório. Nenhum link usa origem hardcoded: `APP_PUBLIC_URL` deve ser configuração de ambiente.

### 5. Login, sessão local e proteção de rotas

**Decision**: Preservar token de acesso em memória no frontend; usar interceptor Axios atual para enviar Bearer; adicionar guards de rotas de conta e logout que limpa estado de autenticação e stores privados. Somente conta ativa e verificada pode receber JWT.

**Rationale**: Mantém o comportamento do login existente e evita persistência de token no browser. Backend permanece autoritativo.

**Alternatives considered**:
- Persistência em localStorage: eleva exposição a XSS e contradiz comportamento atual.
- Proteção somente frontend: contornável e proibida pela spec.

### 6. Rate limiting e enumeração

**Decision**: Não introduzir Redis, gateway rate limiter ou nova infraestrutura neste incremento sem suporte existente. Documentar login, recuperação e reenvio como superfícies que precisam de limitação operacional; manter respostas uniformes em login, recovery e resend e não registrar credenciais/tokens. Reavaliar limite no deployment com evidência de abuso/necessidade.

**Rationale**: A spec explicitamente orienta documentar a necessidade sem infraestrutura desnecessária se não houver rate limiting atual.

**Alternatives considered**:
- Implementar rate limiting em memória em réplicas: limites inconsistentes entre instâncias e resetados em restart.
- Adicionar Redis/gateway filter: dependência operacional fora do escopo aprovado.

## Dependências a confirmar durante tasks

- Disponibilidade de host SMTP na rede de produção Railway e remetente/domínio autenticado.
- URLs internas Railway/Compose para User Service a partir de Cart e Product Service.
- Estratégia de rollout para que os três serviços validem a nova versão do token antes de habilitar os fluxos frontend.
- CORS/Gateway já encaminha `/api/auth/**` e `/api/users/**`; introspecção permanece fora do routing público.
