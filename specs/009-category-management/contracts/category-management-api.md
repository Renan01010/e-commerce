# Delta do Contrato REST: Gerenciamento de Categorias

Este documento descreve a evolução do contrato atual, cuja fonte OpenAPI permanece [product-service-api.openapi.yaml](../../001-foundation-product-catalog/contracts/product-service-api.openapi.yaml). Não cria uma segunda API nem substitui as rotas atuais.

## Autenticação e visibilidade

- Leituras padrão de categoria ativa permanecem públicas através do Gateway.
- Criação, edição, desativação e reativação exigem JWT com role ADMIN.
- `includeInactive=true` em listagem ou consulta por ID exige ADMIN. Ausência de autenticação retorna `401`; usuário autenticado sem ADMIN retorna `403`.
- O ator de auditoria vem do principal autenticado; requests não escolhem `createdBy`/`updatedBy`.

## Operações

| Método e rota | Request | Sucesso | Erros relevantes |
|---|---|---|---|
| `GET /api/categories` | Query opcional `search`; query `includeInactive=true` somente ADMIN | `200` com lista; pública active-only por padrão | `401`/`403` se pedir inativas sem autorização |
| `GET /api/categories/{id}` | Query opcional `includeInactive=true` somente ADMIN | `200` com categoria incluindo slug | `404` se ausente ou inativa para leitura pública; `401`/`403` para includeInactive sem permissão |
| `POST /api/categories` | `name`, `description?`, `parentCategoryId?`, `slug?` | `201 CategoryResponse` | `400` validação; `401`; `403`; `404` pai inválido; `409` nome/slug duplicado |
| `PUT /api/categories/{id}` | Campos existentes editáveis e `slug?`; slug omitido preserva valor | `200 CategoryResponse` | `400`; `401`; `403`; `404`; `409` nome/slug duplicado |
| `DELETE /api/categories/{id}` | Sem body | `204` desativada logicamente | `401`; `403`; `404`; `409` se categoria tiver produtos |
| `PATCH /api/categories/{id}/activation` | **Sem body** | `200 CategoryResponse` ativada | `401`; `403`; `404` |

## Slug

`CategoryResponse` adiciona `slug` (string canônica, obrigatória e única inclusive em registros inativos). Em criação, slug é opcional: omitido deriva de `name`; informado é normalizado. Em update, omissão preserva o slug existente. Slug/nome duplicado retorna `409` sem mutação parcial.

## Migração de registros existentes

Na migration, cada categoria existente recebe candidato derivado do nome, inclusive inativas. Colisões abortam antes de gravação/constraint e retornam diagnóstico com IDs, nomes e candidatos. A retomada utiliza o mapa externo `CATEGORY_SLUG_OVERRIDES_JSON` (`categoryId -> slug`) cobrindo todos os IDs dos grupos conflitantes; o conjunto final é validado globalmente. Não há sufixo automático. Detalhes operacionais estão em [quickstart.md](../quickstart.md).

## ErrorResponse

Erros usam o envelope comum do Product Service: `status`, `message`, `details`, `correlationId` e `timestamp`. Status previstos: `400` para validação, `401` não autenticado, `403` sem ADMIN, `404` ausente/oculto por visibilidade e `409` por unicidade ou regra de categoria vinculada a produtos.