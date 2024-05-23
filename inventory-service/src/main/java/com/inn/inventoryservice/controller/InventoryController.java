package com.inn.inventoryservice.controller;

import com.inn.inventoryservice.dto.InventoryDto;
import com.inn.inventoryservice.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/all")
    public List<InventoryDto> getAll(){
       List <InventoryDto> inventoryDtoList
        = inventoryService.getAll();

       return inventoryDtoList.stream()
               .map(inventory -> InventoryDto.builder()
                       .invCode(inventory.getInvCode())
                       .quantity(inventory.getQuantity())
                       .price(inventory.getPrice()).build())
               .collect(Collectors.toList());
    }
    @GetMapping("/find/{invCode}")
    public InventoryDto getByInvCode(@PathVariable String invCode){
        return inventoryService.getByInvCode(invCode);
    }
    @PostMapping("/create")
    public ResponseEntity<String> createInventory(@RequestBody List<InventoryDto> inventoryDtos) {
        inventoryService.createInventory(inventoryDtos);
        return new ResponseEntity<>("Inventories created successfully", HttpStatus.CREATED);
    }
    @PatchMapping ("/update")
    public ResponseEntity<String> updateInventory(@RequestBody List<InventoryDto> inventoryDtos) {
        inventoryService.updateInventory(inventoryDtos);
        return new ResponseEntity<>("Inventory updated successfully", HttpStatus.CREATED);
    }
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteInventory(@RequestBody List <InventoryDto> inventoryDtos) {
        inventoryService.deleteInventory(inventoryDtos);
        return new ResponseEntity<>("Inventories deleted successfully", HttpStatus.OK);
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

