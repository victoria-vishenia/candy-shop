package com.inn.gatewayservice.config;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator orderServiceRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("order-service", r -> r.path("/order")
                        .uri("lb://order-service"))
                .route("order-find", r -> r.path("/order/all")
                        .uri("lb://order-service"))
                .route("order-find", r -> r.path("/order/find/**")
                        .uri("lb://order-service"))
                .route("order-update-items", r -> r.path("/order/update-order-items/**")
                        .uri("lb://order-service"))
                .route("order-add", r -> r.path("/order/add")
                        .uri("lb://order-service"))
                .route("order-delete", r -> r.path("/order/delete/**")
                        .uri("lb://order-service"))
                .build();
    }

    @Bean
    public RouteLocator inventoryServiceRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("inventory", r -> r.path("/inventory/all")
                        .uri("lb://inventory-service"))
                .route("inventory-find", r -> r.path("/inventory/find/**")
                        .uri("lb://inventory-service"))
                .route("inventory-update", r -> r.path("/inventory/update")
                        .uri("lb://inventory-service"))
                .route("inventory-create", r -> r.path("/inventory/create")
                        .uri("lb://inventory-service"))
                .route("inventory-delete", r -> r.path("/inventory/delete")
                        .uri("lb://inventory-service"))
                .build();
    }

    @Bean
    public RouteLocator productServiceRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r.path("/product/all")
                        .uri("lb://product-service"))
                .route("product-add", r -> r.path("/product/create")
                        .uri("lb://product-service"))
                .route("product-find", r -> r.path("/product/candy-name/**")
                        .uri("lb://product-service"))
                .route("product-get", r -> r.path("/product/get/**")
                        .uri("lb://product-service"))
                .route("product-update", r -> r.path("/product/update")
                        .uri("lb://product-service"))
                .route("product-delete", r -> r.path("/product/delete")
                        .uri("lb://product-service"))
                .build();
    }
}

