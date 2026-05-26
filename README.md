#  Imunidata — Sistema de Monitoramento de Vacinação

> Backend REST em Java (Spring Boot) para consulta e gerenciamento de registros de vacinação pública.  
> Dados baseados no **OpenDataSUS** — carregados automaticamente via CSV no startup e mantidos em cache in-memory.

---

##  Autores

| Nome | Papel |
|------|-------|
| Yasmin Feliciano Serafim | Desenvolvimento |
| Leandro Costa Belfor | Desenvolvimento |

---

## ️ Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTE                              │
│          (Browser / Postman / Frontend React)               │
└──────────────────────────┬──────────────────────────────────┘
                           │  HTTP Request (GET, POST, PUT, DELETE)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     CONTROLLER LAYER                        │
│        RegistroVacinacaoController  /api/v1/vacinacao       │
│                HealthController     /healthz                │
│                                                             │
│  • Recebe requisições HTTP                                  │
│  • Valida e repassa parâmetros ao Service                   │
│  • Aplica paginação in-memory (page + limit)                │
│  • Retorna ResponseEntity com status HTTP adequado          │
└──────────────────────────┬──────────────────────────────────┘
                           │  Chamada de método Java
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                          │
│               RegistroVacinacaoService                      │
│                                                             │
│  • Regras de negócio                                        │
│  • Cache in-memory: synchronizedList + ConcurrentHashMap<id,obj> + newKeySet<coDoc> │
│  • Carrega CSV no startup (@PostConstruct)                  │
│  • Aceita upload de novo CSV via método dedicado            │
│  • Separador ';', multi-encoding, multi-formato de data     │
│  • Filtros via streams (sem query ao banco)                 │
│  • Busca por ID em O(1) via Map                             │
│  • Verifica duplicatas por co_documento em O(1) via Set     │
│  • Lança exceções customizadas                              │
└──────────────────────────┬──────────────────────────────────┘
                           │  Persiste/lê apenas no startup e CRUD
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                         │
│            RegistroVacinacaoRepository                      │
│            (extends JpaRepository<T, Long>)                 │
│                                                             │
│  • Interface gerenciada pelo Spring Data JPA                │
│  • Usada para persistência (save/delete)                    │
│  • Leituras são feitas via cache, não direto ao banco       │
└──────────────────────────┬──────────────────────────────────┘
                           │  SQL gerado automaticamente
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                       BANCO DE DADOS                        │
│                   H2 (in-memory)                            │
│                   Tabela: registro_vacinacao                │
│                                                             │
│  • Banco relacional em memória (recriado a cada startup)    │
│  • Populado automaticamente via CSV no boot                 │
│  • Acessível via H2 Console em /h2-console                  │
└─────────────────────────────────────────────────────────────┘

           ┌──────────────────────────────┐
           │     TRATAMENTO DE ERROS      │
           │   @ControllerAdvice          │
           │   (utils/ExceptionHandler)   │
           │                              │
           │  Intercepta exceções de      │
           │  qualquer camada e retorna   │
           │  JSON padronizado (400, 404, │
           │  409, 500, 503)              │
           └──────────────────────────────┘

           ┌──────────────────────────────┐
           │    FILTRO DE LOG             │
           │   RequestLoggingFilter       │
           │                              │
           │  Registra método, path,      │
           │  status e tempo (ms) de      │
           │  cada requisição HTTP        │
           └──────────────────────────────┘

           ┌──────────────────────────────┐
           │    HEALTH PING SCHEDULER     │
           │   HealthCheckScheduler       │
           │                              │
           │  Chama /healthz a cada 60s   │
           │  internamente para evitar    │
           │  hibernação em PaaS gratuito │
           └──────────────────────────────┘
