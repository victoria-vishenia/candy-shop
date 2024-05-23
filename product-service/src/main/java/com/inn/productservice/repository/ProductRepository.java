package com.inn.productservice.repository;

import com.inn.productservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Product findByName(String productName);
    @Query("SELECT p FROM Product p WHERE LOWER(p.compound) LIKE LOWER(concat('%', :word, '%'))")
    List<Product> findByDescriptionContainingWord(String word);

}
