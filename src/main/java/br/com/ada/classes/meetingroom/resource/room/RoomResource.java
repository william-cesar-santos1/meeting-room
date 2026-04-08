package br.com.ada.classes.meetingroom.resource.room;

import br.com.ada.classes.meetingroom.model.Room;
import br.com.ada.classes.meetingroom.service.ReservationService;
import br.com.ada.classes.meetingroom.service.RoomService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

@Path("/rooms")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Inject
    RoomService roomService;

    @Inject
    ReservationService reservationService;

    @GET
    public List<Room> list(@QueryParam("minCapacity") Integer minCapacity) {
        return roomService.list(minCapacity);
    }

    @GET
    @Path("/{id}")
    public Room findById(@PathParam("id") Long id) {
        return roomService.findById(id);
    }

    @POST
    @Transactional
    public Response create(@Valid CreateRoomRequest request, @Context UriInfo uriInfo) {
        Room room = roomService.create(request);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId().toString())
                .build();

        return Response.created(location)
                .entity(room)
                .build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Room update(@PathParam("id") Long id, @Valid UpdateRoomRequest request) {
        return roomService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        roomService.delete(id);
        reservationService.deleteByRoomId(id);
        return Response.status(Status.NO_CONTENT).build();
    }
}

