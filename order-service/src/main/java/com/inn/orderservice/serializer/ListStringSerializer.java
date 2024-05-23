package com.inn.orderservice.serializer;

import org.apache.kafka.common.serialization.Serializer;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class ListStringSerializer implements Serializer<List<String>> {

    @Override
    public byte[] serialize(String topic, List<String> data) {
        if (data == null)
            return null;

        try {
            StringBuilder sb = new StringBuilder();
            for (String str : data) {
                sb.append(str).append(",");
            }
            String serializedString = sb.toString();

            if (!serializedString.isEmpty()) {
                serializedString = serializedString.substring(0, serializedString.length() - 1);
            }
            return serializedString.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing List<String>", e);
        }
    }
}
