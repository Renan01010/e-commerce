# Feature Specification: Carrinho de Compras

**Feature Branch**: `004-cart`  
**Created**: 2026-09-27  
**Status**: Draft  
**Input**: Descrição fornecida para a feature 004-cart do TechStore.

## Clarifications

### Session 2026-09-27

- Q: Quais dados cada item deve retornar em `GET /api/cart`? → A: Incluir o resumo atual do Product Service (`name`, `price`, `brand` e `imageUrl`), sem persistir snapshot; se o produto estiver inativo, manter a linha indisponível.

## User Scenarios & Testing

### User Story 1 - Consultar o próprio carrinho (Priority: P1)

Como cliente autenticado, quero consultar meu carrinho para revisar os produtos que selecionei.

**Why this priority**: Consultar o estado do carrinho é a jornada central e também permite validar o isolamento entre usuários.

**Independent Test**: Preparar itens para um usuário e manter outro sem itens, autenticar ambos e consultar `GET /api/cart`; cada resposta deve conter exclusivamente as linhas do `sub` autenticado e o carrinho sem itens deve ser representado vazio. A preparação inicial não usa as operações de manutenção da User Story 2.

**Acceptance Scenarios**:

1. **Given** um JWT válido cujo `sub` identifica um usuário com itens no carrinho, **When** ele consultar `GET /api/cart`, **Then** recebe seus próprios itens e quantidades.
2. **Given** um usuário sem itens cadastrados no carrinho, **When** consultar `GET /api/cart`, **Then** recebe sucesso com a lista de itens vazia.
3. **Given** um JWT de outro usuário, **When** consultar o carrinho, **Then** o serviço retorna somente o carrinho correspondente ao `sub` desse token.
4. **Given** uma solicitação sem JWT, expirado ou inválido, **When** consultar o carrinho, **Then** recebe `401 Unauthorized` e nenhum dado é retornado.

---

### User Story 2 - Adicionar e manter produtos (Priority: P1)

Como cliente autenticado, quero adicionar produtos, alterar suas quantidades e remover itens para manter meu carrinho atualizado.

**Why this priority**: Essa jornada materializa o conteúdo do carrinho e é necessária para que ele tenha valor além de uma lista vazia.

**Independent Test**: Com estado inicial preparado para o teste, autenticar um usuário, adicionar um produto válido, repetir a adição, substituir a quantidade e remover o item; confirmar pelas respostas das mutações e pelo estado persistido que há uma única linha com a soma correta, a substituição foi aplicada e a linha foi removida. A consulta ponta a ponta após as mutações usa o GET entregue pela User Story 1.

**Acceptance Scenarios**:

1. **Given** um JWT válido e um produto existente e ativo, **When** enviar `POST /api/cart/items` com `productId` e quantidade positiva, **Then** o produto é adicionado ao carrinho do usuário.
2. **Given** que o produto já está no carrinho, **When** adicioná-lo novamente, **Then** o carrinho continua contendo uma única linha para esse UUID e a quantidade é somada à existente.
3. **Given** um produto no próprio carrinho, **When** enviar `PUT /api/cart/items/{productId}` com quantidade positiva, **Then** a quantidade existente é substituída pela nova quantidade.
4. **Given** um item no próprio carrinho, **When** enviar `DELETE /api/cart/items/{productId}`, **Then** somente esse item é removido.
5. **Given** uma quantidade igual a zero ou negativa, **When** tentar adicionar ou atualizar um item, **Then** recebe `400 Bad Request` e o carrinho permanece inalterado.
6. **Given** um UUID de produto inexistente ou inativo, **When** tentar adicionar ou atualizar o item, **Then** a operação é rejeitada e nenhum item inválido é gravado.
7. **Given** dois usuários autenticados diferentes, **When** ambos adicionarem o mesmo UUID de produto, **Then** cada usuário vê apenas sua própria linha e quantidade.
8. **Given** um JWT válido, **When** `productId` não estiver no carrinho do usuário ao removê-lo ou atualizá-lo, **Then** recebe `404 Not Found` sem alterar o carrinho de outro usuário.

---

### User Story 3 - Limpar o carrinho (Priority: P2)

Como cliente autenticado, quero remover todos os itens do meu carrinho para começar novamente.

**Why this priority**: Limpar o carrinho é uma operação conveniente, mas o usuário pode remover cada item individualmente.

**Independent Test**: Preparar várias linhas para um usuário e ao menos uma linha para outro, chamar `DELETE /api/cart` para o primeiro e confirmar que suas linhas foram removidas, as do outro usuário permanecem e uma segunda limpeza retorna `204`. A validação ponta a ponta por consulta usa o GET da User Story 1; criar os itens pela API requer também a User Story 2.

