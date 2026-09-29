package team.anonyms.athletelink.gameservice.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CursorJacksonConfig {

    @Bean
    public ObjectMapper cursorObjectMapper() {
        return new ObjectMapper();
    }
}
