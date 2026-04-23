package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.exception.AccessDeniedException;
import br.com.ada.classes.meetingroom.exception.BusinessException;
import br.com.ada.classes.meetingroom.exception.ResourceNotFoundException;
import br.com.ada.classes.meetingroom.integration.holiday.HolidayService;
import br.com.ada.classes.meetingroom.model.*;
import br.com.ada.classes.meetingroom.resource.reservation.CreateReservationRequest;
import br.com.ada.classes.meetingroom.resource.reservation.UpdateReservationRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
            Span.current().setAttribute("reservation.access.denied", true);
            Span.current().setAttribute("reservation.owner.id", ownerId);
            Span.current().setStatus(StatusCode.ERROR, "acesso negado: usuario nao e o dono da reserva");
            throw new AccessDeniedException(
                    "Acesso negado: apenas o criador da reserva ou um ADMIN pode executar esta acao"
            );
        }
    }

    @WithSpan("ReservationService.list")
    public PageResult<Reservation> list(
            @SpanAttribute("reservation.filter.roomId") Long roomId,
            @SpanAttribute("reservation.page") Integer page,
            @SpanAttribute("reservation.size") Integer size) {
        LOG.debugf("Consultando reservas - roomId=%s, page=%d, size=%d", roomId, page, size);
        var query = (roomId != null)
                ? Reservation.find("room.id = ?1", Sort.by("startAt").and("id"), roomId)
                : Reservation.findAll(Sort.by("startAt").and("id"));
        var result = query.page(Page.of(page, size));
        Span.current().setAttribute("reservation.result.count", result.list().size());
        LOG.debugf("Consulta de reservas retornou %d registros (total=%d)", result.list().size(), result.count());
        return new PageResult<>(result.list(), page, size, result.count());
    }

    @WithSpan("ReservationService.findById")
    public Reservation findById(@SpanAttribute("reservation.id") Long id) {
        LOG.debugf("Buscando reserva por id=%d", id);
        Reservation reservation = Reservation.findById(id);
        if (reservation == null) {
            LOG.warnf("Reserva não encontrada: id=%d", id);
            throw ResourceNotFoundException.ofId("Reservation", id);
        }
        return reservation;
    }

    @WithSpan("ReservationService.create")
    public Reservation create(CreateReservationRequest request) {
        LOG.infof("Criando reserva - roomId=%d, guestName='%s', startAt=%s, endAt=%s",
                request.roomId(), request.guestName(), request.startAt(), request.endAt());
        Span.current().setAttribute("reservation.room.id", request.roomId());
        Span.current().setAttribute("reservation.guest.name", request.guestName());
        Span.current().setAttribute("reservation.startAt", request.startAt().toString());
        Span.current().setAttribute("reservation.endAt", request.endAt().toString());

        Room room = roomService.getRequiredRoom(request.roomId());
        holidayService.validateDate(request.startAt().toLocalDate());
        validateReservation(request.guestName(), request.startAt(), request.endAt());

        User owner = User.findById(loggedUser().id());
        Span.current().setAttribute("reservation.createdBy.id", owner.id);

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setGuestName(request.guestName().trim());
        reservation.setStartAt(request.startAt());
        reservation.setEndAt(request.endAt());
        reservation.setCreatedBy(owner);
        reservation.persist();
        Span.current().setAttribute("reservation.id", reservation.id);
        LOG.infof("Reserva persistida com id=%d para sala id=%d", reservation.id, room.id);
        return reservation;
    }

    @WithSpan("ReservationService.update")
    public Reservation update(@SpanAttribute("reservation.id") Long id, UpdateReservationRequest request) {
        LOG.infof("Atualizando reserva id=%d - roomId=%d, guestName='%s'", id, request.roomId(), request.guestName());
        Span.current().setAttribute("reservation.room.id", request.roomId());
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

    @WithSpan("ReservationService.delete")
    public void delete(@SpanAttribute("reservation.id") Long id) {
        LOG.infof("Excluindo reserva id=%d", id);
        Reservation reservation = findById(id);
        Span.current().setAttribute("reservation.room.id", reservation.getRoom().id);
        Span.current().setAttribute("reservation.guest.name", reservation.getGuestName());
        Span.current().setAttribute("reservation.createdBy.id", reservation.getCreatedBy().id);
        checkOwnership(reservation);
        reservation.delete();
        LOG.infof("Reserva id=%d excluída do banco", id);
    }

    @WithSpan("ReservationService.findByDate")
    public PageResult<Reservation> findByDate(
            @SpanAttribute("reservation.filter.date") LocalDate date,
            @SpanAttribute("reservation.page") int page,
            @SpanAttribute("reservation.size") int size) {
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
        Span.current().setAttribute("reservation.result.count", reservations.size());
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