**Acceptance Scenarios**:

1. **Given** um JWT válido e um carrinho com vários itens, **When** enviar `DELETE /api/cart`, **Then** todos os itens do carrinho do usuário são removidos e o serviço responde `204 No Content`.
2. **Given** um carrinho vazio ou ainda não materializado, **When** enviar `DELETE /api/cart`, **Then** o serviço responde `204 No Content` e não altera carrinhos de outros usuários.
3. **Given** um JWT ausente, expirado ou inválido, **When** tentar limpar o carrinho, **Then** recebe `401 Unauthorized` e nenhum dado é alterado.

---

### Edge Cases

- O token é validado pelo Cart Service; `sub` ausente ou que não represente um UUID válido não pode identificar nem criar carrinho.
- O request contém um campo `userId`: ele não pode escolher ou substituir o proprietário; requests não devem aceitar esse campo.
- Duas adições concorrentes do mesmo produto não podem produzir linhas duplicadas nem perder incrementos de quantidade.
- O Product Service informa que um produto não existe ou está inativo: adicionar ou aumentar sua quantidade é rejeitado sem persistência parcial.
- O Product Service está indisponível ou excede o tempo de resposta durante validação: a mutação falha sem gravar parcialmente o item.
- Um produto previamente adicionado deixa de estar ativo: o item continua identificável e removível; a consulta retorna sua linha com `available=false` e sem resumo do produto.
- O JWT é válido, mas o usuário não tem carrinho persistido: a consulta retorna carrinho vazio, sem criar dados para outro usuário.
- Quantidade enviada tem tipo inválido, está ausente ou fora do intervalo permitido: retornar erro de validação sem modificar o estado persistido.

## Requirements

### Functional Requirements

- **FR-001**: Todas as operações do carrinho DEVEM exigir JWT válido; token ausente, inválido ou expirado DEVE resultar em `401 Unauthorized`.
- **FR-002**: O proprietário do carrinho DEVE ser identificado exclusivamente pelo UUID no claim `sub` do JWT validado pelo Cart Service.
- **FR-003**: Requests e paths públicos NÃO DEVEM aceitar `userId` para selecionar o proprietário. O serviço DEVE impedir acesso ou alteração ao carrinho de outro usuário.
- **FR-004**: Cada usuário DEVE ter um carrinho logicamente independente; consultar um usuário sem itens DEVE retornar uma lista `items` vazia.
- **FR-005**: O serviço DEVE expor `GET /api/cart` para consultar o carrinho do usuário autenticado.
- **FR-006**: O serviço DEVE expor `POST /api/cart/items` com `productId` UUID e quantidade maior que zero para adicionar um produto validado.
- **FR-007**: O serviço DEVE expor `PUT /api/cart/items/{productId}` para substituir a quantidade de um item pertencente ao usuário autenticado; a nova quantidade DEVE ser maior que zero.
- **FR-008**: O serviço DEVE expor `DELETE /api/cart/items/{productId}` para remover somente a linha do produto no carrinho do usuário autenticado.
- **FR-009**: O serviço DEVE expor `DELETE /api/cart` para limpar somente o carrinho do usuário autenticado.
- **FR-010**: Um UUID de produto DEVE aparecer no máximo uma vez no mesmo carrinho. Adições repetidas DEVEM consolidar a linha existente e somar a quantidade solicitada.
- **FR-011**: Antes de adicionar um produto ou substituir a quantidade de um item, o Cart Service DEVE consultar a API REST do Product Service para confirmar que o produto existe e está ativo. O Cart Service NÃO DEVE acessar diretamente o banco do Product Service.
- **FR-012**: O Cart Service DEVE possuir e controlar seu próprio banco PostgreSQL, isolado do banco do Product Service.
- **FR-013**: A indisponibilidade do Product Service durante a validação DEVE impedir a mutação e não pode deixar gravação parcial do carrinho.
- **FR-014**: O Cart Service NÃO DEVE reservar, deduzir ou validar estoque em tempo real. Disponibilidade para compra final será revalidada em incremento futuro.
- **FR-015**: O Cart Service e o Product Service DEVEM validar localmente os JWTs recebidos e aplicar as próprias regras; o Gateway apenas encaminha as rotas do carrinho.
- **FR-016**: Senhas, tokens JWT, cabeçalhos Authorization e secrets NÃO DEVEM ser registrados em logs.
- **FR-017**: Esta feature NÃO DEVE implementar checkout, pedidos, pagamentos, estoque, eventos Kafka ou notificações.

