package com.example.imunidata.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "registro_vacinacao")
public class RegistroVacinacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String municipio;

    @Column(nullable = false, length = 2)
    private String estado;

    @Column(nullable = false)
    private String vacina;

    @Column(nullable = false)
    private String dose;

    @Column(nullable = false)
    private Integer quantidadeAplicada;

    @Column(nullable = false)
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

