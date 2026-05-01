package com.example.imunidata.repository;

import com.example.imunidata.model.RegistroVacinacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroVacinacaoRepository extends JpaRepository<RegistroVacinacao, Long> {

    List<RegistroVacinacao> findByVacinaIgnoreCase(String vacina);

    List<RegistroVacinacao> findByEstadoIgnoreCase(String estado);

    List<RegistroVacinacao> findByVacinaIgnoreCaseAndEstadoIgnoreCase(String vacina, String estado);

    List<RegistroVacinacao> findByVacinaIgnoreCaseAndEstadoIgnoreCaseAndMunicipioIgnoreCaseAndDoseIgnoreCase(String vacina, String estado, String municipio, String dose);
}
