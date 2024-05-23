package com.inn.orderservice.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDto {

    private String orderNumber;
    private String orderStatus;
    private List<OrderItemDto> orderItemDtos;
    private String userId;
}