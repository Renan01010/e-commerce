# Modelo de Dados: Login do Cliente

Os dados deste fluxo são transitórios no browser; não há persistência frontend/back-end nova nesta feature.

## LoginCredentials

Entrada temporária de uma tentativa de autenticação.

| Campo | Tipo lógico | Regra |
|---|---|---|
| `email` | string | Obrigatório; validação básica de formato antes do envio. Espaços periféricos não devem causar rejeição local de endereço válido. |
| `password` | string | Obrigatório; valor é enviado ao serviço existente, nunca exibido em erro nem guardado na sessão. |

## AuthSession

Estado transitório criado somente após resposta válida do serviço.

| Campo | Tipo lógico | Regra |
|---|---|---|
| `accessToken` | string | Obrigatório e não vazio; usado como bearer token nas chamadas autenticadas. |
| `tokenType` | string | O contrato atual retorna `Bearer`. |
| `expiresAt` | timestamp | Derivado de `expiresIn`; o interceptor não deve enviar token expirado. |

**Armazenamento e ciclo de vida**: estado compartilhado em memória durante a vida do SPA; sem `localStorage`, `sessionStorage`, IndexedDB ou cookie persistente. Reload/fechamento retorna a aplicação ao estado não autenticado. Não existe renovação automática nesta feature.

## LoginViewState

Estado apenas da interação corrente, separado da sessão:

| Campo | Tipo lógico | Estados/uso |
|---|---|---|
| `status` | enum lógico | `idle`, `submitting`, `error`, `success`; impede envios concorrentes e representa carregamento. |
| `errorMessage` | string opcional | Texto seguro para usuário; nunca contém senha, token ou detalhe interno do backend. |
| `passwordVisible` | boolean | Controla somente o tipo visual do campo; não altera o valor. |

## Transições

| Estado atual | Evento | Resultado |
|---|---|---|
| Não autenticado/idle | Envio inválido | Erro local; nenhuma requisição. |
| Não autenticado/idle | Envio válido | `submitting`; um único request em andamento. |
| Submitting | 200 com resposta válida | Cria `AuthSession` em memória e navega para `/`. |
| Submitting | 401 | Retorna a idle com mensagem genérica de credenciais inválidas. |
| Submitting | Rede/timeout/5xx/resposta inválida | Retorna a idle com mensagem segura e opção de tentar novamente; sem sessão. |
| Autenticado | Reload, fechamento ou expiração | Remove sessão volátil; nenhuma renovação automática. |