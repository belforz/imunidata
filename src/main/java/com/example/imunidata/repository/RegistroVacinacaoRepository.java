package com.example.imunidata.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.imunidata.model.RegistroVacinacao;

@Repository
public interface RegistroVacinacaoRepository extends JpaRepository<RegistroVacinacao, Long> {

    List<RegistroVacinacao> findByVacina(String vacina);

    List<RegistroVacinacao> findByEstado(String estado);

    List<RegistroVacinacao> findByVacinaAndEstado(String vacina, String estado);

    List<RegistroVacinacao> findByVacinaIgnoreCaseAndEstadoIgnoreCaseAndMunicipioIgnoreCaseAndDoseIgnoreCase(String vacina, String estado, String municipio, String dose);

    List<RegistroVacinacao> findByVacinaIgnoreCaseAndEstadoIgnoreCaseAndMunicipioIgnoreCaseAndDoseIgnoreCaseAndIdNot(String vacina, String estado, String municipio, String dose, Long id);

    boolean existsByVacinaAndEstadoAndMunicipioAndDose(String vacina, String estado, String municipio, String dose);
}
