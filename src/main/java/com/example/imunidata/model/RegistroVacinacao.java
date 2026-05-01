package com.example.imunidata.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "registro_vacinacao")
@Schema(description = "Registro de vacinação com dados de município, estado, vacina aplicada e quantidade")
public class RegistroVacinacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do registro", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Nome do município", example = "São Paulo")
    private String municipio;

    @Column(nullable = false, length = 2)
    @Schema(description = "Sigla do estado (2 letras)", example = "SP")
    private String estado;

    @Column(nullable = false)
    @Schema(description = "Tipo de vacina aplicada", example = "BCG", allowableValues = {"BCG", "Gripe", "Pentavalente", "Hepatite B", "Febre Amarela"})
    private String vacina;

    @Column(nullable = false)
    @Schema(description = "Dose da vacina", example = "1ª Dose", allowableValues = {"1ª Dose", "2ª Dose", "3ª Dose", "Reforço", "Dose Única"})
    private String dose;

    @Column(nullable = false)
    @Schema(description = "Quantidade de doses aplicadas", example = "15234")
    private Integer quantidadeAplicada;

    @Column(nullable = false)
    @Schema(description = "Data do registro", example = "2024-01-15")
    private LocalDate dataRegistro;

    public RegistroVacinacao() {}

    public RegistroVacinacao(String municipio, String estado, String vacina, String dose,
                              Integer quantidadeAplicada, LocalDate dataRegistro) {
        this.municipio = municipio;
        this.estado = estado;
        this.vacina = vacina;
        this.dose = dose;
        this.quantidadeAplicada = quantidadeAplicada;
        this.dataRegistro = dataRegistro;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getVacina() { return vacina; }
    public void setVacina(String vacina) { this.vacina = vacina; }

    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }

    public Integer getQuantidadeAplicada() { return quantidadeAplicada; }
    public void setQuantidadeAplicada(Integer quantidadeAplicada) { this.quantidadeAplicada = quantidadeAplicada; }

    public LocalDate getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDate dataRegistro) { this.dataRegistro = dataRegistro; }
}
