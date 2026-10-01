package com.nexatech.nexacare.repository;

import com.nexatech.nexacare.model.Agendamento;
import com.nexatech.nexacare.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /** Filtros opcionais usam sentinelas (0 = qualquer, "%" = qualquer texto, datas extremas). */
    @Query("""
            select a from Agendamento a
            where a.data between :inicio and :fim
              and a.status in :statuses
              and (:profissionalId = 0L or a.profissional.id = :profissionalId)
              and (:pacienteId = 0L or a.paciente.id = :pacienteId)
              and (lower(a.paciente.nome) like :busca or lower(a.profissional.nome) like :busca
                   or lower(a.profissional.especialidade.nome) like :busca)
            order by a.data, a.hora""")
    List<Agendamento> filtrar(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim,
                              @Param("statuses") Collection<StatusAgendamento> statuses,
                              @Param("profissionalId") Long profissionalId, @Param("pacienteId") Long pacienteId,
                              @Param("busca") String busca);

    @Query("""
            select count(a) from Agendamento a
            where a.profissional.id = :profissionalId and a.data = :data and a.hora = :hora
              and a.status in :ativos and (:ignorarId = 0L or a.id <> :ignorarId)""")
    long conflitosDoProfissional(@Param("profissionalId") Long profissionalId, @Param("data") LocalDate data,
                                 @Param("hora") LocalTime hora, @Param("ativos") Collection<StatusAgendamento> ativos,
                                 @Param("ignorarId") Long ignorarId);

    @Query("""
            select count(a) from Agendamento a
            where a.paciente.id = :pacienteId and a.data = :data and a.hora = :hora
              and a.status in :ativos and (:ignorarId = 0L or a.id <> :ignorarId)""")
    long conflitosDoPaciente(@Param("pacienteId") Long pacienteId, @Param("data") LocalDate data,
                             @Param("hora") LocalTime hora, @Param("ativos") Collection<StatusAgendamento> ativos,
                             @Param("ignorarId") Long ignorarId);

    List<Agendamento> findByDataAndStatusIn(LocalDate data, Collection<StatusAgendamento> statuses);

    @Query("""
            select count(a) from Agendamento a
            where a.paciente.id = :pacienteId and a.status in :ativos
              and (a.data > :hoje or (a.data = :hoje and a.hora >= :agora))""")
    long futurasDoPaciente(@Param("pacienteId") Long pacienteId, @Param("ativos") Collection<StatusAgendamento> ativos,
                           @Param("hoje") LocalDate hoje, @Param("agora") LocalTime agora);

    @Query("""
            select count(a) from Agendamento a
            where a.profissional.id = :profissionalId and a.status in :ativos
              and (a.data > :hoje or (a.data = :hoje and a.hora >= :agora))""")
    long futurasDoProfissional(@Param("profissionalId") Long profissionalId,
                               @Param("ativos") Collection<StatusAgendamento> ativos,
                               @Param("hoje") LocalDate hoje, @Param("agora") LocalTime agora);

    boolean existsByProfissionalIdAndPacienteId(Long profissionalId, Long pacienteId);

    long countByRemarcacaoSolicitadaTrueAndStatusIn(Collection<StatusAgendamento> statuses);
}
