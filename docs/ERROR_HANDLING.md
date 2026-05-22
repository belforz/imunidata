# Tratamento de Erros e Formato de Resposta

Este documento descreve como a API Imunidata trata erros e qual o formato padrão de resposta de erro.

## Formato padrão — ErrorResponse

A API usa um DTO padrão `ErrorResponse` com os seguintes campos:

- `message` (String) — mensagem legível sobre o erro.
- `status` (String) — status HTTP textual (ex.: `404 NOT_FOUND`).
- `error` (String) — breve descrição/razão (ex.: `Not Found`).
- `timestamp` (long) — epoch millis quando o erro aconteceu.

Exemplo:

```json
{
  "message": "Registro com ID 999 não encontrado",
  "status": "404 NOT_FOUND",
  "error": "Not Found",
  "timestamp": 1670000000000
}
```

## Exceções customizadas

As exceções usadas pela aplicação são implementadas como classes internas em `ErrorResponse` (por simplicidade do projeto):

- `ResourceNotFoundException` → mapeado para 404 NOT FOUND
- `ResourceAlreadyExistsException` → mapeado para 409 CONFLICT
- `ServiceUnavailableException` → mapeado para 503 SERVICE UNAVAILABLE
- `GenericServiceException` → mapeado para 500 INTERNAL SERVER ERROR

## `@ControllerAdvice` global

- Existe um `@ControllerAdvice` que contém `@ExceptionHandler` para as exceções acima. Ele monta um `ErrorResponse` e retorna um `ResponseEntity` com o status adequado.
- Recomenda-se lançar exceções específicas (ex.: `ResourceNotFoundException`) a partir da camada de serviço quando a regra de negócio indicar falha; o handler global cuida da conversão para HTTP.

## Validação e JSON inválido

- Quando usar `@Valid` em DTOs (não presente em todos os endpoints ainda), `MethodArgumentNotValidException` deve ser capturada pelo handler e convertida em `400 Bad Request` com detalhes por campo.
- `HttpMessageNotReadableException` (JSON malformado) também deve mapear para `400 Bad Request` com mensagem apropriada.

## Recomendações

- Em produção, evite expor stacktraces no corpo da resposta. Logue o stacktrace no servidor e devolva ao cliente apenas a mensagem amigável e o `timestamp`.
- Para garantir integridade contra duplicatas concorrentes, combine verificação via `existsBy...` com constraint de unicidade no schema (índice único) e trate `DataIntegrityViolationException` no handler global (mapear para 409 Conflict).

