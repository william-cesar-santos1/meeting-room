package br.com.ada.classes.meetingroom.resource.reservation;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long roomId,
        String guestName,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}

