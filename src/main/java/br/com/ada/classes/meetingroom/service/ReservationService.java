package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.model.Reservation;
import br.com.ada.classes.meetingroom.resource.reservation.CreateReservationRequest;
import br.com.ada.classes.meetingroom.resource.reservation.UpdateReservationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class ReservationService {

    @Inject
    RoomService roomService;

    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Reservation> list(Long roomId) {
        return reservations.values().stream()
                .filter(reservation -> roomId == null || reservation.getRoomId().equals(roomId))
                .sorted(Comparator.comparing(Reservation::getStartAt)
                        .thenComparing(Reservation::getId))
                .map(this::copy)
                .toList();
    }

    public Reservation findById(Long id) {
        return copy(getRequiredReservation(id));
    }

    public Reservation create(CreateReservationRequest request) {
        validateRequest(request.roomId(), request.guestName(), request.startAt(), request.endAt());

        long id = sequence.incrementAndGet();
        Reservation reservation = new Reservation(
                id,
                request.roomId(),
                request.guestName().trim(),
                request.startAt(),
                request.endAt()
        );

        reservations.put(id, reservation);
        return copy(reservation);
    }

    public Reservation update(Long id, UpdateReservationRequest request) {
        Reservation existingReservation = getRequiredReservation(id);
        validateRequest(request.roomId(), request.guestName(), request.startAt(), request.endAt());

        existingReservation.setRoomId(request.roomId());
        existingReservation.setGuestName(request.guestName().trim());
        existingReservation.setStartAt(request.startAt());
        existingReservation.setEndAt(request.endAt());
        return copy(existingReservation);
    }

    public void delete(Long id) {
        Reservation removedReservation = reservations.remove(id);
        if (removedReservation == null) {
            throw new NotFoundException("Reservation with id " + id + " was not found");
        }
    }

    public void deleteByRoomId(Long roomId) {
        reservations.values().removeIf(reservation -> reservation.getRoomId().equals(roomId));
    }

    public List<Reservation> findByDate(LocalDateTime date) {
        if (date == null) {
            return List.of();
        }
        return reservations.values().stream()
                .filter(r -> !r.getStartAt().isAfter(date) && !r.getEndAt().isBefore(date))
                .sorted(Comparator.comparing(Reservation::getStartAt))
                .map(this::copy)
                .toList();
    }

    private Reservation getRequiredReservation(Long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new NotFoundException("Reservation with id " + id + " was not found");
        }
        return reservation;
    }

    private void validateRequest(Long roomId, String guestName, LocalDateTime startAt, LocalDateTime endAt) {
        roomService.getRequiredRoom(roomId);

        if (guestName == null || guestName.isBlank()) {
            throw new BadRequestException("Guest name is required");
        }

        if (!startAt.isBefore(endAt)) {
            throw new BadRequestException("Start date/time must be before end date/time");
        }
    }

    private Reservation copy(Reservation reservation) {
        return new Reservation(
                reservation.getId(),
                reservation.getRoomId(),
                reservation.getGuestName(),
                reservation.getStartAt(),
                reservation.getEndAt()
        );
    }
}

