package com.nexatech.nexacare.dto;

import com.nexatech.nexacare.model.StatusAgendamento;
import com.nexatech.nexacare.model.TipoNotificacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.nexatech.nexacare.dto.AuthDtos.OBRIGATORIO;

/** DTOs de agendamentos, agenda (grade de horários), notificações, dashboard e configurações. */
public final class AgendaDtos {
    private AgendaDtos() {}

    public record AgendamentoRequest(
            Long pacienteId,
            @NotNull(message = OBRIGATORIO) Long profissionalId,
            Long especialidadeId,
            @NotNull(message = OBRIGATORIO) LocalDate data,
            @NotNull(message = OBRIGATORIO) LocalTime hora,
            @Size(max = 255, message = "Observação muito longa (máximo 255 caracteres).") String observacao) {}

    public record StatusRequest(@NotNull(message = OBRIGATORIO) StatusAgendamento status) {}

    public record AgendamentoResponse(Long id, Long pacienteId, String pacienteNome, String pacienteTelefone,
                                      Long profissionalId, String profissionalNome, Long especialidadeId,
                                      String especialidade, LocalDate data, LocalTime hora,
                                      StatusAgendamento status, String statusRotulo, String observacao,
                                      boolean remarcacaoSolicitada, LocalDateTime criadoEm) {}

    /** estado: LIVRE, OCUPADO ou INDISPONIVEL (horário que já passou). */
    public record Slot(LocalTime hora, String estado, Long agendamentoId, String pacienteNome,
                       StatusAgendamento status) {}

    public record GradeProfissional(Long profissionalId, String nome, String especialidade, List<Slot> slots) {}

    public record GradeDia(LocalDate data, boolean diaUtil, List<GradeProfissional> profissionais) {}

    public record NotificacaoResponse(Long id, Long agendamentoId, TipoNotificacao tipo, String mensagem,
                                      boolean lida, LocalDateTime criadaEm, String pacienteNome,
                                      String profissionalNome) {}

    public record DashboardResponse(String perfil, long consultasHoje, long consultasSemana, long canceladas,
                                    long realizadas, Long pacientes, Long profissionais, Long horariosLivresHoje,
                                    Long remarcacoesPendentes, Long usuarios, Integer taxaCancelamento,
                                    long notificacoesNaoLidas, List<AgendamentoResponse> proximas) {}

    public record ConfiguracaoSistema(
            @NotNull(message = OBRIGATORIO) @Pattern(regexp = "\\d{2}:\\d{2}", message = "Horário inválido.") String inicio,
            @NotNull(message = OBRIGATORIO) @Pattern(regexp = "\\d{2}:\\d{2}", message = "Horário inválido.") String fim,
            @NotNull(message = OBRIGATORIO) Integer duracaoMinutos,
            @NotNull(message = OBRIGATORIO) @Pattern(regexp = "\\d{2}:\\d{2}", message = "Horário inválido.") String pausaInicio,
            @NotNull(message = OBRIGATORIO) @Pattern(regexp = "\\d{2}:\\d{2}", message = "Horário inválido.") String pausaFim) {}
}
