package org.example.backendo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfigRequest {

    @NotNull(message = "Il supplemento per ospite extra è obbligatorio")
    @DecimalMin(value = "0.0", message = "Il supplemento per ospite extra non può essere negativo")
    private Double supplementoOspiteExtra;

    private Double moltiplicatoreGiugnoSettembre;
    private Double moltiplicatoreLuglio;
    private Double moltiplicatoreAgosto;
}
