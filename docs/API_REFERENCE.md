# Referência da API — Imunidata

Base URL (local): `http://localhost:{PORT}/api/v1`  
Base URL (produção): `https://imunidata.onrender.com/api/v1`

---

## Endpoints

### 1) GET `/api/v1/vacinacao`

Lista registros com filtros opcionais + paginação in-memory.

**Query params (todos opcionais):**

| Parâmetro | Descrição | Exemplo |
|-----------|-----------|---------|
| `vacina` | Nome da vacina | `Vacina dengue (atenuada)` |
| `estado` | Sigla UF | `SP` |
| `municipio` | Município | `SAO PAULO` |
| `coDocumento` | ID do documento | `c8a9789c-...` |
| `coPaciente` | Hash do paciente | `3ebb002b...` |
| `sexo` | Sexo | `M`, `F` |
| `racaCor` | Raça/cor | `BRANCA` |
| `idade` | Idade | `13` |
| `estabelecimento` | UBS/hospital | `UBS VILA TEREZINHA` |
| `dataVacina` | Data | `2026-01-01` |
| `dose` | Dose | `1ª Dose` |
| `localAplicacao` | Local no corpo | `Deltoides do Braco Esquerdo` |
| `viaAdministracao` | Via | `Intramuscular` |
| `loteVacina` | Lote | `561413` |
| `fabricante` | Fabricante | `FIOCRUZ` |
| `estrategia` | Estratégia | `Rotina`, `Campanha` |
| `origemRegistro` | Origem | `Transcricao de caderneta` |
| `page` | Página (padrão: `0`) | `0` |
| `limit` | Itens por página (padrão: `20`, máx: `100`) | `10` |

**Respostas:**
- `200 OK` — array paginado de registros
- `404 Not Found` — nenhum resultado para os filtros

Exemplo `200`:
```json
[
  {
    "id": 1,
    "coDocumento": "c8a9789c-a4d7-4851-a698-bd39895f922c-i0b0",
    "coPaciente": "3ebb002b287e37b4e3713b9cc3bac86438e3ad0b",
    "sexo": "M",
    "racaCor": "BRANCA",
    "municipio": "SAO PAULO",
    "estado": "SP",
    "idade": 13,
    "estabelecimento": "UBS VILA TEREZINHA",
    "vacina": "Vacina dengue (atenuada)",
    "dataVacina": "2026-01-01",
    "dose": "1ª Dose",
    "localAplicacao": "Face Externa Inferior do Braco Esquerdo",
    "viaAdministracao": "Subcutanea",
    "loteVacina": "561413",
    "fabricante": "IDT BIOLOGIKA GMBH",
    "estrategia": "Rotina",
    "origemRegistro": "Transcricao de caderneta"
  }
]
```

---

### 2) GET `/api/v1/vacinacao/{id}`

Busca um registro pelo índice sequencial. Resolução O(1) via `ConcurrentHashMap`.

**Respostas:**
- `200 OK` — objeto do registro
- `404 Not Found` — ID não existe

Exemplo `404`:
```json
{
  "message": "Recurso não encontrado",
  "status": "404 NOT_FOUND",
  "error": "Registro com ID 999 não encontrado",
  "timestamp": 1748000000000
}
```

---

### 3) POST `/api/v1/vacinacao`

Cria um novo registro. Verifica duplicata por `coDocumento`.

**Body (JSON):**
```json
{
  "municipio": "SAO PAULO",
  "estado": "SP",
  "vacina": "Vacina dengue (atenuada)",
  "dataVacina": "2026-02-01",
  "dose": "1ª Dose",
  "estabelecimento": "UBS CENTRO",
  "fabricante": "IDT BIOLOGIKA GMBH",
  "estrategia": "Rotina",
  "origemRegistro": "Sistema de informacao"
}
```

**Respostas:**
- `201 Created` — retorna objeto criado com `id` gerado
- `409 Conflict` — `coDocumento` já existe

---

### 4) POST `/api/v1/vacinacao/upload`

Faz upload de arquivo CSV no formato OpenDataSUS.

- Detecta encoding automaticamente (UTF-8 → ISO-8859-1)
- Pula linhas inválidas sem abortar
- Ignora duplicatas por `coDocumento`
- Tamanho máximo: 50MB

```bash
curl -X POST http://localhost:8080/api/v1/vacinacao/upload \
  -F "file=@vacinacao.csv"
```

**Respostas:**
- `200 OK` — retorna quantidade de registros inseridos
- `500 Internal Server Error` — arquivo não é CSV ou falha de leitura

Exemplo `200`:
```json
{
  "mensagem": "CSV carregado com sucesso",
  "registrosInseridos": 20
}
```

---

### 5) PUT `/api/v1/vacinacao/{id}`

Atualiza os campos de um registro existente.

**Respostas:**
- `200 OK` — objeto atualizado
- `404 Not Found` — ID não existe

---

### 6) DELETE `/api/v1/vacinacao/{id}`

Remove o registro do banco e do cache in-memory.

**Respostas:**
- `204 No Content` — removido com sucesso
- `404 Not Found` — ID não existe

---

### 7) GET `/healthz`

Health check de liveness. Também chamado internamente a cada 60s pelo `HealthCheckScheduler`.

**Resposta:** `200 OK` com corpo `OK`

---

## Formato padrão de erro

```json
{
  "message": "descrição legível",
  "status": "404 NOT_FOUND",
  "error": "mensagem detalhada da exceção",
  "timestamp": 1748000000000
}
```

## Códigos HTTP resumidos

| Código | Quando |
|--------|--------|
| `200` | GET/PUT com sucesso, upload CSV |
| `201` | POST criou registro |
| `204` | DELETE com sucesso |
| `404` | ID inexistente, filtros sem resultado |
| `409` | `coDocumento` duplicado |
| `500` | Erro inesperado, CSV inválido |
| `503` | Serviço externo indisponível |
