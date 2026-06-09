package com.example.itsm_final.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Talep onceligi (Dusuk, Normal, Yuksek, Acil...) lookup tablosu.
 * level_weight kullanici siralamasi icin kullanilir (ASC: dusukten yuksek onem'e).
 */
@Entity
@Table(name = "priorities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Priority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "priority_name", nullable = false, length = 50)
    private String priorityName;

    @Column(name = "level_weight", nullable = false)
    private Integer levelWeight;

    @Override
    public String toString() {
        return priorityName;
    }
}
