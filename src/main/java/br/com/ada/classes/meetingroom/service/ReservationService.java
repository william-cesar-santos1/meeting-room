package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.exception.AccessDeniedException;
import br.com.ada.classes.meetingroom.exception.BusinessException;
import br.com.ada.classes.meetingroom.exception.ResourceNotFoundException;
import br.com.ada.classes.meetingroom.integration.holiday.HolidayService;
import br.com.ada.classes.meetingroom.model.*;
import br.com.ada.classes.meetingroom.resource.reservation.CreateReservationRequest;
import br.com.ada.classes.meetingroom.resource.reservation.UpdateReservationRequest;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
public class ReservationService {

    private static final Logger LOG = Logger.getLogger(ReservationService.class);

    @Inject
    RoomService roomService;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    HolidayService holidayService;

    private LoggedUser loggedUser() {
        return currentUserService.getLoggedUser();
    }

    private void checkOwnership(Reservation reservation) {
        if (loggedUser().isAdmin()) return;

        Long ownerId = reservation.getCreatedBy().id;
        Long currentId = loggedUser().id();

        if (!currentId.equals(ownerId)) {
            LOG.warnf("Acesso negado à reserva id=%d: usuário id=%d não é o dono (dono id=%d)",
                    reservation.id, currentId, ownerId);
            throw new AccessDeniedException(
                    "Acesso negado: apenas o criador da reserva ou um ADMIN pode executar esta acao"
            );
        }
    }

    public PageResult<Reservation> list(Long roomId, Integer page, Integer size) {
        LOG.debugf("Consultando reservas - roomId=%s, page=%d, size=%d", roomId, page, size);
        var query = (roomId != null)
                ? Reservation.find("room.id = ?1", Sort.by("startAt").and("id"), roomId)
                : Reservation.findAll(Sort.by("startAt").and("id"));
        var result = query.page(Page.of(page, size));
        LOG.debugf("Consulta de reservas retornou %d registros (total=%d)", result.list().size(), result.count());
        return new PageResult<>(result.list(), page, size, result.count());
    }

    public Reservation findById(Long id) {
        LOG.debugf("Buscando reserva por id=%d", id);
        Reservation reservation = Reservation.findById(id);
        if (reservation == null) {
            LOG.warnf("Reserva não encontrada: id=%d", id);
            throw ResourceNotFoundException.ofId("Reservation", id);
        }
        return reservation;
    }

    public Reservation create(CreateReservationRequest request) {
        LOG.infof("Criando reserva - roomId=%d, guestName='%s', startAt=%s, endAt=%s",
                request.roomId(), request.guestName(), request.startAt(), request.endAt());
        Room room = roomService.getRequiredRoom(request.roomId());
        holidayService.validateDate(request.startAt().toLocalDate());
        validateReservation(request.guestName(), request.startAt(), request.endAt());

        User owner = User.findById(loggedUser().id());

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setGuestName(request.guestName().trim());
        reservation.setStartAt(request.startAt());
        reservation.setEndAt(request.endAt());
        reservation.setCreatedBy(owner);
        reservation.persist();
        LOG.infof("Reserva persistida com id=%d para sala id=%d", reservation.id, room.id);
        return reservation;
    }

    public Reservation update(Long id, UpdateReservationRequest request) {
        LOG.infof("Atualizando reserva id=%d - roomId=%d, guestName='%s'", id, request.roomId(), request.guestName());
        Reservation reservation = findById(id);
        checkOwnership(reservation);

        Room room = roomService.getRequiredRoom(request.roomId());
        holidayService.validateDate(request.startAt().toLocalDate());
        validateReservation(request.guestName(), request.startAt(), request.endAt());

        reservation.setRoom(room);
        reservation.setGuestName(request.guestName().trim());
        reservation.setStartAt(request.startAt());
        reservation.setEndAt(request.endAt());
        LOG.debugf("Reserva id=%d atualizada com sucesso", id);
        return reservation;
    }

    public void delete(Long id) {
        LOG.infof("Excluindo reserva id=%d", id);
        Reservation reservation = findById(id);
        checkOwnership(reservation);
        reservation.delete();
        LOG.infof("Reserva id=%d excluída do banco", id);
    }

    public PageResult<Reservation> findByDate(LocalDate date, int page, int size) {
        if (date == null) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        LOG.debugf("Consultando reservas pela data %s, page=%d, size=%d", date, page, size);
        var query = Reservation.find(
                "startAt <= ?1 AND endAt >= ?2",
                Sort.by("startAt"),
                date.atTime(LocalTime.MAX),
                date.atTime(LocalTime.MIN)
        );
        long total = query.count();
        List<Reservation> reservations = query.page(Page.of(page, size)).list();
        LOG.debugf("Consulta por data retornou %d registros (total=%d)", reservations.size(), total);
        return new PageResult<>(reservations, page, size, total);
    }

    private void validateReservation(String guestName, LocalDateTime startAt, LocalDateTime endAt) {
        if (guestName == null || guestName.isBlank()) {
            LOG.debug("Validação falhou: nome do convidado em branco");
            throw new BusinessException("Guest name is required");
        }
        if (!startAt.isBefore(endAt)) {
            LOG.debugf("Validação falhou: startAt=%s não é anterior a endAt=%s", startAt, endAt);
            throw new BusinessException("Start date/time must be before end date/time");
        }
    }
}
