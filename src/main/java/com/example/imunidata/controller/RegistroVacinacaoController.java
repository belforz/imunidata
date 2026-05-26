package com.example.imunidata.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.imunidata.model.ErrorResponse;
import com.example.imunidata.model.RegistroVacinacao;
import com.example.imunidata.service.RegistroVacinacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/v1/vacinacao")
@Tag(name = "Vacinação", description = "API para gerenciamento de registros de vacinação (OpenDataSUS)")
public class RegistroVacinacaoController {

    private final RegistroVacinacaoService service;

    public RegistroVacinacaoController(RegistroVacinacaoService service) {
        this.service = service;
    }

    // GET api/v1/vacinacao | ?TODOS OS FILTROS PELO CAMPO DO DB
    @GetMapping
    @Operation(summary = "Listar registros", description = "Retorna todos os registros ou filtra por vacina, estado e/ou municipio")
    public ResponseEntity<List<RegistroVacinacao>> listar(
            @Parameter(description = "Filtrar por vacina (ex: Vacina dengue (atenuada))")
            @RequestParam(required = false) String vacina,
            @Parameter(description = "Filtrar por UF (ex: SP)")
            @RequestParam(required = false) String estado,
            @Parameter(description = "Filtrar por município (ex: SAO PAULO)")
            @RequestParam(required = false) String municipio,
            @Parameter(description = "Filtrar por município (ex: SAO PAULO)")
            @RequestParam(required = false) String coDocumento,
            @Parameter(description = "Filtrar por hash anonimizado do paciente (ex: 3ebb002b287e37b4e3713b9cc3bac86438e3ad0b)")
            @RequestParam(required = false) String coPaciente,
            @Parameter(description = "Filtrar por sexo do paciente (ex: M, F, I)")
            @RequestParam(required = false) String sexo,
            @Parameter(description = "Filtrar por raça/cor do paciente (ex: BRANCA)")
            @RequestParam(required = false) String racaCor,
            @Parameter(description = "Filtrar por idade do paciente (ex: 13)")
            @RequestParam(required = false) Integer idade,
            @Parameter(description = "Filtrar por nome do estabelecimento de saúde (ex: UBS VILA TEREZINHA)")
            @RequestParam(required = false) String estabelecimento,
            @Parameter(description = "Filtrar por data da vacinação (ex: 2026-01-01)")
            @RequestParam(required = false) LocalDate dataVacina,
            @Parameter(description = "Filtrar por dose aplicada (ex: 1ª Dose, 2ª Dose, 3ª Dose, Dose Única, Reforço)")
            @RequestParam(required = false) String dose,
            @Parameter(description = "Filtrar por local de aplicação da vacina (ex: Face Externa Inferior do Braço Esquerdo)")
            @RequestParam(required = false) String localAplicacao,
            @Parameter(description = "Filtrar por via de administração (ex: Subcutânea)")
            @RequestParam(required = false) String viaAdministracao,
            @Parameter(description = "Filtrar por lote da vacina (ex: 12345)")
            @RequestParam(required = false) String loteVacina,
            @Parameter(description = "Filtrar por fabricante da vacina (ex: Butantan)")
            @RequestParam(required = false) String fabricante,
            @Parameter(description = "Filtrar por estratégia de vacinação (ex: Campanha, Rotina)")
            @RequestParam(required = false) String estrategia,
            @Parameter(description = "Filtrar por origem do registro (ex: Registro anterior/Transcrição de caderneta)")
            @RequestParam(required = false) String origemRegistro,
            @Parameter(description = "Página (começa em 0, padrão: 0)")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Registros por página (padrão: 20, máximo: 100)")
            @RequestParam(required = false, defaultValue = "20") Integer limit

            ) {

        List<RegistroVacinacao> resultado = service.filtrar(
                coDocumento, coPaciente, sexo, racaCor,
                municipio, estado, idade, estabelecimento,
                vacina, dataVacina, dose, localAplicacao,
                viaAdministracao, loteVacina, fabricante,
                estrategia, origemRegistro);

        if (resultado.isEmpty()) {
            throw new ErrorResponse.ResourceNotFoundException("Nenhum registro encontrado para os filtros informados");
        }

        // Paginação in-memory
        int pageSize = Math.clamp(limit, 1, 100);
        int fromIndex = Math.min(page * pageSize, resultado.size());
        int toIndex = Math.min(fromIndex + pageSize, resultado.size());


        return ResponseEntity.ok(resultado.subList(fromIndex, toIndex));
    }

    // GET api/v1/vacinacao/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Buscar por ID", description = "Busca um registro pelo índice sequencial (O(1) via cache)")
    public ResponseEntity<RegistroVacinacao> buscarPorId(
            @Parameter(description = "ID sequencial do registro") @PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado"));
    }

    // POST api/v1/vacinacao/upload — envia um CSV no formato OpenDataSUS
    @PostMapping("/upload")
    @Operation(summary = "Upload de CSV", description = "Carrega registros de vacinação a partir de um arquivo CSV no formato OpenDataSUS")
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @Parameter(description = "Arquivo CSV no formato OpenDataSUS (co_documento, co_paciente, ...)")
            @RequestParam("file") MultipartFile file) {
        try {
            File temp = File.createTempFile("vacinacao_upload_", ".csv");
            file.transferTo(temp);
            int inseridos = service.carregarDadosCSVArquivo(temp);
            temp.delete();
            return ResponseEntity.ok(Map.of(
                    "mensagem", inseridos > 0 ? "CSV carregado com sucesso" : "CSV processado, mas nenhum registro novo foi inserido (verifique duplicatas ou erros no log)",
                    "registrosInseridos", inseridos
            ));
        } catch (ErrorResponse.ResourceAlreadyExistsException | ErrorResponse.GenericServiceException ex) {
            throw ex; // deixa o @ControllerAdvice tratar
        } catch (Exception e) {
            throw new ErrorResponse.GenericServiceException("Erro ao processar o arquivo: " + e.getMessage());
        }
    }

    // POST api/v1/vacinacao
    @PostMapping
    @Operation(summary = "Criar registro", description = "Cria um novo registro de vacinação")
    public ResponseEntity<RegistroVacinacao> criar(@RequestBody RegistroVacinacao registro) {
        RegistroVacinacao salvo = service.salvar(registro);

        return ResponseEntity.status(201).body(salvo);
    }

    // PUT api/v1/vacinacao/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar registro", description = "Atualiza dados de um registro existente pelo ID")
    public ResponseEntity<RegistroVacinacao> atualizar(
            @Parameter(description = "ID do registro") @PathVariable Long id,
            @RequestBody RegistroVacinacao dados) {
        return ResponseEntity.ok(service.atualizar(id, dados));
    }

    // DELETE api/v1/vacinacao/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar registro", description = "Remove um registro pelo ID")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID do registro") @PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
