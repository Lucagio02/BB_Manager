package org.example.backendo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequest {

    @NotBlank(message = "Il nome della stanza è obbligatorio")
    private String nomeStanza;

    @NotNull(message = "I posti letto sono obbligatori")
    @Min(value = 1, message = "Deve esserci almeno 1 posto letto")
    private Integer postiLetto;

    @NotNull(message = "Il prezzo base per notte è obbligatorio")
    @Min(value = 1, message = "Il prezzo base per notte deve essere maggiore di zero")
    private Double prezzoPerNotte;

    private String descrizione;

    private Boolean disponibile;
}
