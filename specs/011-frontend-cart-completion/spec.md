# Especificação da Feature: Conclusão do Carrinho no Frontend

**Feature Branch**: `011-frontend-cart-completion`  
**Criado em**: 2026-10-02  
**Status**: Draft  
**Entrada**: Completar a experiência de carrinho existente no frontend TechStore usando os dados e operações já oferecidos pelo Cart Service, sem alterar serviços ou contratos do backend.

## Contexto

Clientes precisam revisar o valor de cada unidade e de cada linha, compreender quando um preço não está disponível e confiar que o total e o contador refletem a última resposta confirmada do carrinho. A experiência também deve deixar claros os estados de carregamento, erro, indisponibilidade e operações em andamento, em telas móveis e desktop.

Esta feature completa a experiência frontend existente; não substitui a especificação [008-frontend-cart](../008-frontend-cart/spec.md) nem as regras de domínio de [004-cart](../004-cart/spec.md). O frontend continua consumidor do carrinho autenticado. Preço, subtotal, total, estoque, disponibilidade e limites permanecem sob autoridade do serviço existente.

## Clarificações

### Sessão 2026-10-02

- Q: Depois de limpar o carrinho, o serviço confirma a operação sem enviar um novo resumo financeiro. Você prefere que a interface consulte o carrinho novamente para obter o total oficial ou que esconda o total no estado vazio? → A: Consultar o carrinho após a limpeza e usar o resumo retornado.
- Q: Devemos tratar o DTO e os controllers atuais como o contrato vigente do carrinho e considerar o OpenAPI versionado desatualizado, sem alterá-lo nesta feature? → A: Usar DTOs/controllers atuais como referência; registrar a divergência do OpenAPI, sem alterá-lo.

## Cenários de Usuário e Testes

### História de Usuário 1 - Entender os valores do carrinho (Prioridade: P1)

Como cliente, quero distinguir preço unitário, quantidade, subtotal de cada produto e total do carrinho para revisar o valor da compra sem cálculos ambíguos.

**Por que esta prioridade**: A apresentação de valores é o principal objetivo desta conclusão e deve refletir fielmente os dados confirmados pelo serviço.

**Teste independente**: Carregar respostas com uma e várias linhas com valores conhecidos e comparar cada valor apresentado com o valor correspondente recebido.

**Cenários de Aceitação**:

1. **Dado** um carrinho com preços conhecidos, **quando** ele for carregado ou atualizado, **então** cada linha apresenta preço unitário, quantidade e subtotal, e o resumo apresenta produtos distintos, unidades e total recebido.
2. **Dado** que uma operação bem-sucedida retorne um carrinho atualizado, **quando** a tela e o cabeçalho forem atualizados, **então** ambos representam essa resposta confirmada.
3. **Dado** um item cujo preço não esteja disponível, **quando** o carrinho for exibido, **então** a linha informa que o preço e o subtotal estão indisponíveis, sem mostrar valor zero ou estimado.
4. **Dado** que exista ao menos um item sem preço disponível, **quando** o resumo for apresentado, **então** o total é identificado como indisponível.
5. **Dado** que o último item sem preço seja removido e a consulta subsequente confirme um total disponível, **quando** a resposta for aplicada, **então** o resumo volta a apresentar o total confirmado.

### História de Usuário 2 - Ajustar quantidades com segurança (Prioridade: P1)

Como cliente, quero aumentar ou diminuir quantidades e receber orientação quando estoque ou limite impedir a operação, sem ver um estado que o serviço não confirmou.

**Por que esta prioridade**: Alterações de quantidade afetam diretamente o resumo financeiro e o contador compartilhado.

**Teste independente**: Exercitar incrementos, decrementos, limite configurado e respostas de conflito, verificando bloqueio de ações pendentes e preservação da quantidade confirmada.

**Cenários de Aceitação**:

