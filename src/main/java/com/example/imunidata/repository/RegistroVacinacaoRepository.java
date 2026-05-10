package com.example.imunidata.repository;

import com.example.imunidata.model.RegistroVacinacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroVacinacaoRepository extends JpaRepository<RegistroVacinacao, Long> {

    List<RegistroVacinacao> findByVacina(String vacina);

    List<RegistroVacinacao> findByEstado(String estado);

    List<RegistroVacinacao> findByVacinaAndEstado(String vacina, String estado);


    boolean existsByVacinaAndEstadoAndMunicipioAndDose(String vacina, String estado, String municipio, String dose);
}
