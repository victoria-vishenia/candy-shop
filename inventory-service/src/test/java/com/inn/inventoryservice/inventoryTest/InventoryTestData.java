package com.inn.inventoryservice.inventoryTest;

import com.inn.inventoryservice.dto.InventoryDto;
import com.inn.inventoryservice.model.Inventory;

import java.util.Arrays;
import java.util.List;

public class InventoryTestData {
  public static   Inventory inventory1 = new Inventory(656L, "code1", 5, 9.33);
  public static   Inventory inventory2 = new Inventory(454L, "code2", 0, 8.33);
  public static   InventoryDto inventoryDtoRequire1 = new InventoryDto("code1", 5, 9.33, "orderN");
  public static   InventoryDto inventoryDtoRequire2 = new InventoryDto("code2", 10, 8.33, "orderN");
  public static InventoryDto inventoryDto = new InventoryDto("code4", 10, 2.33, "order3");
  public static Inventory existingInventory = new Inventory(656L, "code4", 10, 2.33);
  public static InventoryDto newInventoryDto = InventoryDto.builder().invCode("code4").quantity(67).price(4.22).build();
  public static Inventory inventory5 = Inventory.builder().invCode("code5").quantity(67).price(4.22).build();;
  public static Inventory inventory6 = Inventory.builder().invCode("code6").quantity(67).price(4.22).build();
  public static InventoryDto inventoryDto5 = InventoryDto.builder().invCode("code5").quantity(67).price(4.22).build();
  public static InventoryDto inventoryDto6 = InventoryDto.builder().invCode("code6").quantity(67).price(4.22).build();

  public static InventoryDto getInventoryDto1(){
    InventoryDto inventoryDto1 = new InventoryDto();
    inventoryDto1.setInvCode("code5");
    inventoryDto1.setQuantity(10);
    return inventoryDto1;
  }

  public static InventoryDto getInventoryDto2(){
    InventoryDto inventoryDto2 = new InventoryDto();
    inventoryDto2.setInvCode("code6");
    inventoryDto2.setQuantity(10);
    return inventoryDto2;
  }
  public static List<InventoryDto> expectedInventoryList =
          Arrays.asList(getInventoryDto1(), getInventoryDto2());
}
