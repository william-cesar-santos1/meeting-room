package br.com.ada.classes.meetingroom.resource.reservation;

import br.com.ada.classes.meetingroom.model.Reservation;
import br.com.ada.classes.meetingroom.resource.PageResponse;
import br.com.ada.classes.meetingroom.service.ReservationService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.LocalDate;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    private static final Logger LOG = Logger.getLogger(ReservationResource.class);

    @Inject
    ReservationService reservationService;

    @GET
    @PermitAll
    @Timed(value = "reservations.list.time", description = "Tempo de processamento da listagem de reservas")
    @Counted(value = "reservations.list.count", description = "Número de consultas de listagem de reservas")
    public PageResponse<ReservationResponse> list(
            @QueryParam("roomId") Long roomId,
            @QueryParam("date") LocalDate date,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        LOG.infof("GET /reservations - roomId=%s, date=%s, page=%d, size=%d", roomId, date, page, size);
        if (date != null) {
            LOG.debugf("Buscando reservas pela data: %s", date);
            return PageResponse.from(
                    reservationService.findByDate(date, page, size),
                    this::toResponse
            );
        }
        LOG.debug("Listando reservas");
        return PageResponse.from(
                reservationService.list(roomId, page, size),
                this::toResponse);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Timed(value = "reservations.findById.time", description = "Tempo de processamento da busca de reserva por ID")
    public ReservationResponse findById(@PathParam("id") Long id) {
        LOG.infof("GET /reservations/%d", id);
        return toResponse(reservationService.findById(id));
    }

    @POST
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    @Timed(value = "reservations.create.time", description = "Tempo de processamento da criação de reserva")
    public Response create(
            @Valid CreateReservationRequest request,
            @Context UriInfo uriInfo
    ) {
        LOG.infof("POST /reservations - roomId=%d, guestName='%s', startAt=%s, endAt=%s",
                request.roomId(), request.guestName(), request.startAt(), request.endAt());
        Reservation reservation = reservationService.create(request);
        LOG.infof("Reserva criada com id=%d", reservation.id);
        URI location = uriInfo.getAbsolutePathBuilder().path(reservation.id.toString()).build();
        return Response.created(location).entity(toResponse(reservation)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    @Timed(value = "reservations.update.time", description = "Tempo de processamento da atualização de reserva")
    public ReservationResponse update(
            @PathParam("id") Long id,
            @Valid UpdateReservationRequest request
    ) {
        LOG.infof("PUT /reservations/%d - roomId=%d, guestName='%s'", id, request.roomId(), request.guestName());
        return toResponse(reservationService.update(id, request));
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    @Timed(value = "reservations.delete.time", description = "Tempo de processamento da exclusão de reserva")
    public Response delete(@PathParam("id") Long id) {
        LOG.infof("DELETE /reservations/%d", id);
        reservationService.delete(id);
        LOG.debugf("Reserva id=%d excluída com sucesso", id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation);
    }
}
