package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.exception.BusinessException;
import br.com.ada.classes.meetingroom.exception.ResourceNotFoundException;
import br.com.ada.classes.meetingroom.model.PageResult;
import br.com.ada.classes.meetingroom.model.Room;
import br.com.ada.classes.meetingroom.resource.room.CreateRoomRequest;
import br.com.ada.classes.meetingroom.resource.room.UpdateRoomRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class RoomService {

    private static final Logger LOG = Logger.getLogger(RoomService.class);

    @WithSpan("RoomService.list")
    public PageResult<Room> list(
            @SpanAttribute("room.filter.minCapacity") Integer minCapacity,
            @SpanAttribute("room.page") Integer page,
            @SpanAttribute("room.size") Integer size) {
        LOG.debugf("Consultando salas - minCapacity=%s, page=%d, size=%d", minCapacity, page, size);
        var query = (minCapacity != null)
                ? Room.find("capacity >= ?1", Sort.by("name"), minCapacity)
                : Room.findAll(Sort.by("name"));
        var result = query.page(Page.of(page, size));
        Span.current().setAttribute("room.result.count", result.list().size());
        LOG.debugf("Consulta de salas retornou %d registros (total=%d)", result.list().size(), result.count());
        return new PageResult<>(result.list(), page, size, result.count());
    }

    @WithSpan("RoomService.findByName")
    public PageResult<Room> findByName(
            @SpanAttribute("room.filter.name") String name,
            @SpanAttribute("room.page") Integer page,
            @SpanAttribute("room.size") Integer size) {
        if (name == null || name.isBlank()) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        LOG.debugf("Consultando salas pelo nome '%s', page=%d, size=%d", name, page, size);
        String term = "%" + name.trim().toLowerCase() + "%";
        var query = Room.find("LOWER(name) LIKE ?1", Sort.by("name"), term);
        var result = query.page(Page.of(page, size));
        Span.current().setAttribute("room.result.count", result.list().size());
        LOG.debugf("Consulta por nome retornou %d registros", result.list().size());
        return new PageResult<>(result.list(), page, size, result.count());
    }

    @WithSpan("RoomService.findById")
    public Room findById(@SpanAttribute("room.id") Long id) {
        LOG.debugf("Buscando sala por id=%d", id);
        return getRequiredRoom(id);
    }

    @WithSpan("RoomService.create")
    public Room create(CreateRoomRequest request) {
        LOG.infof("Criando sala - name='%s', capacity=%d", request.name(), request.capacity());
        Span.current().setAttribute("room.name", request.name());
        Span.current().setAttribute("room.capacity", request.capacity());
        validateUniqueName(request.name(), null);
        Room room = new Room();
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        room.persist();
        Span.current().setAttribute("room.id", room.id);
        LOG.infof("Sala persistida com id=%d", room.id);
        return room;
    }

    @WithSpan("RoomService.update")
    public Room update(@SpanAttribute("room.id") Long id, UpdateRoomRequest request) {
        LOG.infof("Atualizando sala id=%d - name='%s', capacity=%d", id, request.name(), request.capacity());
        Room room = getRequiredRoom(id);
        validateUniqueName(request.name(), id);
        Span.current().setAttribute("room.name", request.name());
        Span.current().setAttribute("room.capacity", request.capacity());
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        LOG.debugf("Sala id=%d atualizada com sucesso", id);
        return room;
    }

    @WithSpan("RoomService.delete")
    public void delete(@SpanAttribute("room.id") Long id) {
        LOG.infof("Excluindo sala id=%d", id);
        Room room = getRequiredRoom(id);
        room.delete();
        LOG.infof("Sala id=%d excluída do banco", id);
    }

    @WithSpan("RoomService.getRequiredRoom")
    public Room getRequiredRoom(@SpanAttribute("room.id") Long id) {
        Room room = Room.findById(id);
        if (room == null) {
            LOG.warnf("Sala não encontrada: id=%d", id);
            throw ResourceNotFoundException.ofId("Room", id);
        }
        return room;
    }

    private void validateUniqueName(String name, Long currentId) {
        String normalized = name.trim();
        LOG.debugf("Validando unicidade do nome '%s' (excluindo id=%s)", normalized, currentId);
        long count = (currentId == null)
                ? Room.count("LOWER(name) = LOWER(?1)", normalized)
                : Room.count("LOWER(name) = LOWER(?1) AND id != ?2", normalized, currentId);
        if (count > 0) {
            LOG.warnf("Nome de sala duplicado: '%s'", normalized);
            throw new BusinessException("A room with this name already exists");
        }
    }

}
