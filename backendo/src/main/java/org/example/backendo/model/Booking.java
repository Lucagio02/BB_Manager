package org.example.backendo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    public static final String STATO_IN_ATTESA = "IN_ATTESA";
    public static final String STATO_CONFERMATA = "CONFERMATA";
    public static final String STATO_SCADUTA = "SCADUTA";
    public static final String STATO_CANCELLATA = "CANCELLATA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Double prezzoTotale;

    @Column(nullable = false)
    private String stato;

    @Column(nullable = false)
    private Integer numeroOspiti;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.stato == null) {
            this.stato = STATO_IN_ATTESA;
        }
    }
}
