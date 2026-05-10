# Deployment / Execução

Instruções e observações para executar a aplicação localmente e em PaaS (Render, Heroku, etc.).

## Executar localmente

1. Build e run com Maven (wrapper):

```bash
./mvnw clean package
./mvnw spring-boot:run
```

2. Aponte o navegador para:
- API base: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (use JDBC URL `jdbc:h2:mem:vacinacaodb;DB_CLOSE_DELAY=-1`, user `sa`, password vazio)

3. Ou rodar o JAR gerado:

```bash
PORT=8080 java -jar target/imunidata-0.0.1-SNAPSHOT.jar
```

## Variável de porta e PaaS

- A aplicação está configurada para usar a variável de ambiente `PORT` quando disponível:
  `server.port=${PORT:8080}`
- Plataformas como Render atribuem dinamicamente uma porta — é crucial que a app escute na porta fornecida (`PORT`).

## Health check

- Endpoint: `GET /healthz` — deve retornar `200 OK` e corpo `OK`.
- Existe um agendador que faz pings internos para `/healthz` usando a variável `PORT` — isso ajuda a evitar que PaaS free hibernem a aplicação.

## H2 Console e persistência

- Atualmente o datasource é H2 in-memory (`jdbc:h2:mem:vacinacaodb;DB_CLOSE_DELAY=-1`). Isso significa que os dados existem apenas enquanto o processo está rodando.
- Se você precisa persistir dados entre reinícios (útil para debugging remoto), altere `spring.datasource.url` para um arquivo H2, por exemplo:

```
spring.datasource.url=jdbc:h2:file:./data/vacinacaodb;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE
```

Isso produzirá um arquivo `./data/vacinacaodb.mv.db` no diretório da aplicação.

## Segurança e produção

- Não exponha H2 Console em produção.
- Use um banco relacional adequado (Postgres, MySQL) e configure migrações (Flyway/Liquibase).
- Proteja endpoints administrativos (se adicionar) com autenticação / tokens.

