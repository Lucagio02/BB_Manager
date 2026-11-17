package org.example.backendo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPriceCalculationRequest {

    @NotNull(message = "L'ID della stanza è obbligatorio")
    private Long roomId;

    @NotNull(message = "La data di check-in è obbligatoria")
    private LocalDate checkIn;

    @NotNull(message = "La data di check-out è obbligatoria")
    private LocalDate checkOut;

    @NotNull(message = "Il numero di ospiti è obbligatorio")
    @Min(value = 1, message = "Il numero di ospiti deve essere almeno 1")
    private Integer numeroOspiti;
}
