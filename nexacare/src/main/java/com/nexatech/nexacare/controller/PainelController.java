package com.nexatech.nexacare.controller;

import com.nexatech.nexacare.dto.AgendaDtos.ConfiguracaoSistema;
import com.nexatech.nexacare.dto.AgendaDtos.DashboardResponse;
import com.nexatech.nexacare.dto.AuthDtos.PerfilUsuarioRequest;
import com.nexatech.nexacare.dto.AuthDtos.UsuarioNovoRequest;
import com.nexatech.nexacare.dto.AuthDtos.UsuarioResponse;
import com.nexatech.nexacare.security.UsuarioLogado;
import com.nexatech.nexacare.service.ConfiguracaoService;
import com.nexatech.nexacare.service.DashboardService;
import com.nexatech.nexacare.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.nexatech.nexacare.model.Perfil.ADMINISTRADOR;

/** Dashboard, configurações do sistema e gestão de usuários. */
@RestController
@RequestMapping("/api")
public class PainelController {
    private final DashboardService dashboard;
    private final ConfiguracaoService configuracao;
    private final UsuarioService usuarios;

    public PainelController(DashboardService dashboard, ConfiguracaoService configuracao, UsuarioService usuarios) {
        this.dashboard = dashboard;
        this.configuracao = configuracao;
        this.usuarios = usuarios;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(UsuarioLogado usuario) {
        return dashboard.montar(usuario);
    }

    @GetMapping("/configuracoes")
    public ConfiguracaoSistema configuracoes() {
        return configuracao.obter();
    }

    @PutMapping("/configuracoes")
    public ConfiguracaoSistema atualizarConfiguracoes(UsuarioLogado usuario, @Valid @RequestBody ConfiguracaoSistema c) {
        usuario.exigir(ADMINISTRADOR);
        return configuracao.atualizar(c);
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listarUsuarios(UsuarioLogado usuario) {
        return usuarios.listar(usuario);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criarUsuario(UsuarioLogado usuario, @Valid @RequestBody UsuarioNovoRequest r) {
        return usuarios.criar(r, usuario);
    }

    @PatchMapping("/usuarios/{id}/ativo")
    public UsuarioResponse alterarAtivo(UsuarioLogado usuario, @PathVariable Long id, @RequestParam boolean valor) {
        return usuarios.alterarAtivo(id, valor, usuario);
    }

    @PatchMapping("/usuarios/{id}/perfil")
    public UsuarioResponse alterarPerfil(UsuarioLogado usuario, @PathVariable Long id,
                                         @Valid @RequestBody PerfilUsuarioRequest r) {
        return usuarios.alterarPerfil(id, r.perfil(), usuario);
    }
}