1. **Dado** um item editável, **quando** o cliente alterar sua quantidade, **então** a interface apresenta pendência, impede operações conflitantes e só exibe a quantidade retornada com sucesso pelo serviço.
2. **Dado** o limite máximo informado pelo carrinho, **quando** a quantidade atingir esse limite, **então** a interface não permite solicitar um incremento inválido, sem substituir a validação do serviço.
3. **Dado** que o serviço rejeite uma quantidade por estoque insuficiente ou limite excedido, **quando** o erro for recebido, **então** a quantidade anterior permanece e a mensagem explica o problema em linguagem amigável.
4. **Dado** que o serviço rejeite uma operação por produto inexistente ou inativo, **quando** a resposta for recebida, **então** a interface não mantém a alteração e oferece uma ação coerente com a disponibilidade do item.
5. **Dado** um item marcado como indisponível, **quando** ele for exibido, **então** o status é claro, a alteração de quantidade não é oferecida e a remoção continua disponível.

### História de Usuário 3 - Remover itens ou esvaziar o carrinho (Prioridade: P1)

Como cliente, quero remover uma linha ou limpar o carrinho e ver o resumo e o cabeçalho sincronizados após a confirmação do serviço.

**Por que esta prioridade**: A recuperação de um carrinho com itens indesejados ou preço desconhecido é parte necessária de uma experiência confiável.

**Teste independente**: Remover uma linha conhecida, uma linha sem preço, o último item e limpar o carrinho; verificar o estado vazio, totais confirmados e contador.

**Cenários de Aceitação**:

1. **Dado** um item no carrinho, **quando** sua remoção for confirmada, **então** a interface sincroniza as linhas e o contador e obtém do serviço o resumo financeiro atualizado, pois a remoção não retorna um corpo com o carrinho.
2. **Dado** que a remoção da última linha sem preço seja confirmada, **quando** o novo estado financeiro for consultado, **então** o total só volta a ser exibido se essa resposta o declarar disponível.
3. **Dado** um carrinho com itens, **quando** o cliente iniciar a limpeza, **então** uma confirmação acessível é apresentada; cancelar não envia a operação nem altera o estado.
4. **Dado** que a limpeza seja confirmada pelo serviço, **quando** a interface for atualizada, **então** os itens são removidos, o contador fica em zero, é apresentado o estado vazio com ação para continuar comprando e uma nova consulta obtém o resumo financeiro oficial do carrinho vazio.
5. **Dado** que a limpeza tenha sido confirmada mas a consulta do resumo falhe, **quando** a interface for atualizada, **então** o carrinho continua vazio, o total não é inventado e o cliente pode tentar atualizar o resumo novamente.

### História de Usuário 4 - Reconhecer o estado e continuar navegando (Prioridade: P2)

Como cliente, quero saber se o carrinho está carregando, vazio ou indisponível e encontrar uma forma clara de recuperar ou continuar.

**Por que esta prioridade**: Estados explícitos evitam que uma falha de rede pareça perda de dados e mantêm o fluxo navegável.

**Teste independente**: Simular carregamento inicial, carrinho vazio, falta de autenticação, erro de serviço, erro de rede e operações pendentes.

**Cenários de Aceitação**:

1. **Dado** que a consulta inicial esteja pendente, **quando** a rota do carrinho abrir, **então** a tela apresenta um estado de carregamento reconhecível.
2. **Dado** que não exista sessão válida, **quando** o cliente abrir o carrinho, **então** recebe orientação para entrar na conta sem visualizar dados da sessão anterior.
3. **Dado** um erro recuperável, **quando** a consulta falhar, **então** a mensagem é amigável e há opção de tentar novamente sem apagar o último estado confirmado.
4. **Dado** que uma operação esteja pendente, **quando** o cliente tentar repeti-la ou iniciar outra operação concorrente, **então** a interface impede a concorrência e comunica a pendência.
5. **Dado** que o cliente selecione continuar comprando, **quando** a navegação ocorrer, **então** retorna ao catálogo existente e não inicia checkout.
6. **Dado** um viewport mobile, tablet ou desktop, **quando** o carrinho for exibido, **então** conteúdo, mensagens e ações permanecem legíveis e acessíveis sem sobreposição ou rolagem horizontal causada pelo layout.

## Casos de Borda

