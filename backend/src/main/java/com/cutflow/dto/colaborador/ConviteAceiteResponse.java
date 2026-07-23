package com.cutflow.dto.colaborador;

import java.util.UUID;

/** Retorno do aceite: para onde o frontend leva o usuario (o projeto). */
public record ConviteAceiteResponse(UUID projetoUuid) {}
