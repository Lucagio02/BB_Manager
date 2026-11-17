package org.example.backendo.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;
    private Long roomId;
    private String roomName;
    private Long userId;
    private String userEmail;
    private String userName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer numeroOspiti;
    private Double prezzoTotale;
    private String stato;
    private LocalDateTime createdAt;
}
