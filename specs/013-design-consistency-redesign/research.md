# Research: Consistência Visual e Redesign das Telas Existentes

## Decisões

### 1. Tratar o CSS existente da Home como origem visual e formalizar seus valores reutilizáveis

**Decision**: Extrair/organizar em tokens CSS compartilhados os valores dark/tech que já aparecem no header e na Home (`#080e18`, superfícies próximas de `#101a27`, linhas azul-marinho e acento lima `#d2fa57`), preservando a aparência atual da Home. Aplicar esses mesmos tokens às rotas de catálogo, detalhe e carrinho e ao layout standalone de autenticação. Migrar o azul de acento específico do Login para o acento lima existente, mantendo os sinais semânticos de erro/sucesso e o contraste.

**Rationale**: `frontend/src/styles.css` ainda define tokens-base claros (`--paper`, `--white`, `--ink`, `--line`, `--green`) e estilos de catálogo/detalhe/carrinho que os consomem. Mais adiante, o mesmo arquivo aplica valores dark hard-coded ao header e às superfícies da Home. `LoginPage.css` tem valores navy compatíveis com a marca, mas usa acentos azuis próprios. Uma fonte compartilhada para os valores existentes permite alinhar as telas sem introduzir uma segunda paleta e sem mudar o design da Home.

**Alternatives considered**:
- Aplicar overrides isolados por tela: rejeitado porque perpetua valores repetidos e desvio de paleta.
- Trocar globalmente os tokens-base claros sem mapear seus consumidores: rejeitado porque estilos antigos e rotas fora do escopo poderiam mudar incidentalmente.
- Criar uma biblioteca de componentes/design system ou dependência adicional: rejeitado por ser desnecessário para esta aplicação e este escopo.

### 2. Manter o shell compartilhado nas rotas da loja e as telas de autenticação standalone

**Decision**: Preservar `StoreLayout` em `frontend/src/App.tsx` para Home, catálogo, detalhe, carrinho e fallback. Não criar headers por página, não exibir esse header no Login ou no cadastro indisponível e apenas alinhar as composições existentes dessas telas à identidade visual.

**Rationale**: O roteamento já distingue as páginas standalone (`/login`, `/register`) das rotas envolvidas pelo shell. O header existente contém busca condicional à Home, categorias reais, navegação e badge conectado ao `cartStore`; nenhuma dessas responsabilidades precisa ser duplicada para o redesign.

**Alternatives considered**:
- Colocar Login e cadastro dentro do shell: rejeitado pela clarificação registrada na spec.
- Criar um header alternativo nas páginas: rejeitado por duplicar navegação e contrariar a decisão sobre layouts standalone.

### 3. Alterar somente apresentação; reutilizar estado, APIs e comportamento de componentes

**Decision**: Manter consultas, URLs, filtros, ordenação, paginação, sessão, ações do carrinho e respostas de erro nos componentes/stores atuais. Reestilizar `CatalogPage`, `ProductDetailPage`, `CartPage`, `ProductCard`, `CartItem`, `ClearCartConfirmation` e controles de autenticação. Reutilizar o padrão de fallback de mídia de marca e abstrair componente apenas se a inspeção durante implementação demonstrar duplicação concreta que não possa ser resolvida por CSS/markup existente.

**Rationale**: O Catálogo já liga a URL aos critérios no `catalogStore`; Detalhe e Cards usam `cartStore`; Carrinho renderiza valores e estados recebidos pelo serviço. O contrato de consumo em `specs/008-frontend-cart/contracts/cart-gateway-consumption.md` confirma POST/PUT respondem com `CartResponse`, DELETE confirma com 204, e produto/preço podem ser nulos. A camada visual não deve recalcular nem substituir esses dados.

**Alternatives considered**:
- Refatorar stores e clientes junto do redesenho: rejeitado; sem requisito funcional e amplia risco para fluxos existentes.
- Criar estado de preços/quantidades específico da UI: rejeitado porque duplicaria autoridade do serviço.
- Forçar extração de um novo componente de imagem: rejeitado até haver necessidade clara; seguir o princípio da mudança mínima.

### 4. Validar com testes existentes e inspeção responsiva nos quatro viewports

**Decision**: Atualizar os testes unitários/de integração de UI em Vitest/RTL, executar a suíte completa e `npm run build`, e conferir as rotas adaptadas a 1280, 1024, 768 e 375 px. Validar estados interativos usando mocks e manter chamadas externas fora dos testes.

**Rationale**: O frontend já usa Vitest com jsdom, React Testing Library e `user-event`; o build (`tsc -b && vite build`) também valida o TypeScript. As larguras constam dos critérios da spec e requerem conferência visual além dos testes de DOM.

**Alternatives considered**:
- Adicionar framework de testes visuais/browser: rejeitado por não existir atualmente e não ser necessário para cobrir o critério de responsividade.
- Depender de Gateway/serviços reais na suíte: rejeitado por aumentar instabilidade e contrariar os requisitos de mocks.

## Findings

- O header compartilhado e as rotas públicas/standalone já estão em `frontend/src/App.tsx`.
- `styles.css` combina tokens claros legados com overrides dark da Home/header; Catálogo, Detalhe e Carrinho precisam ser trazidos para a identidade dark sem regressão da Home.
- `LoginPage.css` já tem superfícies navy e layout responsivo, mas declara paleta/acento próprios. A rota `/register` renderiza estado de indisponibilidade, não um formulário.
- O catálogo usa URL + `catalogStore` para consulta, busca, categoria, filtros, ordenação e paginação. O redesign não requer alteração dessas fontes nem de lógica.
- O Cart Service continua autoridade para linha, quantidade e valores. A confirmação de carrinho é atualizada com as respostas POST/PUT e os DELETE só confirmam a operação; a UI não deve supor valores.
- Não há necessidade de entidade, persistência, endpoint, dependência ou contrato externo novo.
