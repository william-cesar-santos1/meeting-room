package br.com.ada.classes.meetingroom.resource.reservation;

import br.com.ada.classes.meetingroom.model.Reservation;
import br.com.ada.classes.meetingroom.resource.room.RoomResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
        Long id,
        RoomResponse room,
        String guestName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String createdBy       // username de quem criou a reserva
) {

    public ReservationResponse(Reservation reservation) {
        this(
                reservation.id,
                new RoomResponse(
                        reservation.getRoom().id,
                        reservation.getRoom().getName(),
                        reservation.getRoom().getCapacity(),
                        null, null, null, null
                ),
                reservation.getGuestName(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getCreatedBy().getUsername()
        );
    }
}
