package org.example.backendo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "room_daily_rates",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_daily_rate_room_data", columnNames = {"room_id", "data"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDailyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private Double prezzo;
}
