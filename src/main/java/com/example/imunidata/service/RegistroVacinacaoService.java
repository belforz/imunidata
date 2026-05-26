package com.example.imunidata.service;

import com.example.imunidata.model.ErrorResponse;
import com.example.imunidata.model.RegistroVacinacao;
import com.example.imunidata.repository.RegistroVacinacaoRepository;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RegistroVacinacaoService {

    private final RegistroVacinacaoRepository repository;

    // Cache in-memory
    private final List<RegistroVacinacao> cache = Collections.synchronizedList(new ArrayList<>());
    private final Map<Long, RegistroVacinacao> cacheById = new ConcurrentHashMap<>();
    private final java.util.Set<String> coDocumentosCache = ConcurrentHashMap.newKeySet();

    public RegistroVacinacaoService(RegistroVacinacaoRepository repository) {
        this.repository = repository;
    }

    private CSVReader buildReader(java.io.Reader reader) {
        return new CSVReaderBuilder(reader)
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build();
    }

    @PostConstruct
    public void carregarDadosCSV() {
        Charset[] charsets = { StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1};
        for (Charset charset : charsets) {
            try {
                ClassPathResource resource = new ClassPathResource("data/vacinacao.csv");
                try (CSVReader reader = buildReader(new InputStreamReader(resource.getInputStream(), charset))) {
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
                                    parseData(trim(linha[9])),  // dt_vacina
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
                            if (saved.getCoDocumento() != null) coDocumentosCache.add(saved.getCoDocumento());
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
            try (CSVReader reader = buildReader(new InputStreamReader(new java.io.FileInputStream(file), charset))) {
                reader.readNext(); // pula cabeçalho
                String[] linha;
                int count = 0;
                int ignoradas = 0;
                while ((linha = reader.readNext()) != null) {
                    if (linha.length < 17) { ignoradas++; continue; }
                    try {
                        RegistroVacinacao reg = new RegistroVacinacao(
                                trim(linha[0]), trim(linha[1]), trim(linha[2]), trim(linha[3]),
                                trim(linha[4]), trim(linha[5]), parseIdade(linha[6]), trim(linha[7]),
                                trim(linha[8]), parseData(trim(linha[9])), trim(linha[10]),
                                trim(linha[11]), trim(linha[12]), trim(linha[13]), trim(linha[14]),
                                trim(linha[15]), trim(linha[16])
                        );
                        // evita duplicatas pelo co_documento
                        if (reg.getCoDocumento() != null && !reg.getCoDocumento().isBlank()) {
                            if (coDocumentosCache.contains(reg.getCoDocumento())) { ignoradas++; continue; }
                        }
                        RegistroVacinacao saved = repository.save(reg);
                        cache.add(saved);
                        cacheById.put(saved.getId(), saved);
                        if (saved.getCoDocumento() != null) coDocumentosCache.add(saved.getCoDocumento());
                        count++;
                    } catch (Exception e) {
                        ignoradas++;
                        System.err.println("Linha ignorada [" + e.getClass().getSimpleName() + "]: " + e.getMessage() + " | colunas=" + linha.length + " | linha=" + String.join(";", linha));
                    }
                }
                System.out.println("Arquivo CSV carregado: " + count + " novos registros, " + ignoradas + " ignoradas (charset: " + charset + ")");
                return count;
            } catch (Exception e) {
                System.err.println("Falha ao ler CSV com charset " + charset + ": " + e.getMessage());
            }
        }
        throw new ErrorResponse.GenericServiceException("Não foi possível processar o arquivo CSV");
    }

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    private LocalDate parseData(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(v, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Formato de data não reconhecido: '" + v + "'");
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
            if (coDocumentosCache.contains(registro.getCoDocumento())) {
                throw new ErrorResponse.ResourceAlreadyExistsException(
                        "Registro com co_documento '" + registro.getCoDocumento() + "' já existe");
            }
        }
        RegistroVacinacao saved = repository.save(registro);
        cache.add(saved);
        cacheById.put(saved.getId(), saved);
        if (saved.getCoDocumento() != null) coDocumentosCache.add(saved.getCoDocumento());
        return saved;
    }

    public RegistroVacinacao atualizar(Long id, RegistroVacinacao dados) {
        RegistroVacinacao existente = cacheById.get(id);
        if (existente == null) {
            throw new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado");
        }

        if (dados.getCoDocumento() != null) existente.setCoDocumento(dados.getCoDocumento());
        if (dados.getCoPaciente() != null) existente.setCoPaciente(dados.getCoPaciente());
        if (dados.getSexo() != null) existente.setSexo(dados.getSexo());
        if (dados.getRacaCor() != null) existente.setRacaCor(dados.getRacaCor());
        if (dados.getMunicipio() != null) existente.setMunicipio(dados.getMunicipio());
        if (dados.getEstado() != null) existente.setEstado(dados.getEstado());
        if (dados.getIdade() != null) existente.setIdade(dados.getIdade());
        if (dados.getEstabelecimento() != null) existente.setEstabelecimento(dados.getEstabelecimento());
        if (dados.getVacina() != null) existente.setVacina(dados.getVacina());
        if (dados.getDataVacina() != null) existente.setDataVacina(dados.getDataVacina());
        if (dados.getDose() != null) existente.setDose(dados.getDose());
        if (dados.getLocalAplicacao() != null) existente.setLocalAplicacao(dados.getLocalAplicacao());
        if (dados.getViaAdministracao() != null) existente.setViaAdministracao(dados.getViaAdministracao());
        if (dados.getLoteVacina() != null) existente.setLoteVacina(dados.getLoteVacina());
        if (dados.getFabricante() != null) existente.setFabricante(dados.getFabricante());
        if (dados.getEstrategia() != null) existente.setEstrategia(dados.getEstrategia());
        if (dados.getOrigemRegistro() != null) existente.setOrigemRegistro(dados.getOrigemRegistro());
        repository.save(existente);
        return existente;
    }

    public void deletar(Long id) {
        RegistroVacinacao existente = cacheById.remove(id);
        if (existente == null) {
            throw new ErrorResponse.ResourceNotFoundException("Registro com ID " + id + " não encontrado");
        }
        synchronized (cache) {
            cache.removeIf(r -> id.equals(r.getId()));
        }
        if (existente.getCoDocumento() != null) coDocumentosCache.remove(existente.getCoDocumento());
        repository.deleteById(id);
    }
}
