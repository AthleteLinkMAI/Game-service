package team.anonyms.athletelink.gameservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka.topics")
public record KafkaTopicsProperties(
        String userChanges,
        String gameSubmitRounds,
        String gameCloseRequests,
        String gameReplies
) {
}
