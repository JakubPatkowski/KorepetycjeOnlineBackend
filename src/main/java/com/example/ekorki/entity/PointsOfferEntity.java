package com.example.ekorki.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.*;


@Data
@Entity
@Table(schema = "e_korki", name = "points_offers")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PointsOfferEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "offer_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OfferType offerType;  // Zamiast boolean active

    public enum OfferType {
        BUY,    // Użytkownik kupuje punkty
        SELL    // Nauczyciel wypłaca punkty
    }

}
