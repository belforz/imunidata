
# Imunidata — Sistema de monitoramento de vacinação

Projeto simples de backend em Java (Spring Boot) para monitoramento de registros de vacinação.

Autores
- Yasmin Feliciano Serafim
- Leandro Costa Belfor

Visão geral
- Backend REST que expõe CRUD para a entidade RegistroVacinacao.
- Dados iniciais carregados a partir de `src/main/resources/data/vacinacao.csv` no startup.
- Banco em memória H2 (configurado por padrão) para desenvolvimento e testes rápidos.

Principais tecnologias
- Java 25, Spring Boot 3.x
- Spring Data JPA (H2)
- OpenCSV (leitura do CSV)
- Springdoc / OpenAPI (Swagger)

Executando localmente
1. Build e run com o wrapper Maven (Linux/macOS):

```bash
./mvnw clean package
./mvnw spring-boot:run
```

2. A aplicação usa a variável de ambiente `PORT` quando fornecida (útil em PaaS). Exemplo:

```bash
# roda na porta 10000
PORT=10000 ./mvnw spring-boot:run
```

Endpoints úteis
- GET /vacinacao — lista todos os registros (aceita filtros `?vacina=` e `?estado=`)
- GET /vacinacao/{id} — busca por id
- POST /vacinacao — cria novo registro (201)
- GET /healthz — health check (200 OK)

Documentação (Swagger / OpenAPI)
- JSON OpenAPI: `/api/v1/api-docs`
- Swagger UI: `/api/v1/swagger-ui.html`

H2 Console
- URL: `http://localhost:{PORT}/h2-console`
- JDBC URL (usar este): `jdbc:h2:mem:vacinacaodb;DB_CLOSE_DELAY=-1`
- User: `sa` (senha em branco)
- Observação: banco em memória existe apenas enquanto a JVM estiver rodando.

Observações de design
- Os dados são carregados automaticamente do CSV em `src/main/resources/data/vacinacao.csv` no método `@PostConstruct` do service.
- Tratamento de erros centralizado com `@ControllerAdvice` (retorna JSON padronizado `ErrorResponse`).
- Verificação de duplicidade feita com um `existsBy...` no repositório para evitar carregar entidades desnecessárias.
- Request logging e um agendador interno batendo em `/healthz` foram adicionados para facilitar observabilidade e para manter o app acordado em PaaS que hibernam.


