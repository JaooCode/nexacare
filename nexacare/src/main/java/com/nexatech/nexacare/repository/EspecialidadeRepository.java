package com.nexatech.nexacare.repository;

import com.nexatech.nexacare.model.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {
    List<Especialidade> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);
}
