package com.nexatech.nexacare.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

import static com.nexatech.nexacare.dto.AuthDtos.OBRIGATORIO;

/** DTOs de pacientes, profissionais e especialidades. */
public final class CadastroDtos {
    private CadastroDtos() {}

    public record PacienteRequest(
            @NotBlank(message = OBRIGATORIO) @Size(max = 120) String nome,
            @NotBlank(message = OBRIGATORIO) String cpf,
            @NotNull(message = OBRIGATORIO) @Past(message = "Data de nascimento inválida.") LocalDate dataNascimento,
            @NotBlank(message = OBRIGATORIO) @Size(max = 20) String telefone,
            @NotBlank(message = OBRIGATORIO) @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            @Size(max = 255, message = "Observação muito longa (máximo 255 caracteres).") String observacao) {}

    /** cpf/email podem vir nulos ou mascarados conforme o perfil de quem consulta (minimização de dados). */
    public record PacienteResponse(Long id, String nome, String cpf, LocalDate dataNascimento, String telefone,
                                   String email, String observacao, boolean ativo) {}

    public record ProfissionalRequest(
            @NotBlank(message = OBRIGATORIO) @Size(max = 120) String nome,
            @NotNull(message = OBRIGATORIO) Long especialidadeId,
            @Size(max = 30) String registro,
            @NotBlank(message = OBRIGATORIO) @Size(max = 20) String telefone,
            @NotBlank(message = OBRIGATORIO) @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            boolean ativo,
            /** Opcional: se informada, cria também o acesso (login) do profissional. */
            String senhaInicial) {}

    public record ProfissionalResponse(Long id, String nome, Long especialidadeId, String especialidade,
                                       String registro, String telefone, String email, boolean ativo,
                                       boolean possuiAcesso) {}

    public record EspecialidadeRequest(@NotBlank(message = OBRIGATORIO) @Size(max = 80) String nome) {}

    public record EspecialidadeResponse(Long id, String nome) {}
}
