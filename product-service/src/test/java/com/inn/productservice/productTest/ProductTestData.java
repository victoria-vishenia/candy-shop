package com.inn.productservice.productTest;

import com.inn.productservice.dto.InventoryDto;
import com.inn.productservice.dto.ProductDto;
import com.inn.productservice.model.Product;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

public class ProductTestData {

  public static ProductDto testProduct = new ProductDto("ProductTest", "ProductCompound", 10.0);
  public static Product product1 = Product.builder().name("Product1").compound("Choco, nuts").price(10.99).build();
  public static Product product2 = Product.builder().name("Product 2").compound("Jelly, water").price(20.99).build();
  public static ProductDto productDto1 = new ProductDto("Product1", "Choco, nuts", 10.99);
  public static ProductDto productDto2 = new ProductDto("Product 2", "Jelly, water", 20.99);
  public static ProductDto updatedProductDto1 = new ProductDto("Product 1", "Choco, nuts, jelly", 10.11);
  public static ProductDto updatedProductDto2 = new ProductDto("Product 2", "Choco, jelly", 19.99);
  public static List<Product> productList = Arrays.asList(product1, product2);
  public static InventoryDto inventoryDto1 = InventoryDto.builder().invCode("Product 1").price(1.33).build();
  public static InventoryDto inventoryDto2 = InventoryDto.builder().invCode("Product 2").price(2.33).build();
  public static SpringBootTest.WebEnvironment webEnvironment;
}
