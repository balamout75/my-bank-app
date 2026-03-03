package com.mybank.accounts.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${application.kafka.topic.notifications:notifications}")
    private String notificationsTopic;

    @Value("${application.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${application.kafka.topic.replicas:1}")
    private int replicas;

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(notificationsTopic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
