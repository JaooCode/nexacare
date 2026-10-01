package com.nexatech.nexacare.repository;

import com.nexatech.nexacare.model.Profissional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
    @Query("""
            select p from Profissional p
            where (:todos = true or p.ativo = true)
              and (:especialidadeId = 0L or p.especialidade.id = :especialidadeId)
              and (lower(p.nome) like :busca or lower(p.especialidade.nome) like :busca
                   or lower(coalesce(p.registro, '')) like :busca)
            order by p.nome""")
    List<Profissional> pesquisar(@Param("busca") String busca, @Param("especialidadeId") Long especialidadeId,
                                 @Param("todos") boolean incluirInativos);

    List<Profissional> findByAtivoTrueOrderByNome();

    /** Trava a linha do profissional durante o agendamento para evitar dois pedidos simultâneos no mesmo horário. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profissional p where p.id = :id")
    Optional<Profissional> buscarComTrava(@Param("id") Long id);

    long countByAtivoTrue();

    boolean existsByEspecialidadeId(Long especialidadeId);
}
