package team.anonyms.athletelink.gameservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.config.KafkaTopicsProperties;
import team.anonyms.athletelink.gameservice.dto.kafka.GameReply;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<UUID, Object> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    public void sendGameReplies(GameReply gameReply) {
        kafkaTemplate.send(topics.gameReplies(), gameReply.requestId(), gameReply);
    }
}
