package team.anonyms.athletelink.gameservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.dto.CloseRequestDto;
import team.anonyms.athletelink.gameservice.dto.SubmitRoundResultDto;
import team.anonyms.athletelink.gameservice.dto.UserChange;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final PlayerProfileService playerProfileService;
    private final RoundResultService roundResultService;

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

    @KafkaListener(topics = "${app.kafka.topics.game-submit-rounds}", groupId = "${spring.application.name}")
    public void receiveRoundResult(String json) {
        log.info("Invoked json: " + json);
        SubmitRoundResultDto dto = parse(json, SubmitRoundResultDto.class);
        log.info("Parsed dto = " + dto.toString());

        roundResultService.submitRoundResult(dto);
    }

    @KafkaListener(topics = "${app.kafka.topics.game-close-requests}", groupId = "${spring.application.name}")
    public void validateGameForClosing(String json) {
        log.info("Invoked json: " + json);
        CloseRequestDto dto = parse(json, CloseRequestDto.class);
        log.info("Parsed dto = " + dto.toString());

        roundResultService.validateGameForClosing(dto);
    }

    private <T> T parse(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid Kafka event payload", exception);
        }
    }
}
