package com.nexatech.nexacare.repository;

import com.nexatech.nexacare.model.Notificacao;
import com.nexatech.nexacare.model.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    /** 0 = sem filtro (equipe vê tudo; profissional e paciente são filtrados pelo próprio id). */
    @Query("""
            select n from Notificacao n
            where (:pacienteId = 0L or n.agendamento.paciente.id = :pacienteId)
              and (:profissionalId = 0L or n.agendamento.profissional.id = :profissionalId)
            order by n.criadaEm desc, n.id desc""")
    List<Notificacao> listar(@Param("pacienteId") Long pacienteId, @Param("profissionalId") Long profissionalId);

    boolean existsByAgendamentoIdAndTipo(Long agendamentoId, TipoNotificacao tipo);
}
