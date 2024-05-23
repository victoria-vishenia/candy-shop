package com.inn.orderservice.repository;


import com.inn.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findOrderByOrderNumber(String orderNumber);

    Optional<Order> getOrderByOrderNumber(String orderNumber);
    List<Order> findAll ();

    void deleteOrderByOrderNumber(String orderNumber);
}
