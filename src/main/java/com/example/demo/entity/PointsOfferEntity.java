package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.*;


@Data
@Entity
@Table(schema = "demo", name = "points_offers")
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

    @Column(nullable = false)
    private Boolean active;

}
