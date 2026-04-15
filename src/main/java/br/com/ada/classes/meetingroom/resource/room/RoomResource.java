package br.com.ada.classes.meetingroom.resource.room;

import br.com.ada.classes.meetingroom.model.Room;
import br.com.ada.classes.meetingroom.resource.PageResponse;
import br.com.ada.classes.meetingroom.service.RoomService;
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

import static br.com.ada.classes.meetingroom.resource.room.RoomResponse.links;
import static br.com.ada.classes.meetingroom.resource.room.RoomResponse.reservations;

@Path("/rooms")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Inject
    RoomService roomService;

    @GET
    @PermitAll
    public PageResponse<RoomResponse> list(
            @QueryParam("minCapacity") Integer minCapacity,
            @QueryParam("name") String name,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @Context UriInfo uriInfo) {
        if (name != null && !name.isBlank()) {
            return PageResponse.from(
                    roomService.findByName(name, page, size),
                    it -> toResponse(it, uriInfo)
            );
        }
        return PageResponse.from(
                roomService.list(minCapacity, page, size),
                it -> toResponse(it, uriInfo)
        );
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public RoomResponse findById(
            @PathParam("id") Long id,
            @Context UriInfo uriInfo
    ) {
        return toResponse(roomService.findById(id), uriInfo);
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public Response create(
            @Valid CreateRoomRequest request,
            @Context UriInfo uriInfo
    ) {
        Room room = roomService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.id.toString())
                .build();
        return Response.created(location)
                .entity(toResponse(room, uriInfo))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public RoomResponse update(
            @PathParam("id") Long id,
            @Valid UpdateRoomRequest request,
            @Context UriInfo uriInfo
    ) {
        return toResponse(roomService.update(id, request), uriInfo);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Long id) {
        roomService.delete(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private RoomResponse toResponse(Room room, UriInfo uriInfo) {
        return new RoomResponse(
                room,
                reservations(room),
                links(room, uriInfo)
        );
    }

}
