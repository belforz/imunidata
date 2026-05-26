# Arquitetura da API Imunidata

Este documento descreve a arquitetura do backend Imunidata (Java + Spring Boot) e as decisões de projeto principais.

## Visão geral

- Aplicação REST simples para monitoramento de vacinação. Dados iniciais são carregados de um CSV localizado em `src/main/resources/data/vacinacao.csv`.
- Banco: H2 (in-memory por padrão). Dados são carregados no startup pelo serviço `RegistroVacinacaoService`.
- OpenAPI/Swagger está habilitado para documentação (paths em `application.properties`).

## Pacotes e camadas

Organização principal (pacote base `com.example.imunidata`):

- `model` — entidades JPA e DTOs (`RegistroVacinacao`, `ErrorResponse`).
- `repository` — interfaces `JpaRepository` para acesso a dados.
- `service` — lógica de negócio, validações e carregamento do CSV. Uso de `@PostConstruct` para popular o banco no startup.
- `controller` — endpoints REST que expõem a API (ex.: `RegistroVacinacaoController`, `HealthController`).
- `utils` / `advice` — `@ControllerAdvice` central para tratamento de exceções e mapeamento para respostas HTTP.
- `config` — configuração (CORS, scheduler de health check, OpenAPI etc.).

Essa separação segue o padrão clássico de 4 camadas: Controller → Service → Repository → Model.

## Fluxo de dados (exemplo de criação/consulta)

1. Cliente chama o endpoint REST (Controller).
2. Controller delega ao Service.
3. Service aplica regras de negócio (validações, normalizações) e usa o Repository para persistir ou consultar.
4. Repository (Spring Data JPA) executa a query no H2.
5. Em caso de erro o Service lança exceção customizada; o `@ControllerAdvice` converte para `ErrorResponse` e `ResponseEntity` apropriado.

## Carregamento inicial (CSV)

- O arquivo `vacinacao.csv` fica em `src/main/resources/data`.
- `RegistroVacinacaoService.carregarDadosCSV()` usa OpenCSV para ler o arquivo no `@PostConstruct` e salva os registros no banco.
- O parser é configurado com separador `;` (padrão dos arquivos OpenDataSUS exportados via pandas).
- Datas são aceitas em múltiplos formatos: `yyyy-MM-dd`, `dd/MM/yyyy`, `d/M/yyyy`, `yyyy/MM/dd`.
- Encoding: tenta UTF-8 primeiro; fallback automático para ISO-8859-1 (Latin-1), comum em arquivos do DATASUS.

## Health check e Agendador

- Existe um endpoint HTTP `/healthz` (controller) que responde `200 OK`.
- Há um agendador que executa periodicamente (a cada 60s) uma chamada interna para `/healthz` usando a porta definida pela variável `PORT` (ou fallback `8080`) — isso ajuda a evitar hibernação em plataformas PaaS.

## Observabilidade e logs

- `RequestLoggingFilter` registra método, URI, status e tempo de resposta. Nível `INFO` para 2xx, `WARN` para 4xx e `ERROR` para 5xx.
- Scheduler usa SLF4J para logar sucesso/falha dos pings de health.

## Tratamento de erros

- Exceções de negócio são modeladas como classes internas de `ErrorResponse` (ex.: `ResourceNotFoundException`, `ResourceAlreadyExistsException`).
- `@ControllerAdvice` mapeia essas exceções para `ErrorResponse` com campos: `message`, `status`, `error`, `timestamp`.

## OpenAPI / Swagger

- A aplicação está configurada para expor a documentação OpenAPI/Swagger (ver `application.properties` para os paths). Use `/api/v1/api-docs` e `/api/v1/swagger-ui.html` conforme configurado.

## Recomendações / Próximos passos

- Para produção, não use H2 em memória. Migrate para PostgreSQL ou MySQL e configure migrações (Flyway/Liquibase).
- Evitar expor H2 Console em produção. Se precisar de inspeção remota, preferir persistência em arquivo temporária ou endpoints administrativos protegidos.
- Adicionar testes de integração que validem o carregamento do CSV e os mapeamentos de erro.
- Adicionar constraints de unicidade no banco e tratar `DataIntegrityViolationException` para garantir atomicidade contra duplicatas concorrentes.

## Decisões arquiteturais (por que escolhemos estas opções)

- H2 (in-memory) durante desenvolvimento: simples de configurar, elimina dependência externa para provas de conceito e testes locais. Não é recomendado para produção porque não persiste dados entre reinícios e não oferece as garantias de concorrência/backup de um banco relacional em produção.

- OpenCSV para carga inicial: é uma biblioteca leve e madura para parsing de CSV que reduz código boilerplate. Para volumes grandes ou pipelines ETL, migrar para ferramentas/batch jobs especializados (Spring Batch) é recomendado.

- `LocalDateTime` para `dataRegistro`: optamos pelo tipo Java que representa data e hora sem fuso horário, porque os dados de vacinação fornecidos não incluem timezone explícito. Se recebermos timestamps com offsets, mudaríamos para `OffsetDateTime`/`ZonedDateTime`.

- `@ControllerAdvice` (handler global) e exceções customizadas: centraliza o tratamento de erros, mantém controllers limpos e garante formato consistente de `ErrorResponse` para o cliente (frontend). Essa abordagem evita duplicação de try/catch em cada endpoint.

- `Optional` nos retornos do service/repository: deixa explícito quando um resultado pode ser ausente (p.ex. findById) e força o consumidor a tratar esse caso — reduz NullPointerException e melhora legibilidade do fluxo de controle.

- `existsBy...` para checagem de duplicidade em vez de buscar listas inteiras: é mais eficiente (consulta booleana) e reduz uso de memória/overhead ao verificar existência de registros.

- `ConcurrentHashMap.newKeySet()` para checagem de duplicatas por `co_documento`: substitui o antigo `cache.stream().anyMatch(...)` que era O(n) e causava `ConcurrentModificationException` ao iterar a lista enquanto outro registro era adicionado. O `Set` baseado em `ConcurrentHashMap` oferece verificação O(1) e é thread-safe sem locks explícitos.

- `Collections.synchronizedList(new ArrayList<>())` para o cache principal: protege contra modificações concorrentes na lista quando upload de CSV e requisições de leitura ocorrem simultaneamente.

- RequestLoggingFilter: registrar requisições e status em um formato simples (INFO/WARN/ERROR) facilita depuração e monitoramento no ambiente PaaS, onde log streaming geralmente é a principal fonte de observabilidade.

- Scheduler interno batendo em `/healthz`: em planos gratuitos de alguns PaaS a aplicação pode hibernar; um ping interno ajuda a manter a JVM ativa. Em produção com readiness/liveness probes do orquestrador, essa técnica não é recomendada.

- SpringDoc (OpenAPI/Swagger): gera documentação automaticamente a partir das controllers e anotações — facilita integração com frontend e clientes sem necessidade de escrever manualmente um contrato OpenAPI.

## Por que não escolhemos outras opções (resumo)

- Não usar um banco relacional completo (Postgres/MySQL) por simplicidade no escopo do projeto e para facilitar execução local e avaliações rápidas.
- Não usar Spring Batch ou pipelines ETL: o CSV aqui é pequeno e carregado no startup; para cargas regulares e dados grandes, Batch é mais apropriado.
- Evitamos expor endpoints administrativos em produção sem autenticação — quando for necessário expor dados para debugging remoto, preferimos persistir em arquivo ou fornecer endpoints temporários protegidos.

---
Arquivo gerado automaticamente para documentação da arquitetura.

