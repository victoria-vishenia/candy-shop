package com.inn.orderservice.service;

import com.inn.orderservice.Utils.OrderMapper;
import com.inn.orderservice.dto.InventoryDto;
import com.inn.orderservice.dto.OrderDto;
import com.inn.orderservice.dto.OrderItemDto;
import com.inn.orderservice.model.Order;
import com.inn.orderservice.model.OrderItem;
import com.inn.orderservice.model.Status;
import com.inn.orderservice.repository.OrderItemRepository;
import com.inn.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaTemplate<String, List<InventoryDto>> kafkaTemplate;
    private final OrderMapper mapper;

    @Transactional(readOnly = true)
    public List<OrderDto> findAll() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(mapper::mapToOrderDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getByOrderNumber(String orderNumber, Authentication authentication) {
        Order order = orderRepository.getOrderByOrderNumber(orderNumber).get();
        String currentUser = authentication.getName();
        log.info("CURRENT USER: " + currentUser);
        boolean isAdmin = isAdmin(authentication);
        String orderOwner = order.getUserId();
        log.info("ORDER OWNER: " + orderNumber);
        if (!currentUser.equals(orderOwner) && !isAdmin) {
            throw new RuntimeException("You have no access rights to this order");
        }
        return mapper.mapToOrderDto(order);
    }

    @Transactional
    public String newOrder(List<OrderItemDto> orderItemDtoList, Authentication auth) {

        String userName = auth.getName();
        log.info("USER NAME IS : " + userName);
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderStatus(Status.INITIATED);
        order.setUserId(userName);
        orderRepository.save(order);
        log.info("1. USER NAME IS : " + order.getUserId());
        LocalDateTime currentTime = LocalDateTime.now();
        log.info("ORDER IS BEING PROCESSED: " + currentTime);
        return initiateOrder(order, orderItemDtoList);
    }

    @Transactional
    public String initiateOrder(Order order, List<OrderItemDto> orderItemDtoList) {
        if (!orderItemDtoList.isEmpty()) {
            List<OrderItem> orderItemList = orderItemDtoList.stream()
                    .map(mapper::mapToOrderItem).collect(Collectors.toList());
            order.setOrderItems(orderItemList);
            for (OrderItem orderItem : orderItemList) {
                if (orderItem != null) {
                    orderItem.setOrder(order);
                }
            }
            List<InventoryDto> inventoryDtoList = orderItemList.stream()
                    .map(mapper::mapToInventoryDto).collect(Collectors.toList());

            LocalDateTime currentTime = LocalDateTime.now();
            log.info("ORDER HAS BEEN INITIATED: " + currentTime);
            log.info("2. USER NAME IS : " + order.getUserId());
            kafkaTemplate.send("orderInitiation", inventoryDtoList);
            return "Order in process.Check status";
        } else {
            return "Your basket is empty";
        }
    }

    @KafkaListener(topics = "availabilityOfInventoryItems", groupId = "order-lifecycle")
    public void orderCreation(List<InventoryDto> availableItems) {
        String orderNumber = availableItems.get(0).getOrderNumber();
        Order order = orderRepository.getOrderByOrderNumber(orderNumber).get();
        List<OrderItem> orderItemsPrev = orderItemRepository.findAllByOrderId(order.getId());
        orderItemRepository.deleteAll(orderItemsPrev);
        //check availability at list of item for creation the order
        if (availableItems.size() == 1 && availableItems.get(0).getQuantity() == 0) {
            order.setOrderItems(null);
            order.setOrderStatus(Status.REJECTED);
        } else {
            List<OrderItem> orderItems = availableItems.stream()
                    .map(mapper::mapInventoryToOrderItem).map(orderItem -> {
                        orderItem.setOrder(order);
                        return orderItem;
                    })
                    .collect(Collectors.toList());
            order.setOrderItems(orderItems);
            order.setOrderStatus(Status.CREATED);
            log.info("3. USER NAME IS : " + order.getUserId());
            kafkaTemplate.send("orderCreation", availableItems);
        }
    }

    @Transactional
    @KafkaListener(topics = "sendingOrder", groupId = "order-send-consumer")
    public void sendOrder(List<InventoryDto> inventoryDtoList) {
        String orderNumber = inventoryDtoList.get(0).getOrderNumber();
        Order order = orderRepository.getOrderByOrderNumber(orderNumber).get();
        List<OrderItem> orderItemsPrev = orderItemRepository.findAllByOrderId(order.getId());
        orderItemRepository.deleteAll(orderItemsPrev);
        if (inventoryDtoList.size() == 1 && inventoryDtoList.get(0).getQuantity() == 0) {
            order.setOrderItems(null);
            order.setOrderStatus(Status.REJECTED);
            LocalDateTime currentTime = LocalDateTime.now();
            log.info("4. USER NAME IS : " + order.getUserId());
            log.info("ORDER WAS REJECTED : " + currentTime);
        } else {
            List<OrderItem> orderItems = inventoryDtoList.stream()
                    .map(mapper::mapInventoryToOrderItem).map(orderItem -> {
                        orderItem.setOrder(order);
                        return orderItem;
                    })
                    .collect(Collectors.toList());
            order.setOrderItems(orderItems);
            order.setOrderStatus(Status.SENT);
            LocalDateTime currentTime = LocalDateTime.now();
            log.info("4. USER NAME IS : " + order.getUserId());
            log.info("ORDER WAS SENT: " + currentTime);
        }
    }

    @Transactional
    public void updateOrderItems(List<OrderItemDto> orderItemDtos
            , String orderNumber, Authentication authentication) {
        Optional<Order> myOrder = orderRepository.getOrderByOrderNumber(orderNumber);
        String currentUser = authentication.getName();
        boolean isAdmin = isAdmin(authentication);
        String orderOwner = myOrder.get().getUserId();
        if (!currentUser.equals(orderOwner) && !isAdmin) {
            throw new RuntimeException("You have no rights for changing this order");
        }
        if (myOrder.isEmpty()) {
            throw new RuntimeException("Order is not found");
        }
        Order order = myOrder.get();
        Status status = order.getOrderStatus();
        if (status == Status.SENT) {
            throw new RuntimeException("Order has already sent. Please create new one.");
        } else if (status == Status.CREATED) {
            throw new RuntimeException("Order had been already created");
        } else if (status == Status.REJECTED || status == Status.INITIATED) {
            initiateOrder(order, orderItemDtos);
        }
    }

    @Transactional
    public void deleteOrder(String orderNumber, Authentication authentication) {
        Optional<Order> order = orderRepository.getOrderByOrderNumber(orderNumber);
        if (order.isEmpty()) {
            throw new RuntimeException("Order not found");
        }
        String currentUser = authentication.getName();
        boolean isAdmin = isAdmin(authentication);
        String orderOwner = order.get().getUserId();
        if (!currentUser.equals(orderOwner) && !isAdmin) {
            throw new RuntimeException("You have no rights for deleting this order");
        }
            Status orderStatus = order.get().getOrderStatus();
            if (orderStatus.equals(Status.SENT)) {
                throw new RuntimeException("Order had been already sent");
            } else if (orderStatus.equals(Status.CREATED)) {
                throw new RuntimeException("Order had been already created");
            } else if (orderStatus.equals(Status.INITIATED)) {
                orderItemRepository.deleteAll(order.get().getOrderItems());
                order.get().setOrderItems(null);
                order.get().setOrderStatus(Status.REJECTED);
                deleteOrder(orderNumber, authentication);
            } else if (orderStatus.equals(Status.REJECTED)) {
                orderRepository.deleteOrderByOrderNumber(orderNumber);
            }
        }

    private boolean isAdmin(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        GrantedAuthority roleAdmin = new SimpleGrantedAuthority("ROLE_ADMIN");
        if (!authorities.contains(roleAdmin)) {
            return false;
        } else {
            return true;
        }
    }
}


