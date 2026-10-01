package com.nexatech.nexacare.repository;

import com.nexatech.nexacare.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    /** Filtros usam sentinelas ("%" e false) em vez de null para funcionar igual em qualquer banco. */
    @Query("""
            select p from Paciente p
            where (:todos = true or p.ativo = true)
              and (lower(p.nome) like :busca or lower(p.email) like :busca
                   or p.cpf like :buscaCpf or p.telefone like :busca)
            order by p.nome""")
    List<Paciente> pesquisar(@Param("busca") String busca, @Param("buscaCpf") String buscaCpf,
                             @Param("todos") boolean incluirInativos);

    /** Pacientes que já têm (ou tiveram) consulta com o profissional. */
    @Query("""
            select distinct a.paciente from Agendamento a
            where a.profissional.id = :profissionalId
              and (lower(a.paciente.nome) like :busca or a.paciente.cpf like :buscaCpf)
            order by a.paciente.nome""")
    List<Paciente> pesquisarDoProfissional(@Param("profissionalId") Long profissionalId,
                                           @Param("busca") String busca, @Param("buscaCpf") String buscaCpf);

    long countByAtivoTrue();
}
