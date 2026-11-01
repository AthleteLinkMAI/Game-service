package team.anonyms.athletelink.gameservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Game Service API",
        version = "v1",
        description = "Game-owned results and player statistics. Endpoints tagged '(draft)' describe planned HTTP contracts; "
                + "their handlers are not implemented yet.\n\n"
                + "Planned Kafka handoff from Request Service: `JOIN_REQUESTED` on `game.join.requests` carries "
                + "eventId, requestId, userId and organizerId. `ACTIVITY_REQUEST_STARTED` on "
                + "`activity.requests.changes` carries requestId, the sport name as sportId, numberOfRounds, "
                + "organizerId and the accepted participant roster.\n\n"
                + "Game will publish `JOIN_REQUEST_RESOLVED` to `game.replies` with the original join attempt "
                + "eventId, requestId, userId and accepted flag. After the final round, Game will publish "
                + "`REQUEST_STATUS_CHANGED` with a new eventId, requestId and status COMPLETED."
))
public class Main {
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
