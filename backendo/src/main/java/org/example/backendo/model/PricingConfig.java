package org.example.backendo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="pricing_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfig {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double moltiplicatoreGiugnoSettembre;

    @Column(nullable = false)
    private Double moltiplicatoreLuglio;

    @Column(nullable = false)
    private Double moltiplicatoreAgosto;

    @Column(nullable = false)
    private Double supplementoOspiteExtra;
}

