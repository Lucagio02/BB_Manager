package org.example.backendo.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPriceCalculationResponse {

    private Long roomId;
    private String roomName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer numeroOspiti;
    private Long numeroNotti;
    private Double prezzoTotale;
}
