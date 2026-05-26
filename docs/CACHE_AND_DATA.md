# Cache In-Memory, Paginação e Dicionário de Dados

## Sumário
- [Por que Cache In-Memory?](#por-que-cache-in-memory)
- [Estruturas de Dados Utilizadas](#estruturas-de-dados-utilizadas)
- [ConcurrentHashMap — Busca por ID (O(1))](#concurrenthashmap--busca-por-id-o1)
- [ConcurrentHashMap.newKeySet() — Checagem de Duplicatas (O(1))](#concurrenthashmapnewkeyset--checagem-de-duplicatas-o1)
- [ArrayList — Filtros e Paginação](#arraylist--filtros-e-paginação)
- [Fluxo do Cache](#fluxo-do-cache)
- [Paginação In-Memory](#paginação-in-memory)
- [Dicionário de Dados](#dicionário-de-dados)
- [Formato do CSV OpenDataSUS](#formato-do-csv-opendatasus)

---

## Por que Cache In-Memory?

O H2 é um banco em memória, mas cada consulta ainda envolve:
- Serialização/deserialização de objetos JPA
- Overhead do Hibernate (geração de SQL, mapeamento de ResultSet)
- Lock/transação do banco

Para um dataset carregado uma única vez via CSV (dados do OpenDataSUS), o padrão mais eficiente é:

| Abordagem | Consulta simples | Filtro com N campos | Busca por ID |
|-----------|-----------------|---------------------|--------------|
| Query JPA ao banco | ~5–50ms (H2) | N queries dinâmicas ou JPQL complexo | ~2–10ms |
| **Cache in-memory** | **< 1ms** | **Stream Java (sem I/O)** | **O(1) via HashMap** |

> A escolha do cache é justificada pelo perfil de uso: **leitura intensiva, escrita esporádica** (startup + CRUD manual).

---

## Estruturas de Dados Utilizadas

O service mantém **três estruturas paralelas em memória**:

```java
// Lista principal — usada para filtros e paginação (sincronizada para thread-safety)
private final List<RegistroVacinacao> cache = Collections.synchronizedList(new ArrayList<>());

// Mapa indexado por ID — busca O(1)
private final Map<Long, RegistroVacinacao> cacheById = new ConcurrentHashMap<>();

// Set de co_documentos — checagem de duplicata em O(1), substitui o antigo stream().anyMatch()
private final Set<String> coDocumentosCache = ConcurrentHashMap.newKeySet();
```

As três são preenchidas no startup (via `@PostConstruct`) e mantidas sincronizadas em cada operação de escrita (salvar, atualizar, deletar).

---

## ConcurrentHashMap — Busca por ID (O(1))

### O que é

`ConcurrentHashMap<K, V>` é uma implementação de `Map` do Java que:
- Garante **thread-safety** sem bloquear toda a estrutura (usa locks por segmento/bucket)
- Realiza **leitura em O(1)** — acesso direto ao valor pelo hash da chave
- Permite leituras simultâneas sem bloquear escritas (ao contrário de `Hashtable` ou `Collections.synchronizedMap`)

### Por que não HashMap simples

| Característica | `HashMap` | `ConcurrentHashMap` |
|----------------|-----------|---------------------|
| Thread-safe | ❌ Não | ✅ Sim |
| Performance em leitura | O(1) | O(1) |
| Performance em escrita | O(1) | O(1) amortizado |
| Leituras simultâneas | ⚠️ Race condition | ✅ Seguro |

Em um ambiente web (Spring Boot), múltiplos threads podem atender requisições ao mesmo tempo. Usar `HashMap` sem sincronização poderia causar **ConcurrentModificationException** ou leituras inconsistentes.

### Como é usado no projeto

```java
// Startup — popula o mapa
cacheById.put(saved.getId(), saved);   // chave = id (Long), valor = objeto

// GET /api/v1/vacinacao/{id} — busca direta, sem varrer a lista
Optional.ofNullable(cacheById.get(id));  // O(1)

// DELETE — remove do mapa e da lista
RegistroVacinacao existente = cacheById.remove(id);

// PUT — atualiza o objeto já referenciado (o mapa aponta para o mesmo objeto da lista)
existente.setVacina(dados.getVacina());
repository.save(existente);  // persiste no H2
```

> **Importante:** como o mapa armazena **referências** ao mesmo objeto que está na lista, atualizar o objeto via `cacheById.get(id)` automaticamente reflete na lista — sem precisar sincronizá-los manualmente.

---

## ConcurrentHashMap.newKeySet() — Checagem de Duplicatas (O(1))

### O que é

`ConcurrentHashMap.newKeySet()` retorna um `Set<String>` thread-safe respaldado por um `ConcurrentHashMap`. É usado para armazenar os valores de `coDocumento` já cadastrados.

### Por que foi necessário

A implementação anterior verificava duplicatas assim:

```java
// ❌ Antigo — O(n), itera toda a lista, suscetível a ConcurrentModificationException
boolean existe = cache.stream().anyMatch(r -> reg.getCoDocumento().equals(r.getCoDocumento()));
```

Isso causava dois problemas:
1. **Performance O(n):** para 50.000 registros, cada inserção varreria toda a lista
2. **`ConcurrentModificationException`:** ao adicionar ao `cache` (`cache.add()`) enquanto outro trecho ainda iterava a lista via stream

A solução foi manter um `Set` dedicado:

```java
// ✅ Novo — O(1), thread-safe, sem iterar a lista
if (coDocumentosCache.contains(reg.getCoDocumento())) { continue; }
// ...
coDocumentosCache.add(saved.getCoDocumento());
```

### Ciclo de vida

| Operação | Ação no Set |
|----------|-------------|
| Startup / upload CSV | `coDocumentosCache.add(coDocumento)` para cada registro inserido |
| `POST /vacinacao` (criar) | Verifica com `.contains()` antes de salvar; `.add()` após salvar |
| `DELETE /vacinacao/{id}` | `coDocumentosCache.remove(coDocumento)` |

---

## ArrayList — Filtros e Paginação

### O que é

`ArrayList<T>` é uma lista dinâmica baseada em array:
- **Acesso por índice em O(1)**
- **Iteração sequencial eficiente** (melhor localidade de cache de CPU vs LinkedList)
- **Não thread-safe por padrão** — por isso é envolvido com `Collections.synchronizedList()`, o que garante que operações de adição/remoção concorrentes (ex.: upload de CSV ao mesmo tempo que uma leitura) não causem `ConcurrentModificationException`

### Por que não LinkedList

| Operação | `ArrayList` | `LinkedList` |
|----------|-------------|--------------|
| Iteração sequencial (filtro) | ✅ O(n) rápido | O(n) mais lento por ponteiros |
| Acesso por índice (paginação) | ✅ O(1) | O(n) |
| Inserção no final | O(1) amortizado | O(1) |
| Remoção no meio | O(n) | O(1) se posição conhecida |

Para o perfil de uso (iterar para filtrar, acessar sub-range para paginação), `ArrayList` é a escolha ideal.

### Como é usado no projeto

```java
// Filtro com streams — itera a lista aplicando predicados
cache.stream()
    .filter(r -> vacina == null || r.getVacina().equalsIgnoreCase(vacina))
    .filter(r -> estado == null || r.getEstado().equalsIgnoreCase(estado))
    // ... outros filtros
    .collect(Collectors.toList());

// Paginação — subList usa acesso por índice (O(1) no ArrayList)
resultado.subList(fromIndex, toIndex);
```

---

## Fluxo do Cache

```
Startup (@PostConstruct)
        │
        ▼
  Lê vacinacao.csv
        │
        ▼
  Para cada linha válida:
    ┌──────────────────────────────────────────────┐
    │  repository.save(reg)  ──►  H2 (banco)       │
    │         │                                    │
    │         └──► salvo (com ID gerado)            │
    │                  │                           │
    │         cache.add(salvo)                     │  ◄── ArrayList
    │         cacheById.put(salvo.getId(), salvo)  │  ◄── ConcurrentHashMap
    └──────────────────────────────────────────────┘

GET /vacinacao?estado=SP
        │
        ▼
  cache.stream().filter(...) ──► subList(page, limit) ──► ResponseEntity

GET /vacinacao/{id}
        │
        ▼
  cacheById.get(id) ──► O(1) ──► ResponseEntity

POST /vacinacao
        │
        ▼
  Verifica duplicata em coDocumentosCache (O(1), sem iterar o cache)
        │
        ▼
  repository.save() ──► H2
        │
        ▼
  cache.add() + cacheById.put() + coDocumentosCache.add()

DELETE /vacinacao/{id}
        │
        ▼
  cacheById.remove(id)
  cache.remove(objeto)
  coDocumentosCache.remove(coDocumento)
  repository.deleteById(id)
```

---

## Paginação In-Memory

A paginação é feita **após o filtro**, diretamente na lista de resultados em memória. Não usa `Pageable` do Spring Data (que geraria uma query ao banco) — isso é intencional para evitar I/O desnecessário.

### Parâmetros

| Parâmetro | Tipo | Padrão | Máximo | Descrição |
|-----------|------|--------|--------|-----------|
| `page` | Integer | `0` | — | Número da página (começa em 0) |
| `limit` | Integer | `20` | `100` | Registros por página |

### Como funciona

```
Resultado filtrado (ex: 51 registros)
        │
        ▼
page=1, limit=10
        │
        ▼
pageSize = clamp(limit, 1, 100)  →  10
fromIndex = page * pageSize       →  10
toIndex = fromIndex + pageSize    →  20
        │
        ▼
resultado.subList(10, 20)  →  registros 11 a 20
```

### Exemplos

```bash
# Primeira página, 20 por página (padrão)
GET /api/v1/vacinacao

# Primeira página, 5 por página
GET /api/v1/vacinacao?page=0&limit=5

# Segunda página, 5 por página (registros 6-10)
GET /api/v1/vacinacao?page=1&limit=5

# Filtro + paginação
GET /api/v1/vacinacao?estado=SP&page=0&limit=10
```

### Comportamento de borda

| Situação | Comportamento |
|----------|--------------|
| `page` além do total | Retorna lista vazia → lança `ResourceNotFoundException` (404) |
| `limit` > 100 | Clampado para `100` automaticamente |
| `limit` < 1 | Clampado para `1` automaticamente |
| Nenhum resultado após filtro | `ResourceNotFoundException` (404) antes da paginação |

---

## Dicionário de Dados

A entidade `RegistroVacinacao` representa um registro de vacinação do OpenDataSUS. Abaixo o dicionário completo de campos:

### `id`
- **Tipo:** `Long`
- **Coluna no banco:** `id` (PK, auto-increment)
- **Coluna CSV:** — (gerado automaticamente)
- **Obrigatório:** Sim (gerado)
- **Descrição:** Identificador sequencial interno, gerado pelo banco na persistência. Usado para busca direta via `GET /api/v1/vacinacao/{id}`. Não é o UUID do documento original.
- **Exemplo:** `1`, `2`, `999`

---

### `coDocumento`
- **Tipo:** `String`
- **Coluna no banco:** `co_documento`
- **Coluna CSV:** `co_documento`
- **Obrigatório:** Não (mas usado para verificação de duplicatas)
- **Descrição:** Identificador único do documento de vacinação no sistema de origem (RNDS/RNVS). Formato UUID com sufixo de item. Usado para evitar inserções duplicadas.
- **Exemplo:** `"c8a9789c-a4d7-4851-a698-bd39895f922c-i0b0"`

---

### `coPaciente`
- **Tipo:** `String`
- **Coluna no banco:** `co_paciente`
- **Coluna CSV:** `co_paciente`
- **Obrigatório:** Não
- **Descrição:** Hash SHA-256 (ou similar) anonimizado do CPF/CNS do paciente. Nunca contém dados pessoais identificáveis diretamente — apenas um hash para correlacionar múltiplos registros do mesmo paciente sem expor identidade.
- **Exemplo:** `"3ebb002b287e37b4e3713b9cc3bac86438e3ad0b..."`

---

### `sexo`
- **Tipo:** `String`
- **Coluna no banco:** `tp_sexo_paciente`
- **Coluna CSV:** `tp_sexo_paciente`
- **Obrigatório:** Não
- **Valores possíveis:** `"M"` (masculino), `"F"` (feminino), `"I"` (ignorado)
- **Descrição:** Sexo biológico do paciente conforme cadastro no sistema de saúde.
- **Exemplo:** `"M"`

---

### `racaCor`
- **Tipo:** `String`
- **Coluna no banco:** `no_raca_cor_paciente`
- **Coluna CSV:** `no_raca_cor_paciente`
- **Obrigatório:** Não
- **Valores possíveis:** `"BRANCA"`, `"PRETA"`, `"PARDA"`, `"AMARELA"`, `"INDIGENA"`, `"SEM INFORMACAO"`
- **Descrição:** Raça/cor autodeclarada do paciente, conforme classificação do IBGE utilizada pelo SUS.
- **Exemplo:** `"BRANCA"`

---

### `municipio`
- **Tipo:** `String`
- **Coluna no banco:** `no_municipio_paciente`
- **Coluna CSV:** `no_municipio_paciente`
- **Obrigatório:** Não
- **Descrição:** Nome do município de residência do paciente (em maiúsculas, sem acentos em alguns datasets).
- **Exemplo:** `"SAO PAULO"`, `"BELO HORIZONTE"`

---

### `estado`
- **Tipo:** `String` (2 caracteres)
- **Coluna no banco:** `sg_uf_paciente`
- **Coluna CSV:** `sg_uf_paciente`
- **Obrigatório:** Não
- **Descrição:** Sigla da Unidade Federativa (UF) de residência do paciente.
- **Exemplo:** `"SP"`, `"RJ"`, `"MG"`

---

### `idade`
- **Tipo:** `Integer`
- **Coluna no banco:** `nu_idade_paciente`
- **Coluna CSV:** `nu_idade_paciente`
- **Obrigatório:** Não (pode ser nulo se não informado)
- **Descrição:** Idade do paciente em anos completos na data da vacinação.
- **Exemplo:** `13`, `65`, `0` (bebê)

---

### `estabelecimento`
- **Tipo:** `String`
- **Coluna no banco:** `no_fantasia_estabelecimento`
- **Coluna CSV:** `no_fantasia_estabelecimento`
- **Obrigatório:** Não
- **Descrição:** Nome fantasia da unidade de saúde onde a vacina foi aplicada (UBS, hospital, clínica, etc.).
- **Exemplo:** `"UBS VILA TEREZINHA"`, `"HOSPITAL DAS CLINICAS"`

---

### `vacina`
- **Tipo:** `String`
- **Coluna no banco:** `ds_vacina`
- **Coluna CSV:** `ds_vacina`
- **Obrigatório:** Não
- **Descrição:** Nome completo da vacina conforme tabela de imunobiológicos do PNI (Programa Nacional de Imunizações).
- **Exemplos:** `"Vacina dengue (atenuada)"`, `"BCG"`, `"Covid-19-Coronavac-Sinovac/Butantan"`, `"Influenza trivalente"`

---

### `dataVacina`
- **Tipo:** `LocalDate`
- **Coluna no banco:** `dt_vacina`
- **Coluna CSV:** `dt_vacina`
- **Formato:** `yyyy-MM-dd`
- **Obrigatório:** Não
- **Descrição:** Data em que a vacina foi aplicada.
- **Exemplo:** `"2026-01-01"`

---

### `dose`
- **Tipo:** `String`
- **Coluna no banco:** `ds_dose_vacina`
- **Coluna CSV:** `ds_dose_vacina`
- **Obrigatório:** Não
- **Valores comuns:** `"1ª Dose"`, `"2ª Dose"`, `"3ª Dose"`, `"Dose Única"`, `"Reforço"`, `"2º Reforço"`
- **Descrição:** Identificação da dose aplicada dentro do esquema vacinal.
- **Exemplo:** `"1ª Dose"`

---

### `localAplicacao`
- **Tipo:** `String`
- **Coluna no banco:** `ds_local_aplicacao`
- **Coluna CSV:** `ds_local_aplicacao`
- **Obrigatório:** Não
- **Descrição:** Região anatômica onde a vacina foi aplicada.
- **Exemplos:** `"Deltoides do Braco Esquerdo"`, `"Face Externa Inferior do Braco Esquerdo"`, `"Coxa Esquerda"`

---

### `viaAdministracao`
- **Tipo:** `String`
- **Coluna no banco:** `ds_via_administracao`
- **Coluna CSV:** `ds_via_administracao`
- **Obrigatório:** Não
- **Valores comuns:** `"Intramuscular"`, `"Subcutanea"`, `"Intradermica"`, `"Oral"`
- **Descrição:** Via pela qual a vacina foi administrada.
- **Exemplo:** `"Intramuscular"`

---

### `loteVacina`
- **Tipo:** `String`
- **Coluna no banco:** `co_lote_vacina`
- **Coluna CSV:** `co_lote_vacina`
- **Obrigatório:** Não
- **Descrição:** Código do lote do imunobiológico utilizado. Importante para rastreabilidade e vigilância de eventos adversos.
- **Exemplo:** `"561413"`, `"2024A01"`

---

### `fabricante`
- **Tipo:** `String`
- **Coluna no banco:** `ds_vacina_fabricante`
- **Coluna CSV:** `ds_vacina_fabricante`
- **Obrigatório:** Não
- **Descrição:** Nome do laboratório fabricante da vacina.
- **Exemplos:** `"IDT BIOLOGIKA GMBH"`, `"FIOCRUZ"`, `"BUTANTAN"`, `"PFIZER"`

---

### `estrategia`
- **Tipo:** `String`
- **Coluna no banco:** `ds_estrategia_vacinacao`
- **Coluna CSV:** `ds_estrategia_vacinacao`
- **Obrigatório:** Não
- **Valores comuns:** `"Rotina"`, `"Campanha"`, `"Especial"`, `"Bloqueio"`
- **Descrição:** Estratégia de vacinação sob a qual a dose foi aplicada. Rotina = calendário regular; Campanha = ação massiva específica.
- **Exemplo:** `"Rotina"`

---

### `origemRegistro`
- **Tipo:** `String`
- **Coluna no banco:** `ds_origem_registro`
- **Coluna CSV:** `ds_origem_registro`
- **Obrigatório:** Não
- **Descrição:** Como o registro foi inserido no sistema. Pode indicar digitação manual, integração de sistema, transcrição de caderneta física, etc.
- **Exemplos:** `"Registro anterior/Transcricao de caderneta"`, `"Sistema de informacao"`, `"RNDS"`

---

## Formato do CSV OpenDataSUS

O CSV esperado pelo sistema deve ter **17 colunas** na seguinte ordem, com cabeçalho na primeira linha:

```
co_documento, co_paciente, tp_sexo_paciente, no_raca_cor_paciente,
no_municipio_paciente, sg_uf_paciente, nu_idade_paciente,
no_fantasia_estabelecimento, ds_vacina, dt_vacina, ds_dose_vacina,
ds_local_aplicacao, ds_via_administracao, co_lote_vacina,
ds_vacina_fabricante, ds_estrategia_vacinacao, ds_origem_registro
```

### Regras de parsing

| Regra | Detalhe |
|-------|---------|
| Separador | `;` (ponto e vírgula) — padrão dos arquivos exportados via pandas/OpenDataSUS |
| Encoding | Tenta UTF-8 primeiro; fallback para ISO-8859-1 / Latin-1 (comum em arquivos do SUS) |
| Linhas com < 17 colunas | Ignoradas silenciosamente |
| `nu_idade_paciente` inválido | Campo fica `null` (não aborta a linha) |
| `dt_vacina` | Aceita múltiplos formatos: `yyyy-MM-dd`, `dd/MM/yyyy`, `d/M/yyyy`, `yyyy/MM/dd` |
| Duplicata por `co_documento` | Verificada em O(1) via `coDocumentosCache` (sem iterar a lista) — linha ignorada |
| Espaços extras | Removidos com `trim()` em todos os campos |
| Linhas com erro de parse | Ignoradas; log exibe tipo de exceção, número de colunas e conteúdo da linha |

### Exemplo de linha válida

```
c8a9789c-a4d7-4851-a698-bd39895f922c-i0b0;3ebb002b287e37b4e3713b9cc3bac86438e3ad0b;M;BRANCA;SAO PAULO;SP;13;UBS VILA TEREZINHA;Vacina dengue (atenuada);2026-01-01;1ª Dose;Face Externa Inferior do Braço Esquerdo;Subcutânea;561413;IDT BIOLOGIKA GMBH;Rotina;Registro anterior/Transcrição de caderneta
```

> ⚠️ **Nota:** o separador é `;`, não `,`. Arquivos gerados pelo pandas com `sep=';'` e `encoding='latin-1'` são compatíveis diretamente.

