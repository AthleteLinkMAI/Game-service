package team.anonyms.athletelink.gameservice.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;
import team.anonyms.athletelink.gameservice.controllers.RankTierController;
import team.anonyms.athletelink.gameservice.exceptions.GlobalExceptionHandler;
import team.anonyms.athletelink.gameservice.exceptions.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StructuredLoggingTest {

    @Test
    void controllerLogIncludesStableApiRoute() {
        ILoggingEvent event = capture(RankTierController.class,
                () -> new RankTierController().getRankTiers());

        assertEquals("API endpoint is called", event.getFormattedMessage());
        assertEquals("GET /api/v1/rank-tiers", apiEndpoint(event));
    }

    @Test
    void exceptionLogUsesRouteTemplateInsteadOfUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/players/123e4567-e89b-12d3-a456-426614174000");
        request.setServletPath("/api/v1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/players/{userId}");

        ILoggingEvent event = capture(GlobalExceptionHandler.class, () -> {
            var response = new GlobalExceptionHandler().handleResourceNotFound(
                    new ResourceNotFoundException("Player profile not found"), request);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
        });

        assertEquals("Player profile not found", event.getFormattedMessage());
        assertEquals("GET /api/v1/players/{userId}", apiEndpoint(event));
    }

    private ILoggingEvent capture(Class<?> source, Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(source);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
            assertEquals(1, appender.list.size());
            return appender.list.getFirst();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private Object apiEndpoint(ILoggingEvent event) {
        assertNotNull(event.getKeyValuePairs());
        return event.getKeyValuePairs().stream()
                .filter(pair -> "API endpoint".equals(pair.key))
                .map(pair -> pair.value)
                .findFirst()
                .orElse(null);
    }
}
