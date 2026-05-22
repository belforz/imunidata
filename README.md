# 💉 Imunidata — Sistema de Monitoramento de Vacinação

> Backend REST em Java (Spring Boot) para consulta e gerenciamento de registros de vacinação pública.  
> Dados iniciais baseados no OpenDataSUS — carregados automaticamente via CSV no startup.

---

## 👥 Autores

| Nome | Papel |
|------|-------|
| Yasmin Feliciano Serafim | Desenvolvimento |
| Leandro Costa Belfor | Desenvolvimento |

---

## 🏗️ Arquitetura

A aplicação segue a arquitetura em camadas padrão de APIs REST com Spring Boot:

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTE                              │
│          (Browser / Postman / Frontend React)               │
└──────────────────────────┬──────────────────────────────────┘
                           │  HTTP Request (GET, POST, DELETE)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     CONTROLLER LAYER                        │
│           RegistroVacinacaoController  /vacinacao           │
│                 HealthController       /healthz             │
│                                                             │
│  • Recebe requisições HTTP                                  │
│  • Valida parâmetros de entrada                             │
│  • Delega para o Service                                    │
│  • Retorna ResponseEntity com status HTTP adequado          │
└──────────────────────────┬──────────────────────────────────┘
                           │  Chamada de método Java
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                          │
│               RegistroVacinacaoService                      │
│                                                             │
│  • Contém as regras de negócio                              │
│  • Carrega dados do CSV no startup (@PostConstruct)         │
│  • Verifica duplicados antes de salvar (existsBy...)        │
│  • Lança exceções customizadas (ResourceNotFoundException,  │
│    ResourceAlreadyExistsException, etc.)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │  Chamada ao repositório JPA
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                         │
│            RegistroVacinacaoRepository                      │
│            (extends JpaRepository<T, ID>)                   │
│                                                             │
│  • Interface gerenciada pelo Spring Data JPA                │
│  • Gera queries automaticamente pelo nome do método         │
│    ex.: findByVacina(), findByEstado(),                     │
│         existsByVacinaAndEstadoAndMunicipioAndDose()        │
└──────────────────────────┬──────────────────────────────────┘
                           │  SQL gerado automaticamente (JPQL/HQL)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                       BANCO DE DADOS                        │
│                   H2 (in-memory)                            │
│                   Tabela: registro_vacinacao                │
│                                                             │
│  • Banco relacional em memória (recriado a cada startup)    │
│  • Populado automaticamente pelo CSV no boot                │
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

## 📦 Estrutura de Pacotes

```
src/main/java/com/example/imunidata/
├── ImunidataApplication.java          # Ponto de entrada Spring Boot
├── config/
│   ├── CorsConfig.java                # Configuração de CORS (permite frontend)
│   ├── HealthCheckScheduler.java      # Agendador que pinga /healthz a cada 60s
│   ├── OpenApiConfig.java             # Configuração do Swagger/OpenAPI
│   └── RequestLoggingFilter.java      # Filtro de log de requisições HTTP
├── controller/
│   ├── RegistroVacinacaoController.java  # CRUD /vacinacao
│   └── HealthController.java             # GET /healthz
├── model/
│   ├── RegistroVacinacao.java         # Entidade JPA (tabela registro_vacinacao)
│   └── ErrorResponse.java             # DTO de erro + exceções customizadas
├── repository/
│   └── RegistroVacinacaoRepository.java  # Interface JpaRepository com filtros
├── service/
│   └── RegistroVacinacaoService.java  # Regras de negócio + carga CSV
└── utils/
    └── ExceptionHandler.java          # @ControllerAdvice global de erros

src/main/resources/
├── application.properties             # Configurações de banco, porta, logging
└── data/
    └── vacinacao.csv                  # Dados reais de vacinação (OpenDataSUS)
```

---

## 🗃️ Entidade Principal — `RegistroVacinacao`

| Campo | Tipo | Descrição | Exemplo |
|-------|------|-----------|---------|
| `id` | Long | Identificador único (auto) | `1` |
| `municipio` | String | Nome do município | `"Sao Paulo"` |
| `estado` | String (2) | Sigla UF | `"SP"` |
| `vacina` | String | Tipo da vacina | `"BCG"`, `"Gripe"` |
| `dose` | String | Número/tipo da dose | `"1a Dose"`, `"Reforco"` |
| `quantidadeAplicada` | Integer | Doses aplicadas | `15234` |
| `dataRegistro` | LocalDateTime | Data do registro | `"2024-01-15T00:00:00"` |

---

## 🔌 Endpoints da API

| Método | Rota | Descrição | Status de retorno |
|--------|------|-----------|-------------------|
| GET | `/vacinacao` | Lista todos os registros | 200 |
| GET | `/vacinacao?vacina=BCG` | Filtra por vacina | 200 |
| GET | `/vacinacao?estado=SP` | Filtra por estado | 200 |
| GET | `/vacinacao?vacina=BCG&estado=SP` | Filtra por ambos | 200 |
| GET | `/vacinacao/{id}` | Busca por ID | 200 / 404 |
| POST | `/vacinacao` | Cria novo registro | 201 / 409 / 400 |
| GET | `/healthz` | Health check | 200 |

