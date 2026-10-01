package com.nexatech.nexacare.controller;

import com.nexatech.nexacare.dto.CadastroDtos.*;
import com.nexatech.nexacare.security.UsuarioLogado;
import com.nexatech.nexacare.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfissionalController {
    private final ProfissionalService service;

    public ProfissionalController(ProfissionalService service) {
        this.service = service;
    }

    @GetMapping("/profissionais")
    public List<ProfissionalResponse> listar(UsuarioLogado usuario, @RequestParam(required = false) String busca,
                                             @RequestParam(required = false) Long especialidadeId,
                                             @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.listar(busca, especialidadeId, incluirInativos, usuario);
    }

    @GetMapping("/profissionais/{id}")
    public ProfissionalResponse buscar(UsuarioLogado usuario, @PathVariable Long id) {
        return service.buscar(id, usuario);
    }

    @PostMapping("/profissionais")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfissionalResponse criar(UsuarioLogado usuario, @Valid @RequestBody ProfissionalRequest r) {
        return service.criar(r, usuario);
    }

    @PutMapping("/profissionais/{id}")
    public ProfissionalResponse atualizar(UsuarioLogado usuario, @PathVariable Long id,
                                          @Valid @RequestBody ProfissionalRequest r) {
        return service.atualizar(id, r, usuario);
    }

    @PatchMapping("/profissionais/{id}/status")
    public ProfissionalResponse alterarStatus(UsuarioLogado usuario, @PathVariable Long id,
                                              @RequestParam boolean ativo) {
        return service.alterarStatus(id, ativo, usuario);
    }

    @DeleteMapping("/profissionais/{id}")
    public ProfissionalResponse desativar(UsuarioLogado usuario, @PathVariable Long id) {
        return service.alterarStatus(id, false, usuario);
    }

    @GetMapping("/especialidades")
    public List<EspecialidadeResponse> especialidades() {
        return service.listarEspecialidades();
    }

    @PostMapping("/especialidades")
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialidadeResponse criarEspecialidade(UsuarioLogado usuario,
                                                    @Valid @RequestBody EspecialidadeRequest r) {
        return service.criarEspecialidade(r, usuario);
    }
}
