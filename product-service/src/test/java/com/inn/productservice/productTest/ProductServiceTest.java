package com.inn.productservice.productTest;

import com.inn.productservice.Utils.ProductMapper;
import com.inn.productservice.dto.InventoryDto;
import com.inn.productservice.dto.ProductDto;
import com.inn.productservice.model.Product;
import com.inn.productservice.repository.ProductRepository;
import com.inn.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static com.inn.productservice.productTest.ProductTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private KafkaTemplate<String, List<InventoryDto>> kafkaTemplate;
    @Mock
    private ProductMapper mapper;
    @InjectMocks
    private ProductService productService;
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("Display list of all products")
    public void testGetAllProducts() {

        when(productRepository.findAll()).thenReturn(productList);
        when(mapper.mapToProductDto(product1)).thenReturn(productDto1);
        when(mapper.mapToProductDto(product2)).thenReturn(productDto2);
        List<ProductDto> result = productService.getAllProducts();
        assertEquals(2, result.size());
        assertEquals("Product1", result.get(0).getName());
        assertEquals("Product 2", result.get(1).getName());
    }

    @Test
    @DisplayName("Find product by name")
    public void testGetByName_ExistingProduct() {
        when(productRepository.findByName("Product1")).thenReturn(product1);
        when(mapper.mapToProductDto(product1)).thenReturn(productDto1);
        ProductDto result = productService.getByName("Product1");
        assertNotNull(result);
        assertEquals("Product1", result.getName());

    }

    @Test
    @DisplayName("Find product by name - failed")
    public void testGetByName_NonExistingProduct() {
        String nonExistingProductName = "NonExistingProduct";
        when(productRepository.findByName(nonExistingProductName)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> productService.getByName(nonExistingProductName));
    }

    @Test
    @DisplayName("Find product by compound")
    public void testGetByDescriptionContainingWord_ExistingProducts() {
        String keyword = "choco";
        when(productRepository.findByDescriptionContainingWord(keyword)).thenReturn(List.of(product1));
        List<ProductDto> result = productService.getByDescriptionContainingWord(keyword);
        assertEquals(1, result.size());
        verify(productRepository, times(2)).findByDescriptionContainingWord(keyword);
    }

    @Test
    @DisplayName("Find product by compound - failed")
    public void testGetByDescriptionContainingWord_NoProductsFound() {
        String nonExistingKeyword = "nonexistent";
        when(productRepository.findByDescriptionContainingWord(nonExistingKeyword)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> productService.getByDescriptionContainingWord(nonExistingKeyword));
        verify(productRepository, times(1)).findByDescriptionContainingWord(nonExistingKeyword);
    }

    @Test
    @DisplayName("Create new product")
    public void testCreateProduct() {
        when(productRepository.findByName("Product1")).thenReturn(null).thenReturn(product1);
        productService.createProduct(List.of(productDto1));
        when(mapper.mapToProductDto(product1)).thenReturn(productDto1);
        verify(productRepository, times(1)).findByName("Product1");
        verify(productRepository, times(1)).save(any(Product.class));
        ProductDto createdProductDto = productService.getByName("Product1");
        assertNotNull(createdProductDto);
        assertEquals("Choco, nuts", createdProductDto.getCompound());
        assertEquals(10.99, createdProductDto.getPrice());
    }

    @Test
    @DisplayName("Create product - already exist")
    public void testCreateProduct_ProductExists() {
        when(productRepository.findByName("Product1"))
                .thenReturn(product1);
        productService.createProduct(List.of(productDto1));
        verify(productRepository, times(1)).findByName("Product1");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Update product")
    public void testUpdateProduct() {
        when(productRepository.findByName("Product 1")).thenReturn(product1);
        when(productRepository.findByName("Product 2")).thenReturn(product2);
        when(mapper.convertToInventoryDto(any())).thenCallRealMethod();
        productService.updateProduct(List.of(updatedProductDto1, updatedProductDto2));
        verify(productRepository, times(1)).save(product1);
        verify(productRepository, times(1)).save(product2);
        verify(kafkaTemplate, times(1)).send(eq("updateProduct"), anyList());
    }
    @Test
    @DisplayName("Delete product")
    public void testDeleteProduct() {

    when(productRepository.findByName("Product 1")).thenReturn(product1);
    when(productRepository.findByName("Product 2")).thenReturn(null);
        productService.deleteProduct(List.of(inventoryDto1, inventoryDto2));
    verify(productRepository, times(1)).delete(any(Product.class));
}
}







