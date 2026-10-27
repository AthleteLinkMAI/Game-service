package team.anonyms.athletelink.gameservice.dto;

import java.util.List;

public record CursorPageDto<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext
) {
}