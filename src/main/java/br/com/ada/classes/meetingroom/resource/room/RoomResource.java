package br.com.ada.classes.meetingroom.resource.room;

import br.com.ada.classes.meetingroom.model.Room;
import br.com.ada.classes.meetingroom.resource.PageResponse;
import br.com.ada.classes.meetingroom.service.RoomService;
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

import static br.com.ada.classes.meetingroom.resource.room.RoomResponse.links;
import static br.com.ada.classes.meetingroom.resource.room.RoomResponse.reservations;

@Path("/rooms")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private static final Logger LOG = Logger.getLogger(RoomResource.class);

    @Inject
    RoomService roomService;

    @GET
    @PermitAll
    @Timed(value = "rooms.list.time", description = "Tempo de processamento da listagem de salas")
    @Counted(value = "rooms.list.count", description = "Número de consultas de listagem de salas")
    public PageResponse<RoomResponse> list(
            @QueryParam("minCapacity") Integer minCapacity,
            @QueryParam("name") String name,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @Context UriInfo uriInfo) {
        LOG.infof("GET /rooms - name=%s, minCapacity=%s, page=%d, size=%d", name, minCapacity, page, size);
        if (name != null && !name.isBlank()) {
            LOG.debugf("Buscando salas pelo nome: '%s'", name);
            return PageResponse.from(
                    roomService.findByName(name, page, size),
                    it -> toResponse(it, uriInfo)
            );
        }
        LOG.debug("Listando todas as salas");
        return PageResponse.from(
                roomService.list(minCapacity, page, size),
                it -> toResponse(it, uriInfo)
        );
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Timed(value = "rooms.findById.time", description = "Tempo de processamento da busca de sala por ID")
    public RoomResponse findById(
            @PathParam("id") Long id,
            @Context UriInfo uriInfo
    ) {
        LOG.infof("GET /rooms/%d", id);
        return toResponse(roomService.findById(id), uriInfo);
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    @Timed(value = "rooms.create.time", description = "Tempo de processamento da criação de sala")
    @Counted(value = "rooms.create.count", description = "Número de criações de salas")
    public Response create(
            @Valid CreateRoomRequest request,
            @Context UriInfo uriInfo
    ) {
        LOG.infof("POST /rooms - name='%s', capacity=%d", request.name(), request.capacity());
        Room room = roomService.create(request);
        LOG.infof("Sala criada com id=%d", room.id);
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
    @Timed(value = "rooms.update.time", description = "Tempo de processamento da atualização de sala")
    @Counted(value = "rooms.update.count", description = "Número de atualizações de salas")
    public RoomResponse update(
            @PathParam("id") Long id,
            @Valid UpdateRoomRequest request,
            @Context UriInfo uriInfo
    ) {
        LOG.infof("PUT /rooms/%d - name='%s', capacity=%d", id, request.name(), request.capacity());
        return toResponse(roomService.update(id, request), uriInfo);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    @Timed(value = "rooms.delete.time", description = "Tempo de processamento da exclusão de sala")
    @Counted(value = "rooms.delete.count", description = "Número de exclusões de salas")
    public Response delete(@PathParam("id") Long id) {
        LOG.infof("DELETE /rooms/%d", id);
        roomService.delete(id);
        LOG.debugf("Sala id=%d excluída com sucesso", id);
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