```

---

##  Estrutura de Pacotes

```
src/main/java/com/example/imunidata/
├── ImunidataApplication.java              # Ponto de entrada Spring Boot
├── config/
│   ├── CorsConfig.java                    # Configuração de CORS
│   ├── HealthCheckScheduler.java          # Pinga /healthz a cada 60s
│   ├── OpenApiConfig.java                 # Configuração Swagger/OpenAPI
│   └── RequestLoggingFilter.java          # Log de todas as requisições HTTP
├── controller/
│   ├── RegistroVacinacaoController.java   # CRUD + upload /api/v1/vacinacao
│   └── HealthController.java              # GET /healthz
├── model/
│   ├── RegistroVacinacao.java             # Entidade JPA (OpenDataSUS)
│   └── ErrorResponse.java                 # DTO de erro + exceções customizadas
├── repository/
│   └── RegistroVacinacaoRepository.java   # Interface JpaRepository (persistência)
├── service/
│   └── RegistroVacinacaoService.java      # Regras de negócio + cache in-memory
└── utils/
    └── ExceptionHandler.java              # @ControllerAdvice global

src/main/resources/
├── application.properties                 # Configurações gerais
└── data/
    └── vacinacao.csv                      # Dados OpenDataSUS (carregados no boot)
```

---

## ️ Entidade Principal — `RegistroVacinacao` (OpenDataSUS)

| Campo | Tipo | Coluna CSV | Descrição | Exemplo |
|-------|------|------------|-----------|---------|
| `id` | Long | — | Índice sequencial (gerado automaticamente) | `1` |
| `coDocumento` | String | `co_documento` | ID único do documento | `"c8a9789c-..."` |
| `coPaciente` | String | `co_paciente` | Hash anonimizado do paciente | `"3ebb002b..."` |
| `sexo` | String | `tp_sexo_paciente` | Sexo do paciente | `"M"`, `"F"` |
| `racaCor` | String | `no_raca_cor_paciente` | Raça/cor | `"BRANCA"` |
| `municipio` | String | `no_municipio_paciente` | Município | `"SAO PAULO"` |
| `estado` | String (2) | `sg_uf_paciente` | UF | `"SP"` |
| `idade` | Integer | `nu_idade_paciente` | Idade | `13` |
| `estabelecimento` | String | `no_fantasia_estabelecimento` | Nome da UBS/hospital | `"UBS VILA TEREZINHA"` |
| `vacina` | String | `ds_vacina` | Nome da vacina | `"Vacina dengue (atenuada)"` |
| `dataVacina` | LocalDate | `dt_vacina` | Data da vacinação | `"2026-01-01"` |
| `dose` | String | `ds_dose_vacina` | Dose aplicada | `"1ª Dose"` |
| `localAplicacao` | String | `ds_local_aplicacao` | Local no corpo | `"Deltoides do Braco Esquerdo"` |
| `viaAdministracao` | String | `ds_via_administracao` | Via de aplicação | `"Intramuscular"` |
| `loteVacina` | String | `co_lote_vacina` | Código do lote | `"561413"` |
| `fabricante` | String | `ds_vacina_fabricante` | Fabricante | `"IDT BIOLOGIKA GMBH"` |
| `estrategia` | String | `ds_estrategia_vacinacao` | Estratégia | `"Rotina"`, `"Campanha"` |
| `origemRegistro` | String | `ds_origem_registro` | Origem do registro | `"Transcricao de caderneta"` |

---

##  Endpoints da API

> Base path: `api/v1/vacinacao`

| Método | Rota | Descrição | Status |
|--------|------|-----------|--------|
| GET | `/api/v1/vacinacao` | Lista registros (todos os filtros + paginação) | 200 / 404 |
| GET | `/api/v1/vacinacao/{id}` | Busca por índice ID (O(1) via cache) | 200 / 404 |
| POST | `/api/v1/vacinacao` | Cria novo registro | 201 / 409 |
| POST | `/api/v1/vacinacao/upload` | Upload de arquivo CSV OpenDataSUS | 200 / 500 |
| PUT | `/api/v1/vacinacao/{id}` | Atualiza registro existente | 200 / 404 |
| DELETE | `/api/v1/vacinacao/{id}` | Remove registro pelo ID | 204 / 404 |
| GET | `/healthz` | Health check | 200 |

---

### Parâmetros de filtro — GET `/api/v1/vacinacao`

Todos os parâmetros são opcionais e combináveis:

| Parâmetro | Tipo | Exemplo |
|-----------|------|---------|
| `vacina` | String | `Vacina dengue (atenuada)` |
| `estado` | String | `SP` |
| `municipio` | String | `SAO PAULO` |
| `coDocumento` | String | `c8a9789c-...` |
| `coPaciente` | String | `3ebb002b...` |
| `sexo` | String | `M` |
| `racaCor` | String | `BRANCA` |
| `idade` | Integer | `13` |
| `estabelecimento` | String | `UBS VILA TEREZINHA` |
| `dataVacina` | LocalDate | `2026-01-01` |
| `dose` | String | `1ª Dose` |
| `localAplicacao` | String | `Deltoides do Braco Esquerdo` |
| `viaAdministracao` | String | `Intramuscular` |
| `loteVacina` | String | `561413` |
| `fabricante` | String | `FIOCRUZ` |
| `estrategia` | String | `Rotina` |
| `origemRegistro` | String | `Transcricao de caderneta` |
| `page` | Integer | `0` (padrão: `0`) |
| `limit` | Integer | `20` (padrão: `20`, máximo: `100`) |

---

##  Como executar localmente

```bash
# Compilar
./mvnw clean package

