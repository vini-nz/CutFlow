package com.cutflow.exception;

/**
 * Usuario autenticado que ainda nao pertence a nenhuma organizacao - precisa
 * criar ou ser adicionado a uma antes de usar o sistema. Mapeado para 409 com
 * um codigo proprio para o frontend levar ao onboarding de organizacao.
 */
public class SemOrganizacaoException extends RuntimeException {
    public SemOrganizacaoException(String message) {
        super(message);
    }
}
