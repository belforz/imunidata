package com.example.imunidata.service;

import com.example.imunidata.model.ErrorResponse;
import com.example.imunidata.model.RegistroVacinacao;
import com.example.imunidata.repository.RegistroVacinacaoRepository;
import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RegistroVacinacaoService {

    private final RegistroVacinacaoRepository repository;

    // Cache in-memory
    private final List<RegistroVacinacao> cache = new ArrayList<>();
    private final Map<Long, RegistroVacinacao> cacheById = new ConcurrentHashMap<>();

    public RegistroVacinacaoService(RegistroVacinacaoRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void carregarDadosCSV() {
        Charset[] charsets = { StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1};
        for (Charset charset : charsets) {
            try {
                ClassPathResource resource = new ClassPathResource("data/vacinacao.csv");
                try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream(), charset))) {
                    reader.readNext();
                    String[] linha;
                    while ((linha = reader.readNext()) != null) {
                        if (linha.length < 17) continue;
                        try {
                            RegistroVacinacao reg = new RegistroVacinacao(
                                    trim(linha[0]),   // co_documento
                                    trim(linha[1]),   // co_paciente
                                    trim(linha[2]),   // tp_sexo_paciente
                                    trim(linha[3]),   // no_raca_cor_paciente
                                    trim(linha[4]),   // no_municipio_paciente
                                    trim(linha[5]),   // sg_uf_paciente
                                    parseIdade(linha[6]),  // nu_idade_paciente
                                    trim(linha[7]),   // no_fantasia_estabelecimento
                                    trim(linha[8]),   // ds_vacina
                                    LocalDate.parse(trim(linha[9])),  // dt_vacina (yyyy-MM-dd)
                                    trim(linha[10]),  // ds_dose_vacina
                                    trim(linha[11]),  // ds_local_aplicacao
                                    trim(linha[12]),  // ds_via_administracao
                                    trim(linha[13]),  // co_lote_vacina
                                    trim(linha[14]),  // ds_vacina_fabricante
                                    trim(linha[15]),  // ds_estrategia_vacinacao
                                    trim(linha[16])   // ds_origem_registro
                            );
                            RegistroVacinacao saved = repository.save(reg);
                            cache.add(saved);
                            cacheById.put(saved.getId(), saved);
                        } catch (Exception e) {
                            System.err.println("Linha ignorada no CSV: " + e.getMessage());
                        }
                    }
                }
                System.out.println("CSV carregado com sucesso: " + cache.size() + " registros (charset: " + charset + ")");
                break;
            } catch (Exception e) {
                System.err.println("Falha ao carregar CSV com charset " + charset + ": " + e.getMessage());
            }
        }
    }

    public int carregarDadosCSVArquivo(File file) {
        if (!file.getName().endsWith(".csv")) {
            throw new ErrorResponse.GenericServiceException("Arquivo inválido: esperado .csv, recebido: " + file.getName());
        }
        Charset[] charsets = { StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1 };
        for (Charset charset : charsets) {
            try (CSVReader reader = new CSVReader(new InputStreamReader(new java.io.FileInputStream(file), charset))) {
                reader.readNext(); // pula cabeçalho
                String[] linha;
                int count = 0;
                while ((linha = reader.readNext()) != null) {
                    if (linha.length < 17) continue;
                    try {
                        RegistroVacinacao reg = new RegistroVacinacao(
                                trim(linha[0]), trim(linha[1]), trim(linha[2]), trim(linha[3]),
                                trim(linha[4]), trim(linha[5]), parseIdade(linha[6]), trim(linha[7]),
                                trim(linha[8]), LocalDate.parse(trim(linha[9])), trim(linha[10]),
                                trim(linha[11]), trim(linha[12]), trim(linha[13]), trim(linha[14]),
                                trim(linha[15]), trim(linha[16])
                        );
                        // evita duplicatas pelo co_documento
                        if (reg.getCoDocumento() != null && !reg.getCoDocumento().isBlank()) {
                            boolean existe = cache.stream().anyMatch(r -> reg.getCoDocumento().equals(r.getCoDocumento()));
                            if (existe) continue;
                        }
                        RegistroVacinacao saved = repository.save(reg);
                        cache.add(saved);
                        cacheById.put(saved.getId(), saved);
                        count++;
                    } catch (Exception e) {
                        System.err.println("Linha ignorada: " + e.getMessage());
                    }
                }
                System.out.println("Arquivo CSV carregado: " + count + " novos registros (charset: " + charset + ")");
                return count;
            } catch (Exception e) {
                System.err.println("Falha ao ler CSV com charset " + charset + ": " + e.getMessage());
            }
        }
        throw new ErrorResponse.GenericServiceException("Não foi possível processar o arquivo CSV");
    }

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }

    private Integer parseIdade(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }


    public Optional<RegistroVacinacao> buscarPorId(Long id) {
        return Optional.ofNullable(cacheById.get(id));
    }

    // Filtro direto do cache in memory sem bater no banco
    public List<RegistroVacinacao> filtrar(String coDocumento, String coPaciente, String sexo, String racaCor,
                                           String municipio, String estado, Integer idade, String estabelecimento,
                                           String vacina, LocalDate dataVacina, String dose, String localAplicacao,
                                           String viaAdministracao, String loteVacina, String fabricante,
                                           String estrategia, String origemRegistro) {
        return cache.stream()
                .filter(r -> vacina == null || r.getVacina().equalsIgnoreCase(vacina))
                .filter(r -> estado == null || r.getEstado().equalsIgnoreCase(estado))
                .filter(r -> municipio == null || r.getMunicipio().equalsIgnoreCase(municipio))
                .filter(r -> coDocumento == null || r.getCoDocumento().equalsIgnoreCase(coDocumento))
                .filter(r -> sexo == null || r.getSexo().equalsIgnoreCase(sexo))
                .filter(r -> racaCor == null || r.getRacaCor().equalsIgnoreCase(racaCor))
                .filter(r -> coPaciente == null || r.getCoPaciente().equalsIgnoreCase(coPaciente))
                .filter(r -> idade == null || r.getIdade().equals(idade))
                .filter(r -> estabelecimento == null || r.getEstabelecimento().equalsIgnoreCase(estabelecimento))
                .filter(r -> dataVacina == null || r.getDataVacina().equals(dataVacina))
                .filter(r -> dose == null || r.getDose().equalsIgnoreCase(dose))
                .filter(r -> localAplicacao == null || r.getLocalAplicacao().equalsIgnoreCase(localAplicacao))
                .filter(r -> viaAdministracao == null || r.getViaAdministracao().equalsIgnoreCase(viaAdministracao))
                .filter(r -> loteVacina == null || r.getLoteVacina().equalsIgnoreCase(loteVacina))
                .filter(r -> fabricante == null || r.getFabricante().equalsIgnoreCase(fabricante))
                .filter(r -> estrategia == null || r.getEstrategia().equalsIgnoreCase(estrategia))
                .filter(r -> origemRegistro == null || r.getOrigemRegistro().equalsIgnoreCase(origemRegistro))
                .collect(Collectors.toList());
    }

    public RegistroVacinacao salvar(RegistroVacinacao registro) {
        if (registro.getCoDocumento() != null && !registro.getCoDocumento().isBlank()) {
            boolean existe = cache.stream()
                    .anyMatch(r -> registro.getCoDocumento().equals(r.getCoDocumento()));
            if (existe) {
                throw new ErrorResponse.ResourceAlreadyExistsException(
                        "Registro com co_documento '" + registro.getCoDocumento() + "' já existe");
            }
        }
        RegistroVacinacao saved = repository.save(registro);
        cache.add(saved);
        cacheById.put(saved.getId(), saved);
        return saved;
    }

    public RegistroVacinacao atualizar(Long id, RegistroVacinacao dados) {
        RegistroVacinacao existente = cacheById.get(id);
        if (existente == null) {
            throw new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado");
        }
        existente.setMunicipio(dados.getMunicipio());
        existente.setEstado(dados.getEstado());
        existente.setVacina(dados.getVacina());
        existente.setDose(dados.getDose());
        existente.setDataVacina(dados.getDataVacina());
        existente.setIdade(dados.getIdade());
        existente.setEstabelecimento(dados.getEstabelecimento());
        existente.setLocalAplicacao(dados.getLocalAplicacao());
        existente.setViaAdministracao(dados.getViaAdministracao());
        existente.setLoteVacina(dados.getLoteVacina());
        existente.setFabricante(dados.getFabricante());
        existente.setEstrategia(dados.getEstrategia());
        existente.setOrigemRegistro(dados.getOrigemRegistro());
        repository.save(existente);
        return existente;
    }

    public void deletar(Long id) {
        RegistroVacinacao existente = cacheById.remove(id);
        if (existente == null) {
            throw new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado");
        }
        cache.remove(existente);
        repository.deleteById(id);
    }
}