- Um item pode ter produto indisponível e resumo de produto nulo, com preço de snapshot conhecido ou desconhecido.
- Preço unitário e subtotal nulos ou sinalizados como indisponíveis nunca podem ser formatados como moeda nem tratados como zero.
- O total pode ser nulo e indisponível mesmo que outras linhas tenham preço conhecido.
- O serviço pode rejeitar uma atualização por estoque, limite, item inexistente ou produto inativo; a última quantidade confirmada deve permanecer.
- As operações de remoção e limpeza respondem sem corpo; após ambas, o frontend consulta o carrinho para obter o resumo financeiro confirmado. Falha nessa consulta não desfaz uma mutação já confirmada e não autoriza inventar valores.
- Uma falha depois de iniciar uma mutação não confirma a alteração local.
- Uma resposta atrasada de consulta não pode sobrescrever uma mutação confirmada posteriormente.
- Uma resposta `401` invalida o acesso e não pode deixar dados do carrinho visíveis para outra sessão.
- Nome longo, preço indisponível, imagem ausente ou erro não podem causar overflow ou ocultar ações essenciais.
- Identificadores técnicos de produto não são informação de apresentação; SKU só pode ser mostrado quando vier dos dados existentes.

## Requisitos

### Requisitos Funcionais

- **RF-001**: O frontend DEVE apresentar claramente, para cada item com valores disponíveis, o preço unitário, a quantidade e o subtotal informados pelo carrinho.
- **RF-002**: O resumo DEVE apresentar quantidade de produtos distintos, quantidade total de unidades e total financeiro informado pelo serviço, sem recalcular valores financeiros no frontend.
- **RF-003**: Quando `priceAvailable` for falso ou `unitPriceSnapshot`/`subtotal` não estiver disponível, o frontend DEVE apresentar preço e subtotal como indisponíveis, sem mostrar `R$ 0,00`, preço atual presumido ou valor fictício.
- **RF-004**: Quando `totalAvailable` for falso ou `total` não estiver disponível, o frontend DEVE comunicar que o total está indisponível; deve exibir o total somente quando a resposta confirmada o declarar disponível.
- **RF-005**: O frontend DEVE substituir seu estado pelo `CartResponse` confirmado em operações de inclusão e atualização de quantidade, incluindo os dados financeiros e `maxItemQuantity` retornados.
- **RF-006**: O frontend DEVE respeitar `maxItemQuantity` recebido para orientar os controles, sem criar regras de estoque nem assumir autoridade superior à validação do serviço.
- **RF-007**: Durante alteração, remoção ou limpeza, a interface DEVE apresentar estado pendente e impedir solicitações concorrentes que possam terminar fora de ordem.
- **RF-008**: Em erro de alteração, o frontend DEVE preservar a quantidade e os valores da última resposta confirmada e apresentar mensagem amigável baseada nos detalhes relevantes retornados.
- **RF-009**: O frontend DEVE comunicar indisponibilidade do produto conforme o estado recebido, sem tentar apresentar campos ausentes, e deve manter a ação de remoção disponível.
- **RF-010**: Após remover uma linha, o frontend DEVE sincronizar as linhas confirmadas e consultar o serviço para obter o resumo atualizado, pois a operação não fornece um novo resumo financeiro.
- **RF-011**: A limpeza DEVE apresentar confirmação acessível, só executar após confirmação e, em sucesso, zerar o contador, consultar o serviço para obter o resumo oficial do carrinho vazio e apresentar esse estado com ação para continuar comprando. Se a consulta falhar, a limpeza permanece confirmada, o total não é inventado e a consulta pode ser repetida. Cancelar não deve mutar o estado.
- **RF-012**: O contador do cabeçalho DEVE continuar representando unidades totais, somando quantidades das linhas disponíveis e indisponíveis, e deve atualizar após cada mutação confirmada.
- **RF-013**: A tela DEVE apresentar estados distintos para carregamento inicial, erro recuperável, carrinho vazio, preço indisponível, produto indisponível e operações pendentes.
- **RF-014**: Erros `401`, `403` quando aplicável, `404`, `409`, `422` quando aplicável, erros `5xx` e falhas de rede DEVEM resultar em feedback não técnico; detalhes úteis do serviço devem ser preservados quando seguros e relevantes.
- **RF-015**: O frontend DEVE continuar usando a autenticação JWT existente e nunca solicitar ou enviar identificador do usuário como proprietário do carrinho.
- **RF-016**: A interface NÃO DEVE exibir UUIDs de produto; SKU só pode ser mostrado se já estiver disponível nos dados usados pelo frontend.
- **RF-017**: A experiência DEVE manter identidade visual dark e responsividade mobile, tablet e desktop, com navegação por teclado, rótulos acessíveis e avisos anunciáveis.
- **RF-018**: A feature DEVE permanecer exclusivamente no frontend; não deve alterar serviços, gateway, autenticação, banco, migrations ou contrato do backend, nem criar checkout ou pagamento.

