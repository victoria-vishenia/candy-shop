package com.inn.inventoryservice.Utils;

import com.inn.inventoryservice.dto.InventoryDto;
import com.inn.inventoryservice.model.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryDto convertToDto(Inventory inventory) {
        InventoryDto dto = new InventoryDto();
        dto.setInvCode(inventory.getInvCode());
        dto.setQuantity(inventory.getQuantity());
        dto.setPrice(inventory.getPrice());
        return dto;
    }

    public Inventory convertToInventory(InventoryDto inventoryDto) {
        Inventory inventory = new Inventory();
        inventory.setInvCode(inventoryDto.getInvCode());
        inventory.setQuantity(inventoryDto.getQuantity());
        inventory.setPrice(inventoryDto.getPrice());
        return inventory;
    }
}
