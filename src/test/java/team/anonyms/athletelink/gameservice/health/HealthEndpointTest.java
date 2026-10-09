package team.anonyms.athletelink.gameservice.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = HealthEndpointTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class HealthEndpointTest {

    private static final AtomicBoolean DATABASE_UP = new AtomicBoolean(true);

    @Value("${local.server.port}")
    private int port;

    @Test
    void healthzReportsServiceAndDatabaseAvailability() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        try {
            DATABASE_UP.set(true);
            HttpResponse<String> healthy = getHealth(client);
            assertEquals(200, healthy.statusCode());
            assertTrue(healthy.body().contains("\"status\":\"UP\""));

            DATABASE_UP.set(false);
            HttpResponse<String> unhealthy = getHealth(client);
            assertEquals(503, unhealthy.statusCode());
            assertTrue(unhealthy.body().contains("\"status\":\"DOWN\""));
        } finally {
            DATABASE_UP.set(true);
        }
    }

    private HttpResponse<String> getHealth(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/healthz"))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    static class TestApplication {

        @Bean
        HealthIndicator dbHealthIndicator() {
            return () -> DATABASE_UP.get() ? Health.up().build() : Health.down().build();
        }
    }
}
