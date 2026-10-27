package team.anonyms.athletelink.gameservice.dto;

import java.util.List;

public record PageDto<T>(
        List<T> items,
        int page,
        int cntPerPage
) {
}
