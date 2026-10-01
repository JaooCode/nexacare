package com.nexatech.nexacare.controller;

import com.nexatech.nexacare.dto.CadastroDtos.PacienteRequest;
import com.nexatech.nexacare.dto.CadastroDtos.PacienteResponse;
import com.nexatech.nexacare.security.UsuarioLogado;
import com.nexatech.nexacare.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {
    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<PacienteResponse> listar(UsuarioLogado usuario, @RequestParam(required = false) String busca,
                                         @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.listar(busca, incluirInativos, usuario);
    }

    @GetMapping("/{id}")
    public PacienteResponse buscar(UsuarioLogado usuario, @PathVariable Long id) {
        return service.buscar(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse criar(UsuarioLogado usuario, @Valid @RequestBody PacienteRequest r) {
        return service.criar(r, usuario);
    }

    @PutMapping("/{id}")
    public PacienteResponse atualizar(UsuarioLogado usuario, @PathVariable Long id,
                                      @Valid @RequestBody PacienteRequest r) {
        return service.atualizar(id, r, usuario);
    }

    @PatchMapping("/{id}/status")
    public PacienteResponse alterarStatus(UsuarioLogado usuario, @PathVariable Long id, @RequestParam boolean ativo) {
        return service.alterarStatus(id, ativo, usuario);
    }

    /** Exclusão lógica: o cadastro é desativado (o histórico de consultas é preservado). */
    @DeleteMapping("/{id}")
    public PacienteResponse desativar(UsuarioLogado usuario, @PathVariable Long id) {
        return service.alterarStatus(id, false, usuario);
    }
}
