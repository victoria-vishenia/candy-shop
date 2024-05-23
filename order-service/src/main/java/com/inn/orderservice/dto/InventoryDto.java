package com.inn.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryDto {

        private String invCode;
        private Integer quantity;
        private String orderNumber;
        private Double price;

}
