package com.inn.productservice.Utils;

import com.inn.productservice.dto.InventoryDto;
import com.inn.productservice.dto.ProductDto;
import com.inn.productservice.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductDto mapToProductDto(Product product) {
        return ProductDto.builder()
                .name(product.getName())
                .compound(product.getCompound())
                .price(product.getPrice())
                .build();
    }
    public Product mapToProduct(ProductDto productDto) {
        return Product.builder()
                .name(productDto.getName())
                .compound(productDto.getCompound())
                .price(productDto.getPrice())
                .build();
    }
   public ProductDto convertToProductDto(InventoryDto inventoryDto) {
        ProductDto dto = new ProductDto();
        dto.setName(inventoryDto.getInvCode());
        dto.setPrice(inventoryDto.getPrice());
        dto.setCompound("Compound");
        return dto;
    }
    public InventoryDto convertToInventoryDto(ProductDto productDto) {
        InventoryDto dto = new InventoryDto();
        dto.setInvCode(productDto.getName());
        dto.setPrice(productDto.getPrice());
        return dto;
    }

}
