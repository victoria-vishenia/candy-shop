package com.inn.inventoryservice.repository;

import com.inn.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  Inventory findInventoryByInvCode(String invCode);

   List<Inventory> findAll();
   void deleteInventoryByInvCode(String invCode);
   List<Inventory> findByInvCodeIn( List <String> invCode);
}
