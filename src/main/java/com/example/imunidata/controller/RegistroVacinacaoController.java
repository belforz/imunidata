package com.example.imunidata.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.imunidata.model.ErrorResponse;
import com.example.imunidata.model.RegistroVacinacao;
import com.example.imunidata.service.RegistroVacinacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/v1/vacinacao")
@Tag(name = "Vacinação", description = "API para gerenciamento de registros de vacinação")
public class RegistroVacinacaoController {

    private final RegistroVacinacaoService service;

    public RegistroVacinacaoController(RegistroVacinacaoService service) {
        this.service = service;
    }

    // GET api/v1//vacinacao  |  GET api/v1//vacinacao?vacina=BCG  |  GET api/v1/vacinacao?estado=SP
    @GetMapping
    @Operation(summary = "Listar registros de vacinação", description = "Retorna todos os registros ou filtrados por vacina e/ou estado")
    public ResponseEntity<List<RegistroVacinacao>> listar(
            @Parameter(description = "Filtrar por tipo de vacina (ex: BCG, Gripe)")
            @RequestParam(required = false) String vacina,
            @Parameter(description = "Filtrar por estado (ex: SP, RJ)")
            @RequestParam(required = false) String estado) {

        List<RegistroVacinacao> resultado = service.filtrar(vacina, estado);
        if (resultado == null || resultado.isEmpty()) {
            throw new ErrorResponse.ResourceNotFoundException("Não foram encontrados registros para os filtros fornecidos");
        }
        return ResponseEntity.ok(resultado);
    }

    // GET api/v1/vacinacao/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Buscar registro por ID", description = "Retorna um registro específico pelo ID")
    public ResponseEntity<RegistroVacinacao> buscarPorId(
            @Parameter(description = "ID do registro de vacinação")
            @PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado"));
    }

    // POST api/v1/vacinacao
    @PostMapping
    @Operation(summary = "Criar novo registro", description = "Cria um novo registro de vacinação")
    public ResponseEntity<RegistroVacinacao> criar(
            @Parameter(description = "Dados do registro de vacinação")
            @RequestBody RegistroVacinacao registro) {
        RegistroVacinacao salvo = service.salvar(registro);
        return ResponseEntity.status(201).body(salvo);
    }

    // PUT api/v1/vacinacao/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar registro", description = "Atualiza os dados de um registro de vacinação existente pelo ID")
    public ResponseEntity<RegistroVacinacao> atualizar(
            @Parameter(description = "ID do registro a ser atualizado") 
            @PathVariable Long id,
            @Parameter(description = "Dados atualizados do registro de vacinação") 
            @RequestBody RegistroVacinacao dadosAtualizados) {

        return service.buscarPorId(id).map(registroExistente -> {
            registroExistente.setMunicipio(dadosAtualizados.getMunicipio());
            registroExistente.setEstado(dadosAtualizados.getEstado());
            registroExistente.setVacina(dadosAtualizados.getVacina());
            registroExistente.setDose(dadosAtualizados.getDose());
            registroExistente.setQuantidadeAplicada(dadosAtualizados.getQuantidadeAplicada());
            
            RegistroVacinacao atualizado = service.salvar(registroExistente);
            return ResponseEntity.ok(atualizado);
            
        }).orElseThrow(() -> new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado para atualização"));
    }

    // DELETE api/v1/vacinacao/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar registro", description = "Remove um registro de vacinação específico pelo ID")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID do registro a ser deletado") 
            @PathVariable Long id) {

        return service.buscarPorId(id).map(registroExistente -> {
            service.deletar(id);
            return ResponseEntity.noContent().<Void>build();
            
        }).orElseThrow(() -> new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado para deletar"));
    }
}
