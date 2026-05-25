package com.example.imunidata.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "registro_vacinacao")
@Schema(description = "Registro de vacinação baseado nos dados do OpenDataSUS")
public class RegistroVacinacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Índice sequencial do registro (gerado automaticamente)", example = "1")
    private Long id;

    @Schema(description = "Identificador único do documento (OpenDataSUS)", example = "c8a9789c-a4d7-4851-a698-bd39895f922c-i0b0")
    private String coDocumento;

    @Schema(description = "Hash anonimizado do paciente", example = "3ebb002b287e37b4e3713b9cc3bac86438e3ad0b")
    private String coPaciente;

    @Schema(description = "Sexo do paciente", example = "M", allowableValues = {"M", "F", "I"})
    private String sexo;

    @Schema(description = "Raça/cor do paciente", example = "BRANCA")
    private String racaCor;

    @Column(nullable = false)
    @Schema(description = "Município do paciente", example = "SAO PAULO")
    private String municipio;

    @Column(nullable = false, length = 2)
    @Schema(description = "UF do paciente", example = "SP")
    private String estado;

    @Schema(description = "Idade do paciente", example = "13")
    private Integer idade;

    @Schema(description = "Nome do estabelecimento de saúde", example = "UBS VILA TEREZINHA")
    private String estabelecimento;

    @Column(nullable = false)
    @Schema(description = "Nome da vacina aplicada", example = "Vacina dengue (atenuada)")
    private String vacina;

    @Column(nullable = false)
    @Schema(description = "Data da vacinação", example = "2026-01-01")
    private LocalDate dataVacina;

    @Column(nullable = false)
    @Schema(description = "Dose aplicada", example = "1ª Dose", allowableValues = {"1ª Dose", "2ª Dose", "3ª Dose", "Dose Única", "Reforço"})
    private String dose;

    @Schema(description = "Local de aplicação da vacina", example = "Face Externa Inferior do Braço Esquerdo")
    private String localAplicacao;

    @Schema(description = "Via de administração", example = "Subcutânea")
    private String viaAdministracao;

    @Schema(description = "Código do lote da vacina", example = "561413")
    private String loteVacina;

    @Schema(description = "Fabricante da vacina", example = "IDT BIOLOGIKA GMBH")
    private String fabricante;

    @Schema(description = "Estratégia de vacinação", example = "Rotina")
    private String estrategia;

    @Schema(description = "Origem do registro", example = "Registro anterior/Transcrição de caderneta")
    private String origemRegistro;

    public RegistroVacinacao() {}

    public RegistroVacinacao(String coDocumento, String coPaciente, String sexo, String racaCor,
                              String municipio, String estado, Integer idade, String estabelecimento,
                              String vacina, LocalDate dataVacina, String dose, String localAplicacao,
                              String viaAdministracao, String loteVacina, String fabricante,
                              String estrategia, String origemRegistro) {
        this.coDocumento = coDocumento;
        this.coPaciente = coPaciente;
        this.sexo = sexo;
        this.racaCor = racaCor;
        this.municipio = municipio;
        this.estado = estado;
        this.idade = idade;
        this.estabelecimento = estabelecimento;
        this.vacina = vacina;
        this.dataVacina = dataVacina;
        this.dose = dose;
        this.localAplicacao = localAplicacao;
        this.viaAdministracao = viaAdministracao;
        this.loteVacina = loteVacina;
        this.fabricante = fabricante;
        this.estrategia = estrategia;
        this.origemRegistro = origemRegistro;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCoDocumento() { return coDocumento; }
    public void setCoDocumento(String coDocumento) { this.coDocumento = coDocumento; }

    public String getCoPaciente() { return coPaciente; }
    public void setCoPaciente(String coPaciente) { this.coPaciente = coPaciente; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getRacaCor() { return racaCor; }
    public void setRacaCor(String racaCor) { this.racaCor = racaCor; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getIdade() { return idade; }
    public void setIdade(Integer idade) { this.idade = idade; }

    public String getEstabelecimento() { return estabelecimento; }
    public void setEstabelecimento(String estabelecimento) { this.estabelecimento = estabelecimento; }

    public String getVacina() { return vacina; }
    public void setVacina(String vacina) { this.vacina = vacina; }

    public LocalDate getDataVacina() { return dataVacina; }
    public void setDataVacina(LocalDate dataVacina) { this.dataVacina = dataVacina; }

    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }

    public String getLocalAplicacao() { return localAplicacao; }
    public void setLocalAplicacao(String localAplicacao) { this.localAplicacao = localAplicacao; }

    public String getViaAdministracao() { return viaAdministracao; }
    public void setViaAdministracao(String viaAdministracao) { this.viaAdministracao = viaAdministracao; }

    public String getLoteVacina() { return loteVacina; }
    public void setLoteVacina(String loteVacina) { this.loteVacina = loteVacina; }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getEstrategia() { return estrategia; }
    public void setEstrategia(String estrategia) { this.estrategia = estrategia; }

    public String getOrigemRegistro() { return origemRegistro; }
    public void setOrigemRegistro(String origemRegistro) { this.origemRegistro = origemRegistro; }

}
