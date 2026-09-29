package org.example.backendo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nomeStanza;

    @Column(nullable = false)
    private Integer postiLetto;

    @Column(nullable = false)
    private Double prezzoPerNotte;

    @Column(length = 1000)
    private String descrizione;

    @Column(nullable = false)
    private boolean disponibile;


}
