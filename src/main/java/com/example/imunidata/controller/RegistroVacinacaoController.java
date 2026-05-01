package com.example.imunidata.controller;

import com.example.imunidata.model.RegistroVacinacao;
import com.example.imunidata.service.RegistroVacinacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vacinacao")
@Tag(name = "Vacinação", description = "API para gerenciamento de registros de vacinação")
public class RegistroVacinacaoController {

    private final RegistroVacinacaoService service;

    public RegistroVacinacaoController(RegistroVacinacaoService service) {
        this.service = service;
    }

    // GET /vacinacao  |  GET /vacinacao?vacina=BCG  |  GET /vacinacao?estado=SP
    @GetMapping
    @Operation(summary = "Listar registros de vacinação", description = "Retorna todos os registros ou filtrados por vacina e/ou estado")
    public ResponseEntity<List<RegistroVacinacao>> listar(
            @Parameter(description = "Filtrar por tipo de vacina (ex: BCG, Gripe)")
            @RequestParam(required = false) String vacina,
            @Parameter(description = "Filtrar por estado (ex: SP, RJ)")
            @RequestParam(required = false) String estado) {
        try {
            List<RegistroVacinacao> resultado = service.filtrar(vacina, estado);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /vacinacao/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Buscar registro por ID", description = "Retorna um registro específico pelo ID")
    public ResponseEntity<RegistroVacinacao> buscarPorId(
            @Parameter(description = "ID do registro de vacinação")
            @PathVariable Long id) {
        try {
            return service.buscarPorId(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // POST /vacinacao
    @PostMapping
    @Operation(summary = "Criar novo registro", description = "Cria um novo registro de vacinação")
    public ResponseEntity<RegistroVacinacao> criar(
            @Parameter(description = "Dados do registro de vacinação")
            @RequestBody RegistroVacinacao registro) {
        try {
            RegistroVacinacao salvo = service.salvar(registro);
            return ResponseEntity.status(201).body(salvo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
