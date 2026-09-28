# Guia de Validação: Gerenciamento de Categorias

## Pré-requisitos

- Java 21 e Maven compatíveis com o monorepo.
- Docker disponível para Testcontainers e PostgreSQL de desenvolvimento.
- API Gateway e Product Service existentes; JWT ADMIN válido emitido pelo User Service.
- Backup do database do Product Service antes de aplicar a migration de slug em ambiente com dados existentes.

## Verificações Automatizadas

Na raiz do repositório:

```powershell
mvn -f backend/pom.xml -pl product-service -am test
mvn -f backend/pom.xml -pl product-service -am package -DskipTests
```

Resultado esperado: testes unitários, MockMvc/OpenAPI e integrações PostgreSQL/Testcontainers aprovados; empacotamento do Product Service concluído.

## Procedimento Operacional: Backfill de Slugs

1. Antes do upgrade, confirmar backup e iniciar o Product Service com `CATEGORY_SLUG_OVERRIDES_JSON` ausente/vazio. A migration calcula candidatos para todas as categorias, inclusive inativas, e valida o conjunto antes de gravar.
2. Sem colisões, Flyway persiste os slugs e ativa a constraint `NOT NULL`/única. Confirmar que API, schema e referências continuam disponíveis.
3. Se a migration detectar colisões, o Product Service não conclui o startup; o diagnóstico lista `categoryId`, nome e slug candidato. A transação deve reverter e não deixar slugs/constraints parciais.
4. Para resolver, escolher explicitamente um slug canônico para cada ID de todos os grupos reportados. Não alterar nomes, IDs, parent nem produtos e não aceitar sufixos gerados automaticamente.
5. Configurar o mapa completo no ambiente de deployment, por exemplo:

   ```powershell
   $env:CATEGORY_SLUG_OVERRIDES_JSON = '{"<categoryId-1>":"slug-aprovado-1","<categoryId-2>":"slug-aprovado-2"}'
   ```

6. Reiniciar/reexecutar o Product Service. A migration verifica que o mapa cobre todos os IDs em conflito, que cada valor é canônico e que não há colisão com qualquer slug candidato ou categoria inativa. Se inválido/incompleto, falha novamente antes de gravar e informa os conflitos restantes.
7. Após Flyway concluir, validar slugs por ID, contagem de linhas e integridade de `parent_category_id`/`products.category_id`; remover `CATEGORY_SLUG_OVERRIDES_JSON` da configuração. O mapa não é persistido como tabela.

## Validação das Operações

Com o Gateway/Product Service ativos e `TOKEN_ADMIN` contendo role ADMIN:

- `GET /api/categories` sem autenticação retorna apenas categorias ativas; busca com `?search=...` corresponde a nome, descrição e slug.
- `GET /api/categories?includeInactive=true` sem ADMIN retorna `401` ou `403`; com ADMIN retorna ativas e inativas.
- `POST /api/categories` com `name` e slug omitido retorna `201` com slug gerado; repetir nome ou slug retorna `409` sem novo registro.
- POST com slug canônico explícito retorna o mesmo slug normalizado. Nome/descrição/slug inválidos retornam `400`.
- `PUT /api/categories/{id}` atualiza os campos permitidos; sem `slug`, mantém o slug anterior. Colisão retorna `409` sem alteração parcial.
- `DELETE /api/categories/{id}` retorna `204` para categoria sem produtos e a remove somente da listagem pública; com produtos vinculados retorna `409`.
- `PATCH /api/categories/{id}/activation` é enviado **sem body**, retorna `200` e a categoria reaparece em `GET /api/categories`.
- Requests sem ADMIN não modificam categorias. As respostas de erro seguem o `ErrorResponse` comum documentado no contrato.

Os testes HTTP podem usar MockMvc para autorização/contrato; os testes de migration e constraints devem usar PostgreSQL Testcontainers. O fluxo de colisão deve cobrir migration sem mapa (diagnóstico e rollback), mapa completo válido, mapa incompleto, mapa inválido e slug ainda duplicado.