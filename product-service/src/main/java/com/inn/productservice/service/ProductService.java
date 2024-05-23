package com.inn.productservice.service;

import com.inn.productservice.Utils.ProductMapper;
import com.inn.productservice.dto.InventoryDto;
import com.inn.productservice.dto.ProductDto;
import com.inn.productservice.model.Product;
import com.inn.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final KafkaTemplate<String, List<InventoryDto>> kafkaTemplate;

    public List<ProductDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        List <ProductDto> productDtos = products.stream().map(mapper::mapToProductDto).toList();
    return productDtos;
    }

    public ProductDto getByName(String name) {
        if (productRepository.findByName(name) != null) {
            Product product = productRepository.findByName(name);
            return mapper.mapToProductDto(product);
        } else {
            throw new RuntimeException("No such candies");
        }
    }

    public List<ProductDto> getByDescriptionContainingWord(String word) {
        if (productRepository.findByDescriptionContainingWord(word) != null) {
            List<Product> products = productRepository.findByDescriptionContainingWord(word);
            return products.stream().map(mapper::mapToProductDto).toList();
        } else {
            throw new RuntimeException("Compound not found");
        }
    }

    public void createProduct(List<ProductDto> productDtos) {
        for (ProductDto productDto : productDtos) {
            if (productRepository.findByName(productDto.getName()) != null) {
                continue;
            } else {
                Product product = Product.builder()
                        .name(productDto.getName())
                        .compound(productDto.getCompound())
                        .price(productDto.getPrice())
                        .build();
                productRepository.save(product);
              mapper.mapToProductDto(product);
            }
        }
    }

    @KafkaListener(topics = "productCreationDueInventory", groupId = "product-window")
    public void createProductsByInventory(List<InventoryDto> inventoryDtos) {
        List<ProductDto> productDtos = new ArrayList<>();
        for (InventoryDto inventoryDto : inventoryDtos) {
            ProductDto productDto = mapper.convertToProductDto(inventoryDto);
            if (productRepository.findByName(productDto.getName()) != null) {
                throw new RuntimeException("Product already exists");
            } else {
                Product product = mapper.mapToProduct(productDto);
                productRepository.save(product);
                productDtos.add(productDto);
            }
        }
    }

    public void updateProduct(List <ProductDto> productDtos) {
        List <InventoryDto> changedPrice = new ArrayList<>();
        for (ProductDto productDto : productDtos) {
            String productName = productDto.getName();
            if (productRepository.findByName(productName) != null) {
                Product product = productRepository.findByName(productName);
                if(!product.getPrice().equals(productDto.getPrice())){
                    changedPrice.add(mapper.convertToInventoryDto(productDto));
                }
                product.setCompound(productDto.getCompound());
                product.setPrice(productDto.getPrice());
                mapper.mapToProductDto(productRepository.save(product));
            }
        }
        if(changedPrice.size()>0){
            kafkaTemplate.send("updateProduct", changedPrice);
        }
    }

    @KafkaListener(topics = "productPriceUpdate", groupId = "product-window")
    public void updateProductDueInventory(List<InventoryDto> inventoryDtos) {
        List<ProductDto> updatedProducts = new ArrayList<>();
        for (InventoryDto inventoryDto : inventoryDtos) {
            String productName = inventoryDto.getInvCode();
            if (productRepository.findByName(productName) == null) {
                throw new RuntimeException("Product not found");
            } else {
                Product product = productRepository.findByName(productName);
                product.setPrice(inventoryDto.getPrice());
                Product updatedProduct = productRepository.save(product);
                ProductDto productDto = mapper.mapToProductDto(updatedProduct);
                updatedProducts.add(productDto);
            }
        }
    }

    @KafkaListener(topics = "deleteInventory", groupId = "product-window")
    public void deleteProductDueInventory(List<InventoryDto> inventoryDtos) {
        for (InventoryDto inventoryDto : inventoryDtos) {
            String productName = inventoryDto.getInvCode();
            if (productRepository.findByName(productName) == null) {
              continue;
            } else {
                Product product = productRepository.findByName(productName);
                productRepository.delete(product);
            }
        }
    }

    public void deleteProduct(List<InventoryDto> inventoryDtos) {
        for (InventoryDto inventoryDto : inventoryDtos) {
            String productName = inventoryDto.getInvCode();
            if (productRepository.findByName(productName) == null) {
                continue;
            } else {
                Product product = productRepository.findByName(productName);
                productRepository.delete(product);
            }
        }
    }
}

