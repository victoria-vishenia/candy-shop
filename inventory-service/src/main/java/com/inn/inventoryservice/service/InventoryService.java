package com.inn.inventoryservice.service;

import com.inn.inventoryservice.Utils.InventoryMapper;
import com.inn.inventoryservice.dto.InventoryDto;
import com.inn.inventoryservice.model.Inventory;
import com.inn.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, List<InventoryDto>> kafkaTemplate1;
    private final InventoryMapper mapper;

    @Transactional(readOnly = true)
    public List<InventoryDto> getAll() {

        List<Inventory> inventories = inventoryRepository.findAll();

        return inventories.stream()
                .map(mapper::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryDto getByInvCode(String invCode) {

        return mapper.convertToDto(inventoryRepository
                .findInventoryByInvCode(invCode));
    }

    @Transactional()
    @KafkaListener(topics = "orderInitiation", groupId = "order-lifecycle")
    public void availableInventoryItems(List<InventoryDto> inventoryDtoList) {
        List<InventoryDto> availableInventory = new ArrayList<>();
        //check if inventory's item's availability and quantity relevant and can be added in order
        for (InventoryDto inventory : inventoryDtoList) {
            if (inventoryRepository.findInventoryByInvCode(inventory.getInvCode()) != null
                    && inventoryRepository.findInventoryByInvCode(inventory.getInvCode()).getQuantity()
                    >= inventory.getQuantity()) {
              Double price = inventoryRepository.findInventoryByInvCode(inventory.getInvCode()).getPrice();
                inventory.setPrice(price);
              availableInventory.add(inventory);
            }
        }
        //process situation of unavailability of items and send message with order number and empty basket
        if (availableInventory.size() == 0) {
            InventoryDto inventoryDto = new InventoryDto();
            inventoryDto.setOrderNumber(inventoryDtoList.get(0).getOrderNumber());
            inventoryDto.setQuantity(0);
            availableInventory.add(inventoryDto);
        }
        kafkaTemplate1.send("availabilityOfInventoryItems", availableInventory);
    }

    @Transactional
    @KafkaListener(topics = "orderCreation", groupId = "order-lifecycle")
    public void updateInventoryDueSendingOrder(List<InventoryDto> inventoryDtoList) {
        //list of items, which will be available for sending due the order
        List<InventoryDto> ultimateInventoryDtoList = new ArrayList<>();
        //list of inventory which is finished
        List<InventoryDto> deletedInventories = new ArrayList<>();
        for (InventoryDto inventoryDto : inventoryDtoList) {
            //check if inventory's item's availability and quantity still relevant
            if (inventoryRepository.findInventoryByInvCode(inventoryDto.getInvCode()) != null
                    && inventoryRepository.findInventoryByInvCode(inventoryDto.getInvCode()).getQuantity()
                    >= inventoryDto.getQuantity()) {
                ultimateInventoryDtoList.add(inventoryDto);
                //update inventory amount in the stock
                Inventory inventory = inventoryRepository
                        .findInventoryByInvCode(inventoryDto.getInvCode());
                int newQuantity = inventory.getQuantity() - inventoryDto.getQuantity();
                if (newQuantity > 0) {
                    inventory.setQuantity(newQuantity);
                    inventoryRepository.save(inventory);
                } else {
                    deletedInventories.add(inventoryDto);
                    inventoryRepository.deleteInventoryByInvCode(inventory.getInvCode());
                }
            }
        }
        if(deletedInventories.size()>0){
            kafkaTemplate1.send("deleteInventory", deletedInventories);
        }
        //process situation of unavailability of items
        if (ultimateInventoryDtoList.size() == 0) {
            InventoryDto inventoryDto = new InventoryDto();
            inventoryDto.setOrderNumber(inventoryDtoList.get(0).getOrderNumber());
            inventoryDto.setQuantity(0);
            ultimateInventoryDtoList.add(inventoryDto);
        }
        kafkaTemplate1.send("sendingOrder", ultimateInventoryDtoList);
    }

    @Transactional
    public void createInventory(List<InventoryDto> inventoryDtos) {
        List<InventoryDto> newInventoryDtos = new ArrayList<>();
        for (InventoryDto inventoryDto : inventoryDtos) {
            if (inventoryRepository.findInventoryByInvCode(inventoryDto.getInvCode()) == null) {
                Inventory inventory = new Inventory();
                inventory.setInvCode(inventoryDto.getInvCode());
                inventory.setQuantity(inventoryDto.getQuantity());
                inventory.setPrice(inventoryDto.getPrice());
                inventoryRepository.save(inventory);
                newInventoryDtos.add(inventoryDto);
            }
        }
        //Absolutely new inventories should be added to Product
        kafkaTemplate1.send("productCreationDueInventory", newInventoryDtos);
    }

    @Transactional
    public void updateInventory(List<InventoryDto> inventoryDtos) {
        List<InventoryDto> priceChangedInventory = new ArrayList<>();
        List<InventoryDto> deletedInventories = new ArrayList<>();
        for (InventoryDto inventoryDto : inventoryDtos) {
            if (inventoryRepository.findInventoryByInvCode(inventoryDto.getInvCode()) == null) {
                throw new RuntimeException("Inventory " + inventoryDto.getInvCode()
                        + " doesn't exist");
            } else {
                Inventory inventory = inventoryRepository
                        .findInventoryByInvCode(inventoryDto.getInvCode());
                //if price is changed, product should be changed too
                if (inventory.getPrice() != inventoryDto.getPrice()) {
                    priceChangedInventory.add(inventoryDto);
                }
                inventory.setPrice(inventoryDto.getPrice());
                    inventory.setQuantity(inventoryDto.getQuantity());
                inventoryRepository.save(inventory);
                //if quantity - null, product should be deleted
                if (inventoryDto.getQuantity() == 0) {
                    inventoryRepository.delete(inventory);
                    deletedInventories.add(inventoryDto);
                }
            }
        }
        if (deletedInventories.size() > 0) {
            kafkaTemplate1.send("deleteInventory", deletedInventories);
        }
        if (priceChangedInventory.size() > 0) {
            kafkaTemplate1.send("productPriceUpdate", priceChangedInventory);
        }
    }

    @Transactional
    @KafkaListener(topics = "updateProduct", groupId = "order-lifecycle")
    public void updateDueProductModification(List <InventoryDto> inventoryDtos){
        for (InventoryDto inventoryDto: inventoryDtos) {
            String invCode = inventoryDto.getInvCode();
            if(inventoryRepository.findInventoryByInvCode(invCode)!=null){
                Inventory inventory = inventoryRepository.findInventoryByInvCode(invCode);
                inventory.setPrice(inventoryDto.getPrice());
                inventoryRepository.save(inventory);
            }
        }
    }

    @Transactional
    public void deleteInventory(List<InventoryDto> inventoryDtos) {
        List <InventoryDto> deletedInventories = new ArrayList<>();
        for (InventoryDto inventoryDto : inventoryDtos) {
            String inventoryName = inventoryDto.getInvCode();
            if (inventoryRepository.findInventoryByInvCode(inventoryName) != null) {
                inventoryRepository.deleteInventoryByInvCode(inventoryName);
                deletedInventories.add(inventoryDto);
            } else {
                throw new RuntimeException("Inventories not found");
            }
        }
        if(deletedInventories.size()>0) {
            kafkaTemplate1.send("deleteInventory", deletedInventories);
        }
    }
}


