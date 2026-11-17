package org.example.backendo.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private Long id;
    private String nomeStanza;
    private Integer postiLetto;
    private Double prezzoPerNotte;
    private String descrizione;
    private Boolean disponibile;
}
