package com.example.imunidata.repository;

import com.example.imunidata.model.RegistroVacinacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroVacinacaoRepository extends JpaRepository<RegistroVacinacao, Long> {
    // Removi atuacao da camada Repository devido aos arquivos longos
}
