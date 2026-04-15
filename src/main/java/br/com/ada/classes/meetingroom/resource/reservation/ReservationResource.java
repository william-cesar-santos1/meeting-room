package br.com.ada.classes.meetingroom.resource.reservation;

import br.com.ada.classes.meetingroom.model.Reservation;
import br.com.ada.classes.meetingroom.resource.PageResponse;
import br.com.ada.classes.meetingroom.service.ReservationService;
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

import java.net.URI;
import java.time.LocalDate;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    @Inject
    ReservationService reservationService;

    @GET
    @PermitAll
    public PageResponse<ReservationResponse> list(
            @QueryParam("roomId") Long roomId,
            @QueryParam("date") LocalDate date,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        if (date != null) {
            return PageResponse.from(
                    reservationService.findByDate(date, page, size),
                    this::toResponse
            );
        }
        return PageResponse.from(
                reservationService.list(roomId, page, size),
                this::toResponse);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public ReservationResponse findById(@PathParam("id") Long id) {
        return toResponse(reservationService.findById(id));
    }

    @POST
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    public Response create(
            @Valid CreateReservationRequest request,
            @Context UriInfo uriInfo
    ) {
        Reservation reservation = reservationService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(reservation.id.toString()).build();
        return Response.created(location).entity(toResponse(reservation)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    public ReservationResponse update(
            @PathParam("id") Long id,
            @Valid UpdateReservationRequest request
    ) {
        return toResponse(reservationService.update(id, request));
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    public Response delete(@PathParam("id") Long id) {
        reservationService.delete(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation);
    }
}