# Executar (porta padrão 8080)
./mvnw spring-boot:run

# Executar simulando ambiente PaaS (ex: Render)
PORT=10000 ./mvnw spring-boot:run
```

### Exemplos de chamadas

```bash
# Listar todos (page=0, limit=20)
curl "http://localhost:8080/api/v1/vacinacao"

# Paginação
curl "http://localhost:8080/api/v1/vacinacao?page=0&limit=5"

# Filtrar por estado + vacina
curl "http://localhost:8080/api/v1/vacinacao?estado=SP&vacina=BCG"

# Filtrar por estratégia
curl "http://localhost:8080/api/v1/vacinacao?estrategia=Campanha"

# Buscar por ID (O(1))
curl "http://localhost:8080/api/v1/vacinacao/1"

# Criar novo registro (POST → 201)
curl -X POST http://localhost:8080/api/v1/vacinacao \
  -H "Content-Type: application/json" \
  -d '{
    "municipio": "SAO PAULO",
    "estado": "SP",
    "vacina": "Vacina dengue (atenuada)",
    "dataVacina": "2026-02-01",
    "dose": "1ª Dose",
    "estabelecimento": "UBS CENTRO",
    "fabricante": "IDT BIOLOGIKA GMBH",
    "estrategia": "Rotina",
    "origemRegistro": "Sistema de informacao"
  }'

# Upload de CSV OpenDataSUS (POST → 200)
curl -X POST http://localhost:8080/api/v1/vacinacao/upload \
  -F "file=@/caminho/para/vacinacao.csv"

# Atualizar (PUT → 200)
curl -X PUT http://localhost:8080/api/v1/vacinacao/1 \
  -H "Content-Type: application/json" \
  -d '{"municipio":"SAO PAULO","estado":"SP","vacina":"BCG","dose":"2ª Dose","dataVacina":"2026-03-01"}'

# Deletar (DELETE → 204)
curl -X DELETE http://localhost:8080/api/v1/vacinacao/1

