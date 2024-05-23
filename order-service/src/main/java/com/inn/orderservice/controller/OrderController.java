package com.inn.orderservice.controller;

import com.inn.orderservice.dto.OrderDto;
import com.inn.orderservice.dto.OrderItemDto;
import com.inn.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderDto> findAll(){
     return orderService.findAll();
    }

    @GetMapping("/find/{orderNumber}")
    public ResponseEntity<String> findOrder(@PathVariable String orderNumber,Authentication authentication) {
        try {
            OrderDto orderDto = orderService.getByOrderNumber(orderNumber, authentication);
            return new ResponseEntity<>("Order " + orderNumber + " is " + orderDto, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    @PatchMapping("/update-order-items/{orderNumber}")
    public ResponseEntity<String> updateOrderItems(@RequestBody List <OrderItemDto> orderItemDto,
                                  @PathVariable String orderNumber, Authentication authentication) {
        try {
            orderService.updateOrderItems(orderItemDto, orderNumber, authentication);
            return new ResponseEntity<>("Order compound was updated successfully.", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    @CircuitBreaker(name = "inventoryIsAvailable", fallbackMethod = "fallBackAddOrder")
    @TimeLimiter(name = "inventoryIsAvailable")
    @Retry(name = "inventoryIsAvailable")
    @PostMapping("/add")
    public CompletableFuture<String> createOrder(@RequestBody List <OrderItemDto> orderItemDtos, Authentication auth) {
       return CompletableFuture.supplyAsync(() -> orderService.newOrder(orderItemDtos, auth));
    }

    @DeleteMapping("/delete/{orderNumber}")
    public ResponseEntity<String> deleteOrder(@PathVariable String orderNumber, Authentication authentication) {
        try {
            orderService.deleteOrder(orderNumber, authentication);
            return new ResponseEntity<>("Order was deleted successfully.",HttpStatus.OK);
        } catch (RuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    public CompletableFuture<String> fallBackAddOrder(List<OrderItemDto> orderItemDtos, RuntimeException runtimeException) {
        return CompletableFuture.supplyAsync(() -> "Something went wrong, please order after some time!");
    }

    public CompletableFuture<String> fallBackSendOrder(String orderNumber, RuntimeException runtimeException) {
        return CompletableFuture.supplyAsync(() -> "Something went wrong, please send after some time!");
    }

    @GetMapping("/logout")
    private String performLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        String redirectUrl = "http://localhost:8080/realms/candy-shop-realm/protocol/openid-connect/logout";
        response.sendRedirect(redirectUrl);

        return "You are logout ";
    }
}