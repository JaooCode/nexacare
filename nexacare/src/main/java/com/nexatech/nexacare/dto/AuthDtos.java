package com.nexatech.nexacare.dto;

import com.nexatech.nexacare.model.Perfil;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** DTOs de autenticação, usuários e preferências. */
public final class AuthDtos {
    private AuthDtos() {}

    public static final String OBRIGATORIO = "Campo obrigatório.";

    public record LoginRequest(
            @NotBlank(message = OBRIGATORIO) String email,
            @NotBlank(message = OBRIGATORIO) String senha) {}

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, Long profissionalId,
                                  Long pacienteId, String tema, boolean notificacoesAtivas, boolean ativo) {}

    public record LoginResponse(String token, UsuarioResponse usuario) {}

    public record RegistroPacienteRequest(
            @NotBlank(message = OBRIGATORIO) @Size(max = 120) String nome,
            @NotBlank(message = OBRIGATORIO) String cpf,
            @NotNull(message = OBRIGATORIO) @Past(message = "Data de nascimento inválida.") LocalDate dataNascimento,
            @NotBlank(message = OBRIGATORIO) @Size(max = 20) String telefone,
            @NotBlank(message = OBRIGATORIO) @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            @NotBlank(message = OBRIGATORIO) String senha,
            @AssertTrue(message = "É necessário aceitar a política de privacidade.") boolean aceiteLgpd) {}

    public record RecuperarSenhaRequest(@NotBlank(message = OBRIGATORIO) String email) {}

    public record AlterarSenhaRequest(
            @NotBlank(message = OBRIGATORIO) String senhaAtual,
            @NotBlank(message = OBRIGATORIO) String novaSenha) {}

    public record PreferenciasRequest(
            @NotBlank(message = OBRIGATORIO) @Size(max = 120) String nome,
            boolean notificacoesAtivas,
            @NotBlank(message = OBRIGATORIO) @Pattern(regexp = "claro|escuro", message = "Tema inválido.") String tema) {}

    public record UsuarioNovoRequest(
            @NotBlank(message = OBRIGATORIO) @Size(max = 120) String nome,
            @NotBlank(message = OBRIGATORIO) @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            @NotBlank(message = OBRIGATORIO) String senha,
            @NotNull(message = OBRIGATORIO) Perfil perfil) {}

    public record PerfilUsuarioRequest(@NotNull(message = OBRIGATORIO) Perfil perfil) {}
}
