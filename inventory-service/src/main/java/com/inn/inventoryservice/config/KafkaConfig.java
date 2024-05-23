package com.inn.inventoryservice.config;

import com.inn.inventoryservice.dto.InventoryDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;
    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerializer;
    @Value("${spring.kafka.consumer.group-id}")
    private String consumerId;
    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;
    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    private  Map<String, Object> configProps = new HashMap<>();
    @Bean
    public NewTopic orderSendTopic1() {
        return TopicBuilder.name("availabilityOfInventoryItems")
                .build();
    }
    @Bean
    public NewTopic orderSendTopic2() {
        return TopicBuilder.name("sendingOrder")
                .build();
    }
    @Bean
    public NewTopic addNewInventory(){
        return TopicBuilder.name("productCreationDueInventory")
                .build();
    }
    @Bean
    public NewTopic updatePrice(){
        return TopicBuilder.name("productPriceUpdate")
                .build();
    }
    @Bean
    public NewTopic deleteInventory(){
        return TopicBuilder.name("deleteInventory")
                .build();
    }

    @Bean
    public ProducerFactory<String, List<InventoryDto>> producerFactory() {

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    @Bean
    public KafkaTemplate<String, List<InventoryDto>> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, List<InventoryDto>> consumerFactory() {
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, List<InventoryDto>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, List<InventoryDto>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}

