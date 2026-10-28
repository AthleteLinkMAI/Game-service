package team.anonyms.athletelink.gameservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import team.anonyms.athletelink.gameservice.services.KafkaConsumerService;
import team.anonyms.athletelink.gameservice.services.PlayerProfileService;
import team.anonyms.athletelink.gameservice.services.RoundResultService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaJacksonConfigTest {

    @Test
    void springCreatesKafkaConsumerWithItsObjectMapper() {
        PlayerProfileService profiles = mock(PlayerProfileService.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(KafkaJacksonConfig.class, KafkaConsumerService.class);
            context.registerBean(PlayerProfileService.class, () -> profiles);
            context.registerBean(RoundResultService.class, () -> mock(RoundResultService.class));
            context.refresh();

            KafkaConsumerService consumer = context.getBean(KafkaConsumerService.class);
            assertNotNull(consumer);

            UUID userId = UUID.randomUUID();
            consumer.receiveUserChange("{\"userChangeType\":\"CREATE\",\"userId\":\"" + userId + "\"}");
            verify(profiles).createProfile(userId);
        }
    }
}