### Exemplo de resposta — GET `/vacinacao/1`
```json
{
  "id": 1,
  "municipio": "Sao Paulo",
  "estado": "SP",
  "vacina": "BCG",
  "dose": "1a Dose",
  "quantidadeAplicada": 15234,
  "dataRegistro": "2024-01-15T00:00:00"
}
```

### Exemplo de resposta — erro 404
```json
{
  "message": "Recurso não encontrado",
  "status": "404 NOT_FOUND",
  "error": "Registro com ID 999 não encontrado",
  "timestamp": 1715000000000
}
```

### Exemplo de body — POST `/vacinacao`
```json
{
  "municipio": "Curitiba",
  "estado": "PR",
  "vacina": "Gripe",
  "dose": "1a Dose",
  "quantidadeAplicada": 5000,
  "dataRegistro": "2024-06-01T00:00:00"
}
```

---

## 🚀 Como executar localmente

### Pré-requisitos
- Java 21+
- Maven (ou use o wrapper `./mvnw` incluído)

### Build e execução
```bash
# Clonar o repositório
git clone <url-do-repositorio>
cd imunidata

# Compilar
./mvnw clean package

# Executar (porta padrão 8080)
./mvnw spring-boot:run

# Executar em porta específica (simula ambiente PaaS como Render)
PORT=10000 ./mvnw spring-boot:run
```

### Testar os endpoints
```bash
# Listar todos os registros
curl http://localhost:8080/vacinacao

# Filtrar por vacina
curl "http://localhost:8080/vacinacao?vacina=BCG"

# Filtrar por estado
curl "http://localhost:8080/vacinacao?estado=SP"

# Buscar por ID
curl http://localhost:8080/vacinacao/1

# Criar novo registro
curl -X POST http://localhost:8080/vacinacao \
  -H "Content-Type: application/json" \
  -d '{"municipio":"Curitiba","estado":"PR","vacina":"Gripe","dose":"1a Dose","quantidadeAplicada":5000,"dataRegistro":"2024-06-01T00:00:00"}'

# Health check
curl http://localhost:8080/healthz
```

---

## 📊 Swagger UI (documentação interativa)

Com a aplicação rodando, acesse:

| Interface | URL |
|-----------|-----|
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/v1/api-docs |

---

## 🗄️ H2 Console (visualizar banco de dados)

> ⚠️ Disponível apenas enquanto a JVM estiver rodando (banco em memória).

1. Acesse: http://localhost:8080/h2-console
2. Preencha exatamente assim:
   - **Driver Class:** `org.h2.Driver`
   - **JDBC URL:** `jdbc:h2:mem:vacinacaodb;DB_CLOSE_DELAY=-1`
   - **User Name:** `sa`
   - **Password:** *(em branco)*
3. Clique em **Connect**

Queries úteis:
```sql
-- Ver todos os registros
SELECT * FROM registro_vacinacao;

-- Contar total
SELECT COUNT(*) FROM registro_vacinacao;

-- Filtrar por vacina
SELECT * FROM registro_vacinacao WHERE vacina = 'BCG';

-- Filtrar por estado
SELECT * FROM registro_vacinacao WHERE estado = 'SP';
```

---

## ⚙️ Variáveis de ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `PORT` | `8080` | Porta em que a aplicação sobe (definida automaticamente em PaaS como Render) |

---

## 🌐 Deploy (Render / PaaS)

A aplicação está preparada para deploy em plataformas PaaS gratuitas (Render, Railway, etc.):

- A porta é lida automaticamente via `server.port=${PORT:8080}`
- Um agendador interno (`HealthCheckScheduler`) chama `/healthz` a cada 60 segundos para evitar hibernação
- Configure o health check da plataforma apontando para `/healthz`

---

## 📋 Códigos HTTP retornados

| Código | Significado | Quando ocorre |
|--------|-------------|---------------|
| `200 OK` | Sucesso | GETs com resultado |
| `201 Created` | Criado | POST com sucesso |
| `400 Bad Request` | Requisição inválida | JSON malformado, campos inválidos |
| `404 Not Found` | Não encontrado | ID inexistente |
| `409 Conflict` | Conflito/Duplicado | Mesmo município+estado+vacina+dose já cadastrado |
| `500 Internal Server Error` | Erro interno | Erro inesperado no servidor |
| `503 Service Unavailable` | Serviço indisponível | Falha em serviço externo |

---

## 📁 Documentação adicional

| Documento | Descrição |
|-----------|-----------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Decisões arquiteturais detalhadas e justificativas |
| [API_REFERENCE.md](docs/API_REFERENCE.md) | Referência completa dos endpoints |
| [ERROR_HANDLING.md](docs/ERROR_HANDLING.md) | Formato de erros e exceções customizadas |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | Instruções de deploy local e em produção |

---

## 🛠️ Tecnologias utilizadas

| Tecnologia | Versão | Motivo da escolha |
|------------|--------|-------------------|
| Java | 21 | LTS estável, suporte a records e pattern matching |
| Spring Boot | 3.3.5 | Framework padrão para APIs REST em Java |
| Spring Data JPA | — | Abstração de repositório sem SQL manual |
| H2 Database | — | Banco em memória, zero configuração, ideal para protótipo |
| OpenCSV | 5.9 | Leitura robusta de CSV com suporte a encoding UTF-8 |
| Springdoc OpenAPI | — | Gera Swagger UI automaticamente a partir das anotações |
| Maven | — | Gerenciamento de dependências e build |
