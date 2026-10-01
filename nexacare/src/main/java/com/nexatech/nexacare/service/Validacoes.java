package com.nexatech.nexacare.service;

import com.nexatech.nexacare.exception.ExcecoesNegocio.RegraNegocioException;

import java.util.regex.Pattern;

/** Validações básicas de dados (CPF, e-mail, telefone, senha) e máscara de CPF para exibição. */
public final class Validacoes {
    private Validacoes() {}

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Valida os dígitos verificadores do CPF e devolve apenas os 11 dígitos. */
    public static String validarCpf(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1
                || d.charAt(9) - '0' != digito(d, 9) || d.charAt(10) - '0' != digito(d, 10)) {
            throw new RegraNegocioException("CPF inválido.");
        }
        return d;
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static String validarEmail(String email) {
        String e = normalizarEmail(email);
        if (e == null || !EMAIL.matcher(e).matches()) {
            throw new RegraNegocioException("E-mail inválido.");
        }
        return e;
    }

    public static String validarTelefone(String telefone) {
        int n = somenteDigitos(telefone).length();
        if (n < 10 || n > 11) {
            throw new RegraNegocioException("Telefone inválido. Informe DDD e número.");
        }
        return telefone.trim();
    }

    public static void validarSenha(String senha) {
        boolean ok = senha != null && senha.length() >= 8
                && senha.chars().anyMatch(Character::isLetter) && senha.chars().anyMatch(Character::isDigit);
        if (!ok) {
            throw new RegraNegocioException("A senha deve ter no mínimo 8 caracteres, com letras e números.");
        }
    }

    public static String mascararCpf(String cpf) {
        return cpf == null || cpf.length() != 11 ? null
                : "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }

    public static String formatarCpf(String cpf) {
        return cpf == null || cpf.length() != 11 ? cpf
                : cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    /** Termo de busca para LIKE; sem termo devolve "%" (casa com tudo). */
    public static String padraoBusca(String busca) {
        return busca == null || busca.isBlank() ? "%" : "%" + busca.trim().toLowerCase() + "%";
    }

    /** Busca por CPF só faz sentido com dígitos; sem dígitos devolve um padrão que não casa. */
    public static String padraoBuscaCpf(String busca) {
        if (busca == null || busca.isBlank()) return "%";
        String d = somenteDigitos(busca);
        return d.isEmpty() ? "#" : "%" + d + "%";
    }
}
