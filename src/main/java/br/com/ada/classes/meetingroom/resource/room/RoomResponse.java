package br.com.ada.classes.meetingroom.resource.room;

import br.com.ada.classes.meetingroom.model.Room;
import br.com.ada.classes.meetingroom.resource.Link;
import br.com.ada.classes.meetingroom.resource.reservation.ReservationResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.core.UriInfo;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomResponse(
        Long id,
        String name,
        Integer capacity,
        Double hoursUsed,
        Double hoursReserved,
        List<ReservationResponse> nextReservations,
        List<Link> links
) {

    private static final Long LIMIT_SHOW_RESERVATIONS = 3L;

    public RoomResponse(Room room, List<ReservationResponse> nextReservations, List<Link> links) {
        this(room.id, room.getName(), room.getCapacity(),
                room.getHoursUsed(), room.getHoursReserved(),
                nextReservations, links);
    }

    public static List<Link> links(Room room, UriInfo uriInfo) {
        return List.of(
                new Link("self",
                        uriInfo.getBaseUriBuilder().path("rooms").path(room.id.toString()).build().toString(),
                        "GET"),
                new Link("reservations",
                        uriInfo.getBaseUriBuilder().path("reservations").queryParam("roomId", room.id).build().toString(),
                        "GET")
        );
    }

    public static List<ReservationResponse> reservations(Room room) {
        return room.getReservations()
                .stream()
                .filter(it -> it.getStartAt().isAfter(LocalDateTime.now()))
                .limit(LIMIT_SHOW_RESERVATIONS)
                .map(it -> new ReservationResponse(
                        it.id,
                        null,
                        it.getGuestName(),
                        it.getStartAt(),
                        it.getEndAt()
                )).toList();
    }

}
