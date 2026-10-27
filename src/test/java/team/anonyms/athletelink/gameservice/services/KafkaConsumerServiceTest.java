package team.anonyms.athletelink.gameservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.gameservice.dto.SubmitRoundResultDto;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @Mock PlayerProfileService playerProfileService;
    @Mock RoundResultService roundResultService;

    @Test
    void authEventsCreateAndDeleteProfiles() {
        KafkaConsumerService consumer = consumer();
        UUID userId = UUID.randomUUID();

        consumer.receiveUserChange("{\"userChangeType\":\"CREATE\",\"userId\":\"" + userId + "\"}");
        consumer.receiveUserChange("{\"userChangeType\":\"DELETE\",\"userId\":\"" + userId + "\"}");

        verify(playerProfileService).createProfile(userId);
        verify(playerProfileService).deleteProfile(userId);
    }

    @Test
    void coreEventProvidesRequestSportRoundAndPlayerOutcomes() {
        KafkaConsumerService consumer = consumer();
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String json = """
                {"type":"ROUND_RESULT","eventId":"%s","requestId":"%s",
                 "sportId":7,"roundNumber":2,"recordedBy":"%s",
                 "players":[{"userId":"%s","outcome":"WIN"}]}
                """.formatted(UUID.randomUUID(), requestId, UUID.randomUUID(), userId);

        consumer.receiveRoundResult(json);

        var captor = org.mockito.ArgumentCaptor.forClass(SubmitRoundResultDto.class);
        verify(roundResultService).submitRoundResult(captor.capture());
        assertEquals(requestId, captor.getValue().requestId());
        assertEquals(2, captor.getValue().roundNumber());
        assertEquals(userId, captor.getValue().players().getFirst().userId());
    }

    @Test
    void coreEventAlsoAcceptsSnakeCaseRoundNumber() {
        KafkaConsumerService consumer = consumer();
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String json = """
                {"type":"ROUND_RESULT","eventId":"%s","requestId":"%s",
                 "sportId":7,"round_number":3,"recordedBy":"%s",
                 "players":[{"userId":"%s","outcome":"LOSS"}]}
                """.formatted(UUID.randomUUID(), requestId, UUID.randomUUID(), userId);

        consumer.receiveRoundResult(json);

        var captor = org.mockito.ArgumentCaptor.forClass(SubmitRoundResultDto.class);
        verify(roundResultService).submitRoundResult(captor.capture());
        assertEquals(3, captor.getValue().roundNumber());
    }

    private KafkaConsumerService consumer() {
        return new KafkaConsumerService(new ObjectMapper(), playerProfileService, roundResultService);
    }
}
