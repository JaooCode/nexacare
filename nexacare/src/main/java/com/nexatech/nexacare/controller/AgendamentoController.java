package com.nexatech.nexacare.controller;

import com.nexatech.nexacare.dto.AgendaDtos.*;
import com.nexatech.nexacare.security.UsuarioLogado;
import com.nexatech.nexacare.service.AgendaService;
import com.nexatech.nexacare.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.nexatech.nexacare.model.StatusAgendamento.CANCELADA;

@RestController
@RequestMapping("/api")
public class AgendamentoController {
    private final AgendamentoService service;
    private final AgendaService agenda;

    public AgendamentoController(AgendamentoService service, AgendaService agenda) {
        this.service = service;
        this.agenda = agenda;
    }

    @GetMapping("/agendamentos")
    public List<AgendamentoResponse> listar(
            UsuarioLogado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String busca) {
        return service.listar(inicio, fim, profissionalId, pacienteId, status, busca, usuario);
    }

    @GetMapping("/agendamentos/{id}")
    public AgendamentoResponse buscar(UsuarioLogado usuario, @PathVariable Long id) {
        return service.buscar(id, usuario);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criar(UsuarioLogado usuario, @Valid @RequestBody AgendamentoRequest r) {
        return service.criar(r, usuario);
    }

    @PutMapping("/agendamentos/{id}")
    public AgendamentoResponse atualizar(UsuarioLogado usuario, @PathVariable Long id,
                                         @Valid @RequestBody AgendamentoRequest r) {
        return service.atualizar(id, r, usuario);
    }

    @PatchMapping("/agendamentos/{id}/status")
    public AgendamentoResponse alterarStatus(UsuarioLogado usuario, @PathVariable Long id,
                                             @Valid @RequestBody StatusRequest r) {
        return service.alterarStatus(id, r.status(), usuario);
    }

    @PostMapping("/agendamentos/{id}/solicitar-remarcacao")
    public AgendamentoResponse solicitarRemarcacao(UsuarioLogado usuario, @PathVariable Long id) {
        return service.solicitarRemarcacao(id, usuario);
    }

    /** Cancelamento lógico: a consulta muda para "Cancelada" e o horário volta a ficar livre. */
    @DeleteMapping("/agendamentos/{id}")
    public AgendamentoResponse cancelar(UsuarioLogado usuario, @PathVariable Long id) {
        return service.alterarStatus(id, CANCELADA, usuario);
    }

    @GetMapping("/agendamentos/horarios-livres")
    public List<LocalTime> horariosLivres(
            UsuarioLogado usuario, @RequestParam Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.horariosLivres(data, profissionalId, usuario);
    }

    @GetMapping("/agenda/grade")
    public GradeDia grade(UsuarioLogado usuario,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                          @RequestParam(required = false) Long profissionalId) {
        return agenda.grade(data, profissionalId, usuario);
    }
}
