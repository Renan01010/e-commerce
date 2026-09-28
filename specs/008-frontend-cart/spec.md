# Especificação da Feature: Carrinho no Frontend

**Branch da Feature**: `008-frontend-cart`  
**Criado em**: 2026-09-28  
**Status**: Draft  
**Entrada**: Criar a experiência de carrinho no frontend do TechStore usando exclusivamente o Cart Service existente.

## Visão Geral

Esta feature permite que clientes autenticados consultem e mantenham seu carrinho a partir do frontend TechStore. O frontend consome os endpoints existentes através do API Gateway; o Cart Service continua responsável pela identidade do proprietário, validação do produto, regras de quantidade e persistência.

`Frontend -> API Gateway -> Cart Service -> PostgreSQL do carrinho`

O resumo atual dos produtos é fornecido pelo Cart Service a partir do Product Service. O frontend não duplica o catálogo nem cria fonte de verdade, regras de negócio, endpoints ou persistência para o carrinho.

## Clarificações

### Sessão 2026-09-28

- P: O que deve acontecer quando alguém sem sessão válida abre `/cart`? → R: Manter a rota aberta, explicar que é necessário entrar e oferecer um link para `/login`.
- P: A ação de limpar o carrinho deve exigir confirmação? → R: Sim. Apresentar uma confirmação acessível e chamar `DELETE /api/cart` somente após a confirmação.

## Compatibilidade com 004-cart

Esta especificação complementa, e não substitui, [004-cart](../004-cart/spec.md) e seu [contrato OpenAPI](../004-cart/contracts/cart-service-api.openapi.yaml). Os únicos endpoints de carrinho consumidos são:

| Método | Rota | Uso no frontend | Resposta relevante |
|---|---|---|---|
| `GET` | `/api/cart` | Carregar o carrinho autenticado | `200` com `{ items: [...] }`; `503` quando o catálogo necessário à consulta está indisponível |
| `POST` | `/api/cart/items` | Adicionar ou somar produto | `201` para nova linha ou `200` para linha consolidada; ambos retornam o carrinho atualizado |
| `PUT` | `/api/cart/items/{productId}` | Substituir quantidade da linha | `200` com o carrinho atualizado |
| `DELETE` | `/api/cart/items/{productId}` | Remover uma linha | `204`, sem corpo |
| `DELETE` | `/api/cart` | Limpar todas as linhas do usuário | `204`, sem corpo |

Cada item retornado contém `productId`, `quantity`, `available` e `product`. Quando disponível, `product` contém os dados atuais `name`, `price`, `brand` e `imageUrl`; quando indisponível, o contrato retorna `available: false` e `product: null`.

O Cart Service deriva o proprietário exclusivamente do `sub` do JWT e valida o produto ativo. `available` representa a disponibilidade informada pelo Cart Service para o item, não uma reserva ou garantia de estoque. Produto ativo com estoque zero pode ser aceito pelo contrato atual; o frontend não deve criar uma validação de estoque que contradiga esse comportamento. Um item que se tornou indisponível permanece identificável e removível, mas o backend rejeita atualização de quantidade para produto inativo.

As respostas de POST e PUT contêm o estado atualizado. DELETE retorna somente `204`; nesses casos, o frontend atualiza o estado compartilhado pela remoção local determinística da linha ou pela limpeza completa, sem presumir que a resposta contenha itens.

## Cenários de Usuário e Testes

### História de Usuário 1 - Adicionar produto ao carrinho (Prioridade: P1)

Como cliente autenticado, quero adicionar um produto a partir de seus detalhes para encontrá-lo no meu carrinho.

**Por que esta prioridade**: É a entrada principal de itens no carrinho e conecta a descoberta de produtos à experiência de manutenção.

**Teste independente**: Abrir o detalhe de um produto ativo, escolher quantidade positiva, adicionar e confirmar feedback, linha atualizada no estado compartilhado e contador do header atualizado a partir da resposta do Cart Service.

**Cenários de Aceitação**:

