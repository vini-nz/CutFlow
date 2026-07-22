package com.cutflow.exception;

/**
 * Usuario autenticado, mas sem permissao para a acao (ex: MEMBRO tentando
 * gerenciar a equipe). Mapeado para 403 no GlobalExceptionHandler.
 */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
