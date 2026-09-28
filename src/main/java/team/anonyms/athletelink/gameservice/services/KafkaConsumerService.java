package team.anonyms.athletelink.gameservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.dto.kafka.UserChange;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings(value = {"unused"})
public class KafkaConsumerService {
    private final PlayerProfileService playerProfileService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-changes",
            groupId = "@{spring.application.name}"
    )
    public void receiveUserChanges(ConsumerRecord<UUID, UserChange> record) {
        UUID userId = record.value().userId();

        switch (record.value().userChangeType()) {
            case CREATE -> playerProfileService.createProfile(userId);
            case DELETE -> playerProfileService.deleteProfile(userId);
        }
    }
}
