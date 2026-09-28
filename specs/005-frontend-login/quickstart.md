# Quickstart de Validação: Login do Cliente

## Pré-requisitos

- Node.js/npm compatíveis com o frontend existente e dependências instaladas.
- API Gateway e User Service compatível disponíveis; endpoint `POST /api/auth/login` acessível via `VITE_API_URL`.
- Credenciais de teste válidas e inválidas. Não registrar nem compartilhar senhas ou tokens.

## Executar o frontend

Na raiz do repositório:

```bash
cd frontend
npm install
```

Se o Gateway local estiver em `http://localhost:8080`, configure a base URL antes de iniciar o servidor Vite:

```powershell
$env:VITE_API_URL = "http://localhost:8080/api"
npm run dev
```

Abra `http://localhost:5173/login`. O fluxo não usa mocks para autenticar; o serviço real precisa estar disponível.

## Verificações automatizadas

```bash
npm test
npm run build
```

Resultado esperado: testes de auth service e componentes aprovados; compilação TypeScript e build Vite concluídos sem erros.

## Cenários manuais

1. Enviar sem e-mail ou senha: mensagens associadas aos campos e nenhuma requisição ao Gateway.
2. Enviar e-mail em formato básico inválido: submissão bloqueada localmente.
3. Alternar mostrar/ocultar senha por mouse e teclado: valor preservado e estado anunciado acessivelmente.
4. Enviar credenciais inválidas: mensagem genérica, sem indicar se a conta existe ou expor resposta interna.
5. Simular Gateway indisponível: mensagem de conexão, formulário continua utilizável e nenhuma sessão é criada.
6. Enviar credenciais válidas: botão mostra carregamento, não aceita submissão duplicada; token fica disponível na sessão em memória e o navegador vai para `/`.
7. Recarregar depois de autenticar: o estado volta a não autenticado, conforme decisão desta feature.
8. Ativar “Criar conta”: navega para `/register` e exibe “Cadastro indisponível no momento”; confirmar que não há formulário/request de cadastro nem alteração da sessão.
9. Verificar desktop e viewport de 320 px: sem campos, ações ou mensagens cortados/sobrepostos.

## Contrato

Ver [contrato OpenAPI](contracts/auth-login-api.openapi.yaml) e [modelo da sessão](data-model.md). O backend e seu contrato não são modificados nesta feature.