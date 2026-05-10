# Referência da API — Imunidata

Base URL (local): http://localhost:{PORT}

Endpoints principais

1) GET /vacinacao

- Descrição: Lista todos os registros de vacinação ou filtra por `vacina` e/ou `estado` quando parâmetros são fornecidos.
- Query params:
  - `vacina` (opcional) — ex.: `BCG`, `Gripe`
  - `estado` (opcional) — sigla do estado ex.: `SP`, `RJ`
- Respostas:
  - 200 OK — array JSON de registros (pode estar vazio se nenhuma correspondência).
  - 500 Internal Server Error — em caso de erro inesperado.

Exemplo de resposta (200):

```json
[
  {
    "id": 1,
    "municipio": "Sao Paulo",
    "estado": "SP",
    "vacina": "BCG",
    "dose": "1a Dose",
    "quantidadeAplicada": 15234,
    "dataRegistro": "2024-01-15T00:00:00"
  }
]
```

2) GET /vacinacao/{id}

- Descrição: Retorna um registro por ID.
- Path param: `id` (Long)
- Respostas:
  - 200 OK — objeto JSON do registro.
  - 404 Not Found — não existe registro com esse ID.
  - 500 Internal Server Error — erro inesperado.

Exemplo de resposta (404):

```json
{
  "message": "Registro com ID 999 não encontrado",
  "status": "404 NOT_FOUND",
  "error": "Not Found",
  "timestamp": 1670000000000
}
```

3) POST /vacinacao

- Descrição: Cria um novo registro de vacinação.
- Body (JSON):

```json
{
  "municipio": "Sao Paulo",
  "estado": "SP",
  "vacina": "Gripe",
  "dose": "1a Dose",
  "quantidadeAplicada": 1000,
  "dataRegistro": "2024-03-01T00:00:00"
}
```

- Respostas:
  - 201 Created — retorna o objeto criado com o `id` gerado.
  - 409 Conflict — registro já existe (quando município+estado+vacina+dose já existem).
  - 400 Bad Request — payload inválido.
  - 500 Internal Server Error — erro inesperado.

4) GET /healthz

- Descrição: Endpoint simples de health (liveness). Retorna 200 OK com corpo `OK`.

Status codes resumidos

- 200 OK — requisição bem-sucedida (GETs).
- 201 Created — recurso criado (POST).
- 400 Bad Request — JSON inválido, validação falhou.
- 404 Not Found — recurso não encontrado.
- 409 Conflict — tentativa de criar recurso duplicado.
- 500 Internal Server Error — erro interno não tratado.

Notas

- Campos de data usam ISO LocalDateTime (ex.: `2024-01-15T00:00:00`).
- Para integrações, documente os possíveis erros no Swagger/OpenAPI. A aplicação expõe OpenAPI JSON em `/api/v1/api-docs` conforme `application.properties`.

