package com.example.imunidata.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
                            LocalDateTime.parse(linha[5].trim())
                    );
                    repository.save(registro);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar CSV: " + e.getMessage());
        }
    }

    public Optional<RegistroVacinacao> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public List<RegistroVacinacao> filtrar(String vacina, String estado) {
        if (vacina != null && estado != null) {
            return repository.findByVacinaAndEstado(vacina, estado);
        }
        if (vacina != null) {
            return repository.findByVacina(vacina);
        }
        if (estado != null) {
            return repository.findByEstado(estado);
        }
        return repository.findAll();
    }

    public RegistroVacinacao salvar(RegistroVacinacao registro) {
        String vacina = registro.getVacina() != null ? registro.getVacina().trim() : null;
        String estado = registro.getEstado() != null ? registro.getEstado().trim() : null;
        String municipio = registro.getMunicipio() != null ? registro.getMunicipio().trim() : null;
        String dose = registro.getDose() != null ? registro.getDose().trim() : null;
        boolean existe = repository.existsByVacinaAndEstadoAndMunicipioAndDose(vacina, estado, municipio, dose);
        if (existe) {
            throw new ErrorResponse.ResourceAlreadyExistsException(
                    "Registro já existe para o município " + municipio +
                            ", estado " + estado +
                            ", vacina " + vacina +
                            " e dose " + dose);
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
