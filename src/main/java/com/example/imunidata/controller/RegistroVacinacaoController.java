package com.example.imunidata.controller;

import com.example.imunidata.model.RegistroVacinacao;
import com.example.imunidata.service.RegistroVacinacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vacinacao")
public class RegistroVacinacaoController {

    private final RegistroVacinacaoService service;

    public RegistroVacinacaoController(RegistroVacinacaoService service) {
        this.service = service;
    }

    // GET em /vacinacao com filtros opcionais
    @GetMapping
    public ResponseEntity<List<RegistroVacinacao>> listar(
            @RequestParam(required = false) String vacina,
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
    public ResponseEntity<RegistroVacinacao> buscarPorId(@PathVariable Long id) {
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
    public ResponseEntity<RegistroVacinacao> criar(@RequestBody RegistroVacinacao registro) {
        try {
            RegistroVacinacao salvo = service.salvar(registro);
            return ResponseEntity.status(201).body(salvo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