1. **Dado** um cliente autenticado no detalhe de um produto, **quando** escolher uma quantidade inteira positiva e adicionar, **então** o frontend envia `POST /api/cart/items` com `productId` e `quantity`, sem `userId`.
2. **Dado** que a inclusão retorna `201` ou `200`, **quando** a resposta for recebida, **então** o frontend substitui o estado do carrinho pelo `CartResponse` retornado, atualiza o badge e informa sucesso.
3. **Dado** que a inclusão está em andamento, **quando** o cliente acionar novamente a ação, **então** a interface impede submissões repetidas até a conclusão da solicitação.
4. **Dado** que o produto está inativo ou não existe, **quando** o Cart Service rejeitar a inclusão com `404`, **então** o frontend apresenta uma mensagem compreensível e não acrescenta uma linha local.
5. **Dado** que o usuário não está autenticado, **quando** tentar adicionar, **então** a operação não é tratada como bem-sucedida e a interface oferece acesso à rota de login existente.
6. **Dado** que o produto está ativo mas o catálogo informa quantidade zero, **quando** o cliente tentar adicioná-lo, **então** o frontend não inventa uma regra de estoque; o resultado do Cart Service determina o sucesso ou erro.

### História de Usuário 2 - Consultar e manter o próprio carrinho (Prioridade: P1)

Como cliente autenticado, quero revisar meus itens, ajustar quantidades, remover linhas ou limpar o carrinho.

**Por que esta prioridade**: A consulta e a manutenção são a experiência central após a inclusão de produtos.

**Teste independente**: Com um carrinho previamente preparado no backend, abrir `/cart`, verificar os dados recebidos e exercitar PUT, DELETE de item e DELETE do carrinho, confirmando cada estado na interface.

**Cenários de Aceitação**:

1. **Dado** um cliente autenticado, **quando** abrir `/cart`, **então** o frontend consulta `GET /api/cart` e apresenta apenas o estado retornado para o token atual.
2. **Dado** um carrinho com produtos disponíveis, **quando** a página terminar de carregar, **então** apresenta quantidade, imagem quando fornecida, nome, dados atuais disponíveis e controles de quantidade/remover.
3. **Dado** uma linha com quantidade positiva, **quando** o cliente aumentar ou diminuir sua quantidade, **então** o frontend envia `PUT /api/cart/items/{productId}` com a nova quantidade positiva e sincroniza o estado com a resposta `200`.
4. **Dado** uma linha com quantidade igual a um, **quando** o cliente visualizar o controle de diminuição, **então** a interface não envia quantidade zero ou negativa; remoção permanece uma ação separada.
5. **Dado** que uma remoção de item retorna `204`, **quando** a operação concluir, **então** a linha é removida do estado do frontend e o contador é recalculado.
6. **Dado** um carrinho com linhas, **quando** o cliente acionar “Limpar carrinho”, **então** a interface apresenta uma confirmação acessível; somente após confirmar chama `DELETE /api/cart` e, após `204`, apresenta carrinho vazio e contador zero. Se cancelar, o estado permanece inalterado e nenhuma requisição é enviada.
7. **Dado** um produto indisponível (`available: false`, `product: null`), **quando** ele for exibido, **então** a interface comunica indisponibilidade sem acessar campos de `product`, desabilita a alteração de quantidade e mantém disponível a remoção.
8. **Dado** que uma mutation falha, **quando** a resposta de erro for recebida, **então** nenhuma alteração otimista não confirmada permanece no estado e uma mensagem adequada é apresentada.
9. **Dado** que o cliente deseja continuar navegando, **quando** selecionar “Continuar comprando” ou a ação equivalente, **então** retorna ao catálogo existente sem iniciar checkout.

### História de Usuário 3 - Acompanhar e acessar o carrinho (Prioridade: P1)

Como cliente, quero abrir o carrinho pelo cabeçalho e reconhecer quantos itens estão nele durante a navegação.

**Por que esta prioridade**: O acesso persistente e um contador confiável tornam as operações do carrinho descobríveis entre catálogo e detalhes.

**Teste independente**: Carregar uma sessão autenticada com várias linhas e quantidades, navegar entre catálogo, detalhe e carrinho e validar navegação e badge sem chamadas repetidas em cada renderização.

**Cenários de Aceitação**:

