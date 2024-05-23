package com.inn.inventoryservice.model;

import lombok.*;

import jakarta.persistence.*;
@Entity
@Table (name = "inventory")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String invCode;
        private Integer quantity;
        private Double price;
    }

