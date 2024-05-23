package com.inn.productservice.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inn.productservice.dto.InventoryDto;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.ArrayList;
import java.util.Map;

public class InventoryDtoListSerializer implements Serializer<ArrayList<InventoryDto>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, ArrayList<InventoryDto> data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing ArrayList<InventoryDto> to byte[]", e);
        }
    }
}



