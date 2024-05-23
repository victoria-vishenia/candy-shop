package com.inn.productservice.controller;

import com.inn.productservice.dto.InventoryDto;
import com.inn.productservice.dto.ProductDto;
import com.inn.productservice.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/all")
    public List<ProductDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/candy-name/{name}")
    public ResponseEntity<ProductDto> getProductByName(@PathVariable String name) {

        try {
            ProductDto productDto = productService.getByName(name);
            return ResponseEntity.ok(productDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/get/{word}")
    public ResponseEntity<List<ProductDto>> getProductsByCompound(@PathVariable String word) {
        try {
          List <ProductDto> productDtos = productService.getByDescriptionContainingWord(word);
            return ResponseEntity.ok(productDtos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> createProduct(@RequestBody List<ProductDto> productDtos) {
        try {
            productService.createProduct(productDtos);
            return ResponseEntity.status(201).body("Products created successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<String> updateProduct(@RequestBody List<ProductDto> productDtos) {
        try {
           productService.updateProduct(productDtos);
            return ResponseEntity.ok("Products updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteProduct(@RequestBody List<InventoryDto> products) {
     try{ productService.deleteProduct(products);
         return ResponseEntity.ok("Products deleted successfully");
    } catch (RuntimeException e) {
         return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
     }
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