### Entidades Principais

- **Carrinho**: Estado atual do cliente autenticado, com linhas, limite máximo informado e resumo financeiro autorizado.
- **Linha do carrinho**: Produto identificado pelos dados já existentes, quantidade confirmada, estado de disponibilidade e dados de preço que podem estar indisponíveis.
- **Resumo financeiro**: Total agregado fornecido pelo serviço e indicação explícita de sua disponibilidade.

## Critérios de Sucesso

### Resultados Mensuráveis

- **CS-001**: Em todos os testes com preço disponível, os valores unitário, subtotal por linha e total exibidos correspondem exatamente aos valores retornados, sem cálculo financeiro no frontend.
- **CS-002**: Em todos os testes com preço ou total indisponível, nenhuma linha ou resumo apresenta valor monetário zero ou estimado como substituto.
- **CS-003**: Após cada mutação bem-sucedida, tela e cabeçalho representam o estado confirmado; respostas de erro preservam o último estado confirmado.
- **CS-004**: Testes cobrem o limite informado, estoque insuficiente, prevenção de operações conflitantes e tratamento de `401`, `404`, `409`, `5xx` e falha de rede.
- **CS-005**: Testes cobrem carrinho vazio, um e vários itens, preços conhecidos/desconhecidos, remoção, limpeza, loading e contador.
- **CS-006**: Verificações em mobile, tablet e desktop confirmam que as informações e ações essenciais permanecem acessíveis sem sobreposição nem overflow horizontal.

## Premissas e Dependências

- O Cart Service continua sendo a fonte de verdade para quantidade, disponibilidade, snapshots de preço, subtotais, total e limite máximo.
- Inclusão e alteração de quantidade retornam o carrinho atualizado; remoção de linha e limpeza retornam `204` sem corpo.
- O cabeçalho continua contando unidades totais, não apenas produtos distintos.
- O login, JWT, API Gateway, rota de catálogo e padrões visuais atuais serão reutilizados.
- Checkout ainda não existe no escopo desta experiência e não será simulado.

## Divergência Documental Encontrada

O DTO e os controllers atuais do Cart Service são a referência para esta feature e retornam `maxItemQuantity`, `total`, `totalAvailable`, `unitPriceSnapshot`, `priceAvailable` e `subtotal`, além de `items`. O OpenAPI versionado em `specs/004-cart/contracts/cart-service-api.openapi.yaml` está desatualizado: descreve somente o contrato anterior (`items` e dados básicos da linha) e não documenta respostas `409` para limite/estoque, embora o handler atual emita `409` para esses conflitos. A implementação frontend deve consumir o contrato exposto pelo serviço atual; nenhum arquivo de backend ou contrato será alterado nesta feature.

## Fora do Escopo

- Alterar Cart Service, Product Service, User Service, API Gateway, OpenAPI do backend, banco, migrations, JWT ou regras de negócio.
- Calcular preço, estoque, subtotal, total, disponibilidade real ou limite no frontend.
- Criar checkout, rota de checkout, pedido, frete, desconto, pagamento ou reserva de estoque.
- Criar SKU ou qualquer identificador comercial que não exista nos dados atuais.