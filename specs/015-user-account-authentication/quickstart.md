# Quickstart de Validação: Conta do Usuário e Autenticação

## Pré-requisitos

- Java 21+, Maven 3.9+, Node.js 20+, npm 10+.
- PostgreSQL/Docker disponível para testes com Testcontainers.
- Para execução manual completa: PostgreSQL e serviços locais via Docker Compose.
- Testes usam adapter de e-mail falso; SMTP real não é necessário para validação automatizada.

## Configuração local

1. Copie `.env.example` para `.env`; use senha PostgreSQL e `TECHSTORE_JWT_SECRET` local com pelo menos 32 bytes.
2. Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_FROM_NAME` e `APP_PUBLIC_URL` somente para teste manual de e-mail. Não versione `.env`.
3. Inicie os serviços com `docker compose up --build`. O Gateway é a entrada do browser em `http://localhost:8080`; o frontend fica em `http://localhost:5173`.
4. A migration nova deve ser aplicada pelo Flyway no database lógico do User Service sem editar a V1.
5. Configure a URL interna do User Service nos serviços Cart/Product para validação remota de sessão; `/internal/auth/introspect` não deve estar disponível por rota pública do Gateway.

## Validação automatizada

Na raiz, execute backend:

```powershell
mvn -f backend/pom.xml test
mvn -f backend/pom.xml verify
```

Em `frontend/`:

```powershell
npm test
npm run build
```

Esperado: testes de domínio/API/migration e validação de JWT passam; testes de introspecção confirmam ativo, expirado, versão revogada, timeout e fail-closed; testes de e-mail não dependem de SMTP; testes frontend verificam fluxos, guards e logout.

## Cenários de fumaça manuais

1. Criar conta com nome, e-mail e senha válidos. Confirmar conta persistida mesmo se SMTP estiver indisponível; a UI não deve dizer que o e-mail foi enviado e deve permitir reenvio.
2. Abrir link de confirmação válido antes de 24 horas; confirmar verificação e rejeitar reuso/expiração.
3. Entrar antes de verificar e confirmar resposta genérica sem JWT. Após verificar, entrar e confirmar o JWT mantém `sub`, `roles`, HS256 e validade atual.
4. Consultar `/account`, editar nome e alterar senha; visitante não acessa essas rotas nem endpoints backend de perfil.
5. Solicitar recuperação sem revelar existência de e-mail; redefinir com link válido em até 1 hora e rejeitar link expirado/reutilizado.
6. Após reset, repetir requisição protegida em User/Cart/Product com JWT antigo; todos devem negar imediatamente. Novo login gera JWT válido.
7. Adicionar item ao carrinho como usuário A, logout/login de A e confirmar persistência. Autenticar usuário B e confirmar isolamento.
8. Desligar User Service e tentar operação autenticada de Cart/Product; serviço deve falhar fechadamente com indisponibilidade, nunca aceitar JWT sem introspecção.
9. Verificar no browser rotas públicas e privadas, header para visitante/autenticado, estados de loading/erro/sucesso, navegação por teclado e viewports móvel/desktop.

## Operação / rollout

- Primeiro aplicar migration no User Service e implantar introspecção privada.
- Implantar os validadores de Cart/Product e confirmar sua conectividade privada antes de ativar os fluxos de conta no frontend.
- Por último implantar cadastro/verificação/reset que emite JWTs com `auth_version`. Tokens antigos sem essa claim deixam de ser aceitos; usuários precisam entrar novamente, e contas ainda não verificadas devem concluir a confirmação antes do novo login.
- Confirmar SMTP, segredos e URLs internas como variáveis Railway; manter User Service não público e não expor introspecção através do Gateway.
- Após rollout, contas legadas precisarão verificar o e-mail antes de novo login. Monitorar 401 de tokens sem versão e falhas/latência de introspecção.

## Referências

- [Spec](spec.md)
- [Plan](plan.md)
- [Research](research.md)
- [Data model](data-model.md)
- [API contracts](contracts/account-authentication-api.openapi.yaml)
