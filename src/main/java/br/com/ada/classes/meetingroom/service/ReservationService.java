package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.exception.AccessDeniedException;
import br.com.ada.classes.meetingroom.exception.BusinessException;
import br.com.ada.classes.meetingroom.exception.ResourceNotFoundException;
import br.com.ada.classes.meetingroom.model.*;
import br.com.ada.classes.meetingroom.resource.reservation.CreateReservationRequest;
import br.com.ada.classes.meetingroom.resource.reservation.UpdateReservationRequest;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
public class ReservationService {

    @Inject
    RoomService roomService;

    @Inject
    CurrentUserService currentUserService;

    private LoggedUser loggedUser() {
        return currentUserService.getLoggedUser();
    }

    private void checkOwnership(Reservation reservation) {
        if (loggedUser().isAdmin()) return;

        Long ownerId = reservation.getCreatedBy().id;
        Long currentId = loggedUser().id();

        if (!currentId.equals(ownerId)) {
            throw new AccessDeniedException(
                    "Acesso negado: apenas o criador da reserva ou um ADMIN pode executar esta acao"
            );
        }
    }

    public PageResult<Reservation> list(Long roomId, int page, int size) {
        var query = (roomId != null)
                ? Reservation.find("room.id = ?1", Sort.by("startAt").and("id"), roomId)
                : Reservation.findAll(Sort.by("startAt").and("id"));
        var result = query.page(Page.of(page, size));
        return new PageResult<>(result.list(), page, size, result.count());
    }

    public Reservation findById(Long id) {
        Reservation reservation = Reservation.findById(id);
        if (reservation == null) {
            throw ResourceNotFoundException.ofId("Reservation", id);
        }
        return reservation;
    }

    public Reservation create(CreateReservationRequest request) {
        Room room = roomService.getRequiredRoom(request.roomId());
        validateReservation(request.guestName(), request.startAt(), request.endAt());

        User owner = User.findById(loggedUser().id());

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setGuestName(request.guestName().trim());
        reservation.setStartAt(request.startAt());
        reservation.setEndAt(request.endAt());
        reservation.setCreatedBy(owner);
        reservation.persist();
        return reservation;
    }

    public Reservation update(Long id, UpdateReservationRequest request) {
        Reservation reservation = findById(id);
        checkOwnership(reservation);

        Room room = roomService.getRequiredRoom(request.roomId());
        validateReservation(request.guestName(), request.startAt(), request.endAt());

        reservation.setRoom(room);
        reservation.setGuestName(request.guestName().trim());
        reservation.setStartAt(request.startAt());
        reservation.setEndAt(request.endAt());
        return reservation;
    }

    public void delete(Long id) {
        Reservation reservation = findById(id);
        checkOwnership(reservation);
        reservation.delete();
    }

    public PageResult<Reservation> findByDate(LocalDate date, int page, int size) {
        if (date == null) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        var query = Reservation.find(
                "startAt <= ?1 AND endAt >= ?2",
                Sort.by("startAt"),
                date.atTime(LocalTime.MAX),
                date.atTime(LocalTime.MIN)
        );
        long total = query.count();
        List<Reservation> reservations = query.page(Page.of(page, size)).list();
        return new PageResult<>(reservations, page, size, total);
    }

    private void validateReservation(String guestName, LocalDateTime startAt, LocalDateTime endAt) {
        if (guestName == null || guestName.isBlank()) {
            throw new BusinessException("Guest name is required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new BusinessException("Start date/time must be before end date/time");
        }
    }
}
