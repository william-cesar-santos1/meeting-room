package br.com.ada.classes.meetingroom.resource.reservation;

import br.com.ada.classes.meetingroom.model.Reservation;
import br.com.ada.classes.meetingroom.service.ReservationService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    @Inject
    ReservationService reservationService;

    @GET
    public List<Reservation> list(
            @QueryParam("roomId") Long roomId,
            @QueryParam("date") String dateStr) {
        if (dateStr != null && !dateStr.isBlank()) {
            LocalDateTime date = LocalDateTime.parse(dateStr);
            return reservationService.findByDate(date);
        }
        return reservationService.list(roomId);
    }

    @GET
    @Path("/{id}")
    public Reservation findById(@PathParam("id") Long id) {
        return reservationService.findById(id);
    }

    @POST
    @Transactional
    public Response create(@Valid CreateReservationRequest request, @Context UriInfo uriInfo) {
        Reservation reservation = reservationService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(reservation.getId().toString()).build();
        return Response.created(location).entity(reservation).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Reservation update(@PathParam("id") Long id, @Valid UpdateReservationRequest request) {
        return reservationService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        reservationService.delete(id);
        return Response.status(Status.NO_CONTENT).build();
    }
}

