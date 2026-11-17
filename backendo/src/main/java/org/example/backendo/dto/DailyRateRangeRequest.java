package org.example.backendo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRateRangeRequest {

    @NotNull(message = "La data di inizio è obbligatoria")
    private LocalDate startDate;

    @NotNull(message = "La data di fine è obbligatoria")
    private LocalDate endDate;

    @NotNull(message = "Il prezzo è obbligatorio")
    @DecimalMin(value = "0.01", message = "Il prezzo deve essere maggiore di zero")
    private Double prezzo;
}
