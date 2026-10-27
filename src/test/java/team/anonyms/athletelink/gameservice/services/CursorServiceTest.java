package team.anonyms.athletelink.gameservice.services;

import org.junit.jupiter.api.Test;
import team.anonyms.athletelink.gameservice.pagination.CursorJacksonConfig;
import team.anonyms.athletelink.gameservice.pagination.CursorService;
import team.anonyms.athletelink.gameservice.pagination.RoundHistoryCursor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CursorServiceTest {

    @Test
    void roundHistoryCursorSurvivesEncodeDecode() {
        CursorService service = new CursorService(new CursorJacksonConfig().cursorObjectMapper());
        RoundHistoryCursor original = new RoundHistoryCursor(
                "round-history", UUID.randomUUID(), 7L,
                Instant.parse("2026-09-30T10:00:00Z"), UUID.randomUUID());

        assertEquals(original,
                service.decode(service.encode(original), RoundHistoryCursor.class));
    }
}