1. **Dado** um estado de carrinho carregado, **quando** qualquer rota com o cabeçalho for apresentada, **então** o controle do carrinho é navegável para `/cart` e o badge mostra a soma de `quantity` de todas as linhas.
2. **Dado** uma linha indisponível ainda persistida, **quando** o badge for calculado, **então** a quantidade dessa linha também será contada, pois ela continua no carrinho até ser removida.
3. **Dado** uma sessão autenticada recém-estabelecida ou um estado de carrinho ainda não carregado, **quando** o frontend precisar inicializar o badge, **então** consulta `GET /api/cart` uma vez e reutiliza o estado compartilhado nas rotas; renderizações não disparam novas consultas.
4. **Dado** uma inclusão ou atualização bem-sucedida, **quando** a API retornar `CartResponse`, **então** o estado compartilhado e o badge são atualizados dessa resposta, sem um GET redundante.
5. **Dado** uma remoção ou limpeza bem-sucedida, **quando** a API retornar `204`, **então** o frontend atualiza localmente somente a linha confirmada como removida ou limpa todas as linhas, respectivamente.
6. **Dado** uma sessão encerrada, expirada ou rejeitada com `401`, **quando** o estado de autenticação for invalidado, **então** os dados do carrinho não permanecem visíveis como se pertencessem à sessão seguinte.

### História de Usuário 4 - Entender estados vazios, carregamento e falha (Prioridade: P2)

Como cliente, quero entender quando o carrinho está carregando, vazio ou indisponível e saber como prosseguir.

**Por que esta prioridade**: Estados explícitos evitam que falhas pareçam perda de dados e mantêm a navegação recuperável.

**Teste independente**: Simular resposta vazia, demora, 401, 404, 503 e falha de rede nas operações para validar mensagens, ações e preservação do último estado confirmado.

**Cenários de Aceitação**:

1. **Dado** um carrinho vazio, **quando** `GET /api/cart` retornar `items: []`, **então** a página apresenta estado vazio e ação para explorar o catálogo.
2. **Dado** uma consulta ou mutation em andamento, **quando** o cliente aguardar, **então** a interface apresenta feedback de carregamento e evita cliques duplicados na ação em andamento.
3. **Dado** uma resposta `401` ou ausência de sessão válida, **quando** o cliente abrir ou tentar usar o carrinho, **então** a rota permanece aberta, a interface explica que é necessário autenticar-se e oferece link para `/login`, sem redirecionamento automático nem envio ou solicitação de `userId`.
4. **Dado** uma resposta `404` ao atualizar uma linha, **quando** o produto ou item não puder mais ser atualizado, **então** a interface comunica a indisponibilidade e pode recarregar o carrinho para reconciliar uma alteração concorrente ocorrida fora da tela atual.
5. **Dado** uma resposta `503` ou falha de comunicação, **quando** a consulta ou mutation falhar, **então** a interface apresenta erro recuperável, oferece nova tentativa quando aplicável e preserva o último estado confirmado.
6. **Dado** uma imagem ausente ou que falha, **quando** a linha for renderizada, **então** a interface apresenta um substituto acessível sem quebrar o layout.

## Casos de Borda

- A resposta pode conter itens com `product: null`; nenhum campo do resumo deve ser lido nesse caso.
- Um item indisponível continua contando no badge e pode ser removido; PUT para produto inativo pode retornar `404` conforme o contrato do backend.
- Uma resposta DELETE `204` não contém `CartResponse`; o frontend não deve tentar desserializar um corpo inexistente.
- Uma mutação pode falhar após o cliente iniciar a ação; o estado confirmado anterior deve permanecer até uma resposta de sucesso ou uma consulta bem-sucedida.
- Um `404` durante atualização pode significar produto inativo ou linha removida por outra sessão; a interface não deve presumir qual causa ocorreu se o corpo não a distinguir.
- `GET /api/cart` pode retornar `503` quando o Product Service estiver indisponível; isso não significa que o carrinho persistido foi apagado.
- Quantidade enviada deve ser inteiro positivo; quantidade um não pode ser decrementada para zero por PUT.
- A sessão do projeto é mantida pelo estado de autenticação atual; o carrinho carregado não deve vazar entre mudanças de sessão.
- Em viewports estreitos, nomes longos, controles, erros e ações não podem provocar overflow horizontal nem sobreposição.

## Requisitos

### Requisitos Funcionais

