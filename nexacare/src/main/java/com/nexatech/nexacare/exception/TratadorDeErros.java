package com.nexatech.nexacare.exception;

import com.nexatech.nexacare.dto.ErroResponse;
import com.nexatech.nexacare.exception.ExcecoesNegocio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converte exceções em mensagens amigáveis; detalhes técnicos ficam só no log do servidor. */
@RestControllerAdvice
public class TratadorDeErros {
    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regra(RegraNegocioException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.getMessage(), null));
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroResponse> conflito(ConflitoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(e.getMessage(), null));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ResponseEntity<ErroResponse> negado(AcessoNegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErroResponse(e.getMessage(), null));
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    ResponseEntity<ErroResponse> naoAutenticado(NaoAutenticadoException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErroResponse(e.getMessage(), null));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(NaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        boolean soObrigatorios = campos.values().stream().allMatch("Campo obrigatório."::equals);
        String mensagem = soObrigatorios ? "Preencha todos os campos obrigatórios."
                : campos.values().iterator().next();
        return ResponseEntity.badRequest().body(new ErroResponse(mensagem, campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErroResponse> requisicaoInvalida(Exception e) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse("Dados inválidos. Confira os campos informados (datas e horários).", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("Não foi possível concluir a operação. Tente novamente.", null));
    }
}
