package org.example.backendo.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRateResponse {

    private Long id;
    private Long roomId;
    private LocalDate data;
    private Double prezzo;
}
