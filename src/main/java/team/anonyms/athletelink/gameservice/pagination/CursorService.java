package team.anonyms.athletelink.gameservice.pagination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import team.anonyms.athletelink.gameservice.exceptions.InvalidCursorException;

import java.util.Base64;

@Service
public class CursorService {

    private final ObjectMapper objectMapper;

    public CursorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(Object value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode cursor", exception);
        }
    }

    public <T> T decode(String cursor, Class<T> type) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            T value = objectMapper.readValue(decoded, type);
            if (value == null) {
                throw new InvalidCursorException("Invalid cursor");
            }
            return value;
        } catch (Exception exception) {
            if (exception instanceof InvalidCursorException invalidCursorException) {
                throw invalidCursorException;
            }
            throw new InvalidCursorException("Invalid cursor", exception);
        }
    }
}
