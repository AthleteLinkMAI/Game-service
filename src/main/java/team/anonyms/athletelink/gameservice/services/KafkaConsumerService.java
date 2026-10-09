package team.anonyms.athletelink.gameservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.dto.SubmitRoundResultDto;
import team.anonyms.athletelink.gameservice.dto.UserChange;

@Service
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final PlayerProfileService playerProfileService;
    private final RoundResultService roundResultService;

    public KafkaConsumerService(
            ObjectMapper objectMapper,
            PlayerProfileService playerProfileService,
            RoundResultService roundResultService
    ) {
        this.objectMapper = objectMapper;
        this.playerProfileService = playerProfileService;
        this.roundResultService = roundResultService;
    }

    @KafkaListener(topics = "${app.kafka.topics.user-changes}", groupId = "${spring.application.name}")
    public void receiveUserChange(String json) {
        UserChange change = parse(json, UserChange.class);
        if (change.userId() == null || change.userChangeType() == null) {
            throw new IllegalArgumentException("Invalid Auth user change event");
        }
        switch (change.userChangeType()) {
            case CREATE -> playerProfileService.createProfile(change.userId());
            case DELETE -> playerProfileService.deleteProfile(change.userId());
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.game-commands}", groupId = "${spring.application.name}")
    public void receiveRoundResult(String json) {
        roundResultService.submitRoundResult(parse(json, SubmitRoundResultDto.class));
    }

    private <T> T parse(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Kafka event payload", exception);
        }
    }
}
