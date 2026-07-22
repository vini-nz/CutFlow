package com.cutflow.exception;

/**
 * Nao ha usuario autenticado na requisicao (sessao ausente/expirada). Mapeado
 * para 401 no GlobalExceptionHandler - o frontend redireciona para o login.
 */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String message) {
        super(message);
    }
}
