package com.nexatech.nexacare.exception;

/** Agrupa as exceções de negócio; cada uma vira um status HTTP em {@link TratadorDeErros}. */
public final class ExcecoesNegocio {
    private ExcecoesNegocio() {}

    /** 400 - dado inválido ou regra de negócio violada. */
    public static class RegraNegocioException extends RuntimeException {
        public RegraNegocioException(String mensagem) { super(mensagem); }
    }

    /** 409 - conflito (ex.: horário já ocupado, cadastro duplicado). */
    public static class ConflitoException extends RuntimeException {
        public ConflitoException(String mensagem) { super(mensagem); }
    }

    /** 403 - perfil sem permissão. */
    public static class AcessoNegadoException extends RuntimeException {
        public AcessoNegadoException() { super("Você não possui permissão para realizar esta ação."); }
    }

    /** 401 - sem login válido. */
    public static class NaoAutenticadoException extends RuntimeException {
        public NaoAutenticadoException(String mensagem) { super(mensagem); }
    }

    /** 404 - registro inexistente (ou fora do escopo do usuário). */
    public static class NaoEncontradoException extends RuntimeException {
        public NaoEncontradoException(String mensagem) { super(mensagem); }
    }
}