- **RF-001**: O frontend DEVE disponibilizar a rota `/cart` integrada ao roteamento existente e acessível pelo cabeçalho da loja. Sem sessão válida, a rota permanece aberta e apresenta orientação com link para `/login`, sem redirecionamento automático.
- **RF-002**: A página do carrinho DEVE consultar `GET /api/cart` através do `apiClient` existente; não deve acessar diretamente o Cart Service fora do Gateway.
- **RF-003**: O frontend DEVE reutilizar a sessão e o interceptor de autenticação existentes para enviar o JWT quando disponível. Sem sessão válida, deve apresentar orientação com link para `/login` sem iniciar uma consulta autenticada. Não deve solicitar, calcular ou enviar `userId`; o proprietário permanece determinado pelo JWT no Cart Service.
- **RF-004**: A página DEVE apresentar título, quantidade de unidades, lista de itens, dados atuais disponíveis do produto, controles de quantidade, remoção, ação de limpar e navegação para continuar comprando.
- **RF-005**: Para item com `available: true` e `product` presente, o frontend DEVE exibir nome e, quando fornecidos, imagem, marca e preço atuais. Preço pode ser mostrado como informação do produto, mas não deve ser somado ou convertido em total do carrinho.
- **RF-006**: Para item com `available: false` e `product: null`, o frontend DEVE mostrar indicação de indisponibilidade sem inventar dados do produto; deve permitir remover o item e não oferecer atualização de quantidade que o contrato rejeitará.
- **RF-007**: Na página de detalhes, o frontend DEVE oferecer seleção de quantidade inteira positiva e habilitar “Adicionar ao carrinho” para chamar `POST /api/cart/items` usando o `productId` do catálogo.
- **RF-008**: A inclusão DEVE aceitar respostas `201` e `200` como sucesso e sincronizar o carrinho com o corpo retornado, sem implementar soma de quantidades em paralelo ao Cart Service.
- **RF-009**: A alteração de quantidade DEVE usar `PUT /api/cart/items/{productId}` com valor inteiro positivo e sincronizar o estado com a resposta `200`.
- **RF-010**: A remoção de item DEVE usar `DELETE /api/cart/items/{productId}` e tratar `204` sem corpo como confirmação para remover localmente a linha correspondente.
- **RF-011**: A limpeza DEVE exigir confirmação acessível antes de usar `DELETE /api/cart`; somente após confirmação e sucesso `204` o estado local deve ser limpo. Cancelar a confirmação não altera o estado nem envia requisição.
- **RF-012**: O badge do cabeçalho DEVE representar unidades totais, calculadas pela soma de `quantity` em todas as linhas, incluindo as indisponíveis. Não deve contar somente linhas nem permanecer fixo em zero.
- **RF-013**: O estado compartilhado do carrinho DEVE ser inicializado por `GET /api/cart` uma vez quando necessário para a sessão autenticada, ser reutilizado entre rotas e ser atualizado pelos corpos de POST/PUT e pelo resultado confirmado de DELETE. Não deve haver GET a cada renderização nem GET redundante após toda mutation bem-sucedida.
- **RF-014**: O estado do carrinho DEVE ser isolado da sessão anterior e limpo quando a autenticação for invalidada, encerrada ou substituída.
- **RF-015**: A interface DEVE apresentar estados distintos de carregamento, vazio e erro, além de feedback de sucesso para as operações mutáveis.
- **RF-016**: A interface DEVE tratar ao menos `401`, `404`, `503` e falhas de rede conforme a operação, sem apresentar sucesso falso ou perder o último estado confirmado.
- **RF-017**: A interface DEVE impedir submissões repetidas enquanto a mesma ação estiver pendente e impedir envio de quantidades zero, negativas ou não inteiras.
- **RF-018**: O frontend NÃO DEVE inferir a propriedade `available` do estoque `quantity` retornado pelo catálogo nem bloquear a inclusão de produto ativo apenas porque seu estoque informado é zero; o Cart Service existente determina o resultado da inclusão.
- **RF-019**: O frontend DEVE reutilizar o cliente HTTP, autenticação, roteamento, estado e padrões visuais existentes; não deve instalar uma biblioteca de gerenciamento de estado sem necessidade demonstrada.
- **RF-020**: A página e seus controles DEVEM ser acessíveis por teclado, ter rótulos e nomes acessíveis, foco visível, textos alternativos e mensagens de erro anunciáveis por tecnologia assistiva.
- **RF-021**: A experiência DEVE funcionar em desktop, tablet e mobile, reorganizando conteúdo em telas estreitas sem overflow horizontal, corte ou sobreposição.
- **RF-022**: A feature NÃO DEVE criar ou alterar endpoints, regras de negócio, tabelas, migrations ou serviços de backend para o carrinho; deve reutilizar exclusivamente o contrato de 004-cart.
- **RF-023**: Esta feature NÃO DEVE implementar checkout, total financeiro, frete, descontos, pagamento, pedido, reserva de estoque ou alteração do Product Service.

