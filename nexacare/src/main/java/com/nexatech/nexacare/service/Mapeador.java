package com.nexatech.nexacare.service;

import com.nexatech.nexacare.dto.AgendaDtos.AgendamentoResponse;
import com.nexatech.nexacare.dto.AgendaDtos.NotificacaoResponse;
import com.nexatech.nexacare.dto.AuthDtos.UsuarioResponse;
import com.nexatech.nexacare.model.Agendamento;
import com.nexatech.nexacare.model.Notificacao;
import com.nexatech.nexacare.model.Usuario;
import org.springframework.stereotype.Component;

/** Conversões entidade -> DTO compartilhadas por vários serviços. */
@Component
public class Mapeador {

    public UsuarioResponse usuario(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                u.getProfissional() != null ? u.getProfissional().getId() : null,
                u.getPaciente() != null ? u.getPaciente().getId() : null,
                u.getTema(), u.isNotificacoesAtivas(), u.isAtivo());
    }

    public AgendamentoResponse agendamento(Agendamento a) {
        return new AgendamentoResponse(a.getId(), a.getPaciente().getId(), a.getPaciente().getNome(),
                a.getPaciente().getTelefone(), a.getProfissional().getId(), a.getProfissional().getNome(),
                a.getProfissional().getEspecialidade().getId(), a.getProfissional().getEspecialidade().getNome(),
                a.getData(), a.getHora(), a.getStatus(), a.getStatus().getRotulo(), a.getObservacao(),
                a.isRemarcacaoSolicitada(), a.getCriadoEm());
    }

    public NotificacaoResponse notificacao(Notificacao n) {
        Agendamento a = n.getAgendamento();
        return new NotificacaoResponse(n.getId(), a.getId(), n.getTipo(), n.getMensagem(), n.isLida(),
                n.getCriadaEm(), a.getPaciente().getNome(), a.getProfissional().getNome());
    }
}
