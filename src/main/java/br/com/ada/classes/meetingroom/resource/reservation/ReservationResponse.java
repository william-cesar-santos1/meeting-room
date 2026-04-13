package br.com.ada.classes.meetingroom.resource.reservation;

import br.com.ada.classes.meetingroom.model.Reservation;
import br.com.ada.classes.meetingroom.resource.Link;
import br.com.ada.classes.meetingroom.resource.room.RoomResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
        Long id,
        RoomResponse room,
        String guestName,
        LocalDateTime startAt,
        LocalDateTime endAt
) {

    public ReservationResponse(Reservation reservation) {
        var roomResponse = new RoomResponse(
                reservation.getRoom().id,
                reservation.getRoom().getName(),
                reservation.getRoom().getCapacity(),
                null,
                null,
                null,
                null
        );
        this(reservation.id,
                roomResponse,
                reservation.getGuestName(),
                reservation.getStartAt(),
                reservation.getEndAt());
    }

}
