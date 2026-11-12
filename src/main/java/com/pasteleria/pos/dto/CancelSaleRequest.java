package com.pasteleria.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelSaleRequest(
        @NotBlank(message = "Indicá el motivo de la anulación")
        @Size(max = 500, message = "El detalle no puede superar los 500 caracteres")
        String detail
) {
}