### Contrato HTTP

| Método e rota | Request | Sucesso | Erros principais |
|---|---|---|---|
| `GET /api/cart` | Nenhum body | `200 OK`, carrinho do usuário com itens contendo `productId`, `quantity`, `available` e, quando disponível, resumo atual do produto (`name`, `price`, `brand`, `imageUrl`) | `401 Unauthorized`, `503 Service Unavailable` se Product Service não responder |
| `POST /api/cart/items` | `{"productId":"<UUID>","quantity":1}` | `201 Created` para nova linha; `200 OK` quando consolida linha existente; retorna carrinho atualizado | `400 Bad Request`, `401 Unauthorized`, `404 Not Found` para produto inexistente/inativo, `503 Service Unavailable` |
| `PUT /api/cart/items/{productId}` | `{"quantity":2}` | `200 OK`, carrinho atualizado | `400 Bad Request`, `401 Unauthorized`, `404 Not Found` para item/produto ausente/inativo, `503 Service Unavailable` |
| `DELETE /api/cart/items/{productId}` | Nenhum body | `204 No Content` | `401 Unauthorized`, `404 Not Found` se o item não pertence ao carrinho do usuário |
| `DELETE /api/cart` | Nenhum body | `204 No Content`, inclusive se vazio | `401 Unauthorized` |

Os requests de inclusão e atualização não aceitam `userId`. Valores de quantidade devem ser inteiros positivos. Erros de validação usam o formato comum de erro do TechStore e não incluem dados de outros usuários.

Uma linha disponível na resposta de `GET /api/cart` contém `available: true` e o resumo atual em `product`. Se o Product Service indicar que o produto está inativo ou não existe, a linha permanece com `available: false` e `product: null`, para que continue identificável e removível.

### Key Entities

- **Cart**: carrinho pertencente a um único usuário identificado pelo UUID do claim `sub`; contém zero ou mais itens. Não expõe nem aceita outro proprietário indicado pelo cliente.
- **CartItem**: linha do carrinho identificada por UUID do produto, com quantidade positiva. `productId` é único dentro do carrinho. A resposta pode ser enriquecida com resumo atual do Product Service, que não é persistido pelo Cart Service.
- **Product**: informação de catálogo pertencente exclusivamente ao Product Service. O Cart Service usa sua API para validar e consultar produtos, sem persistir cópia autoritativa do catálogo.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Em testes com pelo menos dois usuários autenticados, 100% das consultas e mutações de cada usuário afetam exclusivamente seu próprio carrinho.
- **SC-002**: Após múltiplas adições do mesmo UUID ao mesmo carrinho, existe exatamente uma linha para esse produto e sua quantidade é igual à soma das quantidades adicionadas.
- **SC-003**: 100% das tentativas sem JWT válido são rejeitadas antes de alterar dados persistidos.
- **SC-004**: Cada uma das cinco operações públicas previstas pode ser concluída e observada no próprio carrinho por uma consulta subsequente.
- **SC-005**: Produto inválido/inativo, quantidade inválida ou indisponibilidade do Product Service não deixa mutação parcial no carrinho.

## Assumptions

- O `sub` do JWT é um UUID; o Cart Service obtém essa identidade após validar localmente assinatura e expiração.
- Adicionar um produto que já existe no carrinho incrementa sua quantidade; `PUT` substitui a quantidade.
- O carrinho é criado/materializado na primeira mutação; consulta sem cart persistido responde com `items: []`.
- A persistência do Cart Service guarda somente identidade do produto e quantidade. Não guarda preço, estoque ou cópia de descrição; o Product Service permanece fonte de verdade.
- A consulta do carrinho apresenta o resumo atual do produto obtido pela API do Product Service; esses dados não são snapshot nem fonte de verdade local.
- Se o Product Service não responder durante uma operação que precise validar/consultar produto, a operação falha como indisponível, sem alterar o carrinho.
- Remover item inexistente retorna `404 Not Found`; limpar carrinho vazio é idempotente e retorna `204 No Content`.
- O Product Service e a rede privada já estão disponíveis como dependências deste incremento; este documento não cria funções de catálogo nem contratos de produto novos.

## Out of Scope

- Checkout e conversão do carrinho em pedido.
- Pagamentos, faturamento, descontos, impostos e cálculo de total final.
- Reserva, movimentação ou sincronização de estoque.
- Persistência de pedido ou histórico de compras.
- Kafka, eventos de domínio e notificações.
- Acesso ao database do Product Service ou autoridade local sobre o catálogo.