# Health check
curl http://localhost:8080/healthz
```

---

##  Upload de CSV

O endpoint `POST /api/v1/vacinacao/upload` aceita arquivos CSV no formato OpenDataSUS:

- Separador: **`;`** (ponto e vírgula) — padrão dos arquivos exportados via pandas
- Tenta leitura em **UTF-8** e depois **ISO-8859-1 (Latin-1)** automaticamente
- Aceita datas nos formatos: `yyyy-MM-dd`, `dd/MM/yyyy`, `d/M/yyyy`, `yyyy/MM/dd`
- Pula linhas inválidas (< 17 colunas) sem abortar
- Ignora duplicatas por `co_documento` em O(1) via `ConcurrentHashMap.newKeySet()` (não sobrescreve)
- Retorna quantos registros foram inseridos e uma mensagem explicativa caso seja 0

```bash
curl -X POST http://localhost:8080/api/v1/vacinacao/upload \
  -F "file=@vacinacao.csv"
```
Resposta `200 OK` com registros inseridos:
```json
{
  "mensagem": "CSV carregado com sucesso",
  "registrosInseridos": 20
}
```
Resposta `200 OK` sem registros novos (todos duplicados ou com erro de parse):
```json
{
  "mensagem": "CSV processado, mas nenhum registro novo foi inserido (verifique duplicatas ou erros no log)",
  "registrosInseridos": 0
}
```

> O CSV deve ter **17 colunas** separadas por `;`. Exemplo gerado via pandas:
> ```python
> df.to_csv("vacinacao.csv", sep=';', index=False, encoding='latin-1')
> ```

---

##  Swagger UI

| Interface | URL |
|-----------|-----|
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/v1/api-docs |

---

## ️ H2 Console

> ⚠️ Disponível apenas enquanto a JVM estiver rodando.

1. Acesse: http://localhost:8080/h2-console
2. Preencha:
   - **JDBC URL:** `jdbc:h2:mem:vacinacaodb;DB_CLOSE_DELAY=-1`
   - **User Name:** `sa`
   - **Password:** *(em branco)*

```sql
SELECT * FROM registro_vacinacao;
SELECT COUNT(*) FROM registro_vacinacao;
SELECT * FROM registro_vacinacao WHERE estado = 'SP';
SELECT vacina, COUNT(*) FROM registro_vacinacao GROUP BY vacina;
```

---

##  Códigos HTTP

| Código | Quando ocorre |
|--------|---------------|
| `200 OK` | GET com resultado, PUT, upload de CSV |
| `201 Created` | POST criou registro |
| `204 No Content` | DELETE com sucesso |
| `404 Not Found` | ID inexistente, filtros sem resultado |
| `409 Conflict` | `co_documento` duplicado |
| `500 Internal Server Error` | Erro inesperado, CSV inválido |
| `503 Service Unavailable` | Falha em serviço externo |

---

## ⚙️ Variáveis de ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `PORT` | `8080` | Porta da aplicação (Render define automaticamente) |

---

##  Deploy (Render / PaaS)

- Porta lida via `server.port=${PORT:8080}`
- `HealthCheckScheduler` pinga `/healthz` a cada 60s para evitar hibernação
- Configure health check da plataforma: path `/healthz`, protocolo HTTP

---

## ️ Tecnologias

| Tecnologia | Versão | Motivo |
|------------|--------|--------|
| Java | 21 | LTS estável |
| Spring Boot | 3.3.5 | Framework padrão REST |
| Spring Data JPA | — | Persistência sem SQL manual |
| H2 Database | — | In-memory, zero configuração |
| OpenCSV | 5.9 | Leitura de CSV com suporte a encoding |
| Springdoc OpenAPI | — | Swagger automático via anotações |
| ConcurrentHashMap | — | Cache in-memory thread-safe para O(1) lookup |

---

##  Documentação adicional

| Documento | Descrição |
|-----------|-----------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Decisões arquiteturais e justificativas |
| [API_REFERENCE.md](docs/API_REFERENCE.md) | Referência completa dos endpoints |
| [ERROR_HANDLING.md](docs/ERROR_HANDLING.md) | Formato de erros e exceções |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | Deploy local e produção |
