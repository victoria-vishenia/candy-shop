package com.inn.inventoryservice.inventoryTest;

import com.inn.inventoryservice.Utils.InventoryMapper;
import com.inn.inventoryservice.dto.InventoryDto;
import com.inn.inventoryservice.model.Inventory;
import com.inn.inventoryservice.repository.InventoryRepository;
import com.inn.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static com.inn.inventoryservice.inventoryTest.InventoryTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class InventoryServiceTest {
    @Mock
    private InventoryRepository inventoryRepositoryMock;
    @Mock
    private KafkaTemplate<String, List<InventoryDto>> kafkaTemplateMock;
    @Mock
    private InventoryMapper mapper;
    @InjectMocks
    private InventoryService inventoryServiceMock;
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    @DisplayName("Get all inventory")
    public void testGetAll() {

        when(inventoryRepositoryMock.findAll()).thenReturn(List.of(inventory5, inventory6));
        when(mapper.convertToDto(inventory5)).thenReturn(inventoryDto5);
        when(mapper.convertToDto(inventory6)).thenReturn(inventoryDto6);
        List<InventoryDto> result = inventoryServiceMock.getAll();
        assertEquals(2, result.size());
        assertEquals(inventoryDto5, result.get(0));
        assertEquals(inventoryDto6, result.get(1));
        verify(inventoryRepositoryMock, times(1)).findAll();
        verify(mapper, times(1)).convertToDto(inventory5);
        verify(mapper, times(1)).convertToDto(inventory6);
    }
    @Test
    @DisplayName("Find inventory by inventory code")
    public void testFindByInvCode() {
        List<InventoryDto> inventoryDtoList = List.of(inventoryDtoRequire1, inventoryDtoRequire2);
        when(inventoryRepositoryMock.findInventoryByInvCode("code1"))
                .thenReturn(inventory1);
        when(inventoryRepositoryMock.findInventoryByInvCode("code2"))
                .thenReturn(inventory2);
        inventoryServiceMock.availableInventoryItems(inventoryDtoList);
        List<InventoryDto> returnList = List.of(inventoryDtoRequire1);
        verify(kafkaTemplateMock).send("availabilityOfInventoryItems", returnList);
    }
    @Test
    @DisplayName("Update inventory")
    public void testUpdateExistingInventory() {
        when(inventoryRepositoryMock.findInventoryByInvCode("code4"))
                .thenReturn(existingInventory);
        assertEquals(existingInventory.getQuantity(), 10);
        assertEquals(existingInventory.getPrice(), 2.33);
        inventoryServiceMock.updateInventory(List.of(newInventoryDto));
        verify(inventoryRepositoryMock, times(2)).findInventoryByInvCode("code4");
        assertEquals(existingInventory.getQuantity(), 67);
        assertEquals(existingInventory.getPrice(), 4.22);
    }
    @Test
    @DisplayName("Update inventory, no such inventory")
    public void testUpdateNoExistingInventory() {
        when(inventoryRepositoryMock.findInventoryByInvCode("code4"))
                .thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            inventoryServiceMock.updateInventory(List.of(newInventoryDto)));
        verify(inventoryRepositoryMock, times(1)).findInventoryByInvCode("code4");
        assertEquals("Inventory code4 doesn't exist", ex.getMessage());
    }
    @Test
    @DisplayName("Create new inventory")
    public void testCreateInventory(){
        when(inventoryRepositoryMock.findInventoryByInvCode(eq("code4"))).thenReturn(null);
        assertNull(inventoryRepositoryMock.findInventoryByInvCode("code4"));
        inventoryServiceMock.createInventory(List.of(inventoryDto));
        verify(inventoryRepositoryMock, times(2)).findInventoryByInvCode(eq("code4"));
        verify(inventoryRepositoryMock, times(1)).save(any(Inventory.class));
    }
    @Test
    @DisplayName("Update inventory due sending order")
    public void testUpdateInventoryDueSendingOrder_ItemsAvailableForSending() {
        when(inventoryRepositoryMock.findInventoryByInvCode("code5"))
                .thenReturn(inventory5);
        when(inventoryRepositoryMock.findInventoryByInvCode("code6"))
                .thenReturn(inventory6);
        inventoryServiceMock.updateInventoryDueSendingOrder(expectedInventoryList);
        verify(inventoryRepositoryMock, times(3)).findInventoryByInvCode("code5");
        verify(inventoryRepositoryMock, times(3)).findInventoryByInvCode("code6");
        verify(inventoryRepositoryMock, times(2)).save(any(Inventory.class));
        verify(inventoryRepositoryMock, never()).deleteInventoryByInvCode(anyString());
        verify(kafkaTemplateMock).send(eq("sendingOrder"), eq(expectedInventoryList));
    }
}
