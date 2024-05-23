package com.inn.orderservice.Utils;

import com.inn.orderservice.dto.InventoryDto;
import com.inn.orderservice.dto.OrderDto;
import com.inn.orderservice.dto.OrderItemDto;
import com.inn.orderservice.model.Order;
import com.inn.orderservice.model.OrderItem;
import com.inn.orderservice.model.Status;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderItem mapToOrderItem(OrderItemDto orderItemDto) {
        OrderItem orderItem = new OrderItem();
        orderItem.setPrice(orderItemDto.getPrice());
        orderItem.setQuantity(orderItemDto.getQuantity());
        orderItem.setInvCode(orderItemDto.getInvCode());
        return orderItem;
    }
    public OrderItem mapInventoryToOrderItem(InventoryDto inventoryDto) {
        OrderItem orderItem = new OrderItem();
        orderItem.setInvCode(inventoryDto.getInvCode());
        orderItem.setQuantity(inventoryDto.getQuantity());
        orderItem.setPrice(inventoryDto.getPrice());
        return orderItem;
    }
    public OrderItemDto mapToOrderItemDto(OrderItem orderItem) {
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setPrice(orderItem.getPrice());
        orderItemDto.setQuantity(orderItem.getQuantity());
        orderItemDto.setInvCode(orderItem.getInvCode());
        orderItemDto.setOrderNumber(orderItem.getOrder().getOrderNumber());
        return orderItemDto;
    }

    public OrderDto mapToOrderDto (Order order){
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderNumber(order.getOrderNumber());
        orderDto.setOrderStatus(order.getOrderStatus().toString());
        orderDto.setUserId(order.getUserId());
        orderDto.setOrderItemDtos(order.getOrderItems()
                .stream().map(this::mapToOrderItemDto).toList());
        return orderDto;
    }

    public Order mapToOrder (OrderDto orderDto){
        Order order = new Order();
        order.setOrderNumber(orderDto.getOrderNumber());
        order.setOrderStatus(Status.valueOf(orderDto.getOrderStatus()));
        order.setUserId(orderDto.getUserId());
        order.setOrderItems(orderDto.getOrderItemDtos().stream()
                .map(this::mapToOrderItem).collect(Collectors.toList()));
        return order;
    }
    public InventoryDto mapToInventoryDto (OrderItem orderItem){
        InventoryDto inventoryDto = new InventoryDto();
        inventoryDto.setInvCode(orderItem.getInvCode());
        inventoryDto.setQuantity(orderItem.getQuantity());
        inventoryDto.setOrderNumber(orderItem.getOrder().getOrderNumber());
        return inventoryDto;
    }
}
