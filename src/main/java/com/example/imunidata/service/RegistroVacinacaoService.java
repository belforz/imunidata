package com.example.imunidata.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.imunidata.model.ErrorResponse;
import com.example.imunidata.model.RegistroVacinacao;
import com.example.imunidata.repository.RegistroVacinacaoRepository;
import com.opencsv.CSVReader;

import jakarta.annotation.PostConstruct;

@Service
public class RegistroVacinacaoService {

    private final RegistroVacinacaoRepository repository;

    public RegistroVacinacaoService(RegistroVacinacaoRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void carregarDadosCSV() {
        try {
            ClassPathResource resource = new ClassPathResource("data/vacinacao.csv");
            try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                reader.readNext(); // pula o cabeçalho
                String[] linha;
                while ((linha = reader.readNext()) != null) {
                    if (linha.length < 6) continue;
                    RegistroVacinacao registro = new RegistroVacinacao(
                            linha[0].trim(),
                            linha[1].trim(),
                            linha[2].trim(),
                            linha[3].trim(),
                            Integer.parseInt(linha[4].trim()),
                            LocalDate.parse(linha[5].trim())
                    );
                    repository.save(registro);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar CSV: " + e.getMessage());
        }
    }

    public List<RegistroVacinacao> listarTodos() {
        return repository.findAll();
    }

    public Optional<RegistroVacinacao> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public List<RegistroVacinacao> filtrar(String vacina, String estado) {
        if (vacina != null && estado != null) {
            return repository.findByVacinaIgnoreCaseAndEstadoIgnoreCase(vacina, estado);
        }
        if (vacina != null) {
            return repository.findByVacinaIgnoreCase(vacina);
        }
        if (estado != null) {
            return repository.findByEstadoIgnoreCase(estado);
        }
        return repository.findAll();
    }

    public RegistroVacinacao salvar(RegistroVacinacao registro) {
        List<RegistroVacinacao> existentes = repository.findByVacinaIgnoreCaseAndEstadoIgnoreCaseAndMunicipioIgnoreCaseAndDoseIgnoreCase(
                registro.getVacina(), registro.getEstado(), registro.getMunicipio(), registro.getDose());
        if (!existentes.isEmpty()) {
            throw new ErrorResponse.ResourceAlreadyExistsException(
                    "Registro já existe para o município " + registro.getMunicipio() +
                            ", estado " + registro.getEstado() +
                            ", vacina " + registro.getVacina() +
                            " e dose " + registro.getDose());
        }
        return repository.save(registro);
    }

    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado para deletar");
        }
        
        repository.deleteById(id);
    }
}