### Dados Consumidos

- **Carrinho**: `items`, retornado pelo Cart Service.
- **Linha do carrinho**: `productId`, `quantity`, `available` e `product` conforme 004-cart.
- **Resumo atual do produto**: `name`, `price`, `brand` e `imageUrl`, somente quando `product` não for nulo.

O frontend não persiste preço ou resumo como fonte autoritativa e não cria entidade de carrinho própria no backend. Estado compartilhado em memória serve somente à interface e deve ser substituído pelas respostas confirmadas da API quando disponíveis.

### Diretriz para o Plano de Implementação

O plano de implementação deve identificar o estado compartilhado já existente e propor o menor estado adicional necessário para manter linhas, carregamento, erros e contador. O badge deve ser derivado do `CartResponse.items` por soma de quantidades. O plano deve documentar um carregamento inicial sob demanda para a sessão autenticada, reutilização entre rotas e reconciliação das mutações sem requisições redundantes. POST/PUT usam o corpo retornado; DELETE/limpeza atualizam o estado após `204`, que não tem corpo. Uma consulta GET adicional deve ficar reservada à carga inicial, nova tentativa explícita ou reconciliação necessária após erro/possível alteração concorrente, não a cada renderização.

## Critérios de Sucesso

### Resultados Mensuráveis

- **CS-001**: Em testes com uma sessão autenticada, a rota `/cart` consulta o carrinho do JWT atual e não envia `userId` em nenhuma operação.
- **CS-002**: Após carga inicial e cada mutation bem-sucedida, o conteúdo e o badge correspondem ao estado confirmado pela API; o badge equivale à soma das quantidades de todas as linhas.
- **CS-003**: As cinco operações existentes podem ser acionadas pelo frontend e enviam somente os métodos, paths e corpos definidos em 004-cart.
- **CS-004**: Em testes de estados disponível e indisponível, nenhum acesso a campo nulo de `product` ocorre; item indisponível é identificado e removível.
- **CS-005**: Respostas vazias, carregamentos, erros 401/404/503 e falhas de rede são apresentados em estados distinguíveis, sem mutação visual não confirmada.
- **CS-006**: Cliques repetidos durante uma operação pendente não geram requests duplicados para essa ação.
- **CS-007**: Em viewports desktop, tablet e mobile, não há overflow horizontal ou sobreposição que impeça acesso a conteúdo e ações essenciais.
- **CS-008**: Os testes automatizados cobrem consulta vazia/com itens, inclusão, alteração, remoção, limpeza, produto indisponível, erros, autenticação, badge e navegação entre detalhe, carrinho e catálogo.

## Premissas

- Os endpoints de 004-cart estão disponíveis atrás do API Gateway e mantêm seus métodos, paths, payloads e códigos HTTP documentados.
- O `apiClient` atual usa `VITE_API_URL` e adiciona o JWT da sessão válida; o Cart Service continua validando o token e derivando o proprietário de `sub`.
- O login existente é a única forma de estabelecer sessão nesta feature. A especificação não adiciona persistência de sessão ou novo fluxo de autenticação.
- O frontend pode exibir preço atual por produto retornado, mas não calcula total, subtotal, imposto, desconto ou frete.
- O resumo fornecido pelo backend pode estar ausente para produto inativo; `productId` permanece disponível para identificar e remover a linha.
- Falhas de catálogo podem fazer a consulta completa do carrinho responder `503`; isso não altera a persistência do carrinho.

## Fora do Escopo

- Criar ou alterar o Cart Service, Product Service, API Gateway, banco de dados, tabelas, migrations ou endpoints.
- Criar regras de estoque, reservar ou deduzir estoque, ou garantir disponibilidade para compra futura.
- Checkout, endereços, frete, pedidos, pagamentos, cupons, descontos, faturamento ou histórico de compra.
- Calcular total financeiro do carrinho ou persistir preço no frontend/backend.
- Criar novo mecanismo de autenticação, enviar `userId` ou mudar a propriedade do carrinho.
- Duplicar catálogo, armazenar uma cópia autoritativa dos produtos ou introduzir uma biblioteca de estado sem justificativa.
