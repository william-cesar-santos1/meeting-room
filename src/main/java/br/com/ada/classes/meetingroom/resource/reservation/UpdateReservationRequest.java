package br.com.ada.classes.meetingroom.resource.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record UpdateReservationRequest(
        @NotNull(message = "Room id is required")
        Long roomId,

        @NotBlank(message = "Guest name is required")
        String guestName,

        @NotNull(message = "Start date/time is required")
        LocalDateTime startAt,

        @NotNull(message = "End date/time is required")
        LocalDateTime endAt
) {
}

