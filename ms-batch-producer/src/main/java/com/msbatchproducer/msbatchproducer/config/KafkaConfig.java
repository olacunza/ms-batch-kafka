package com.msbatchproducer.msbatchproducer.config;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic recordReadyTopic(@Value("${app.kafka.topic.record-ready}") String topic,
                                     @Value("${app.kafka.partitions:3}") int partitions,
                                     @Value("${app.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas)
            .config("min.insync.replicas", "1").config("retention.ms", "604800000").build();
    }
}
