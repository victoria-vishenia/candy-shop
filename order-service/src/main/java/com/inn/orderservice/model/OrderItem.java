package com.inn.orderservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invCode;
    private Double price;
    private Integer quantity;
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;


}