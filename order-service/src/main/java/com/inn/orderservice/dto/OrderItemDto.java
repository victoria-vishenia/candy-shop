package com.inn.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemDto {

        private String invCode;
        private Double price;
        private Integer quantity;
        private String orderNumber;
    }

