package team.anonyms.athletelink.gameservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
@RequiredArgsConstructor
@Slf4j
public class KafkaConfiguration {
    @Bean
    public NewTopic userChangesTopic(KafkaTopicsProperties topic) {
        return TopicBuilder.name(topic.userChanges()).partitions(3).replicas(1).build();
    }
}
