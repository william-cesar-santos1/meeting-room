package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.exception.BusinessException;
import br.com.ada.classes.meetingroom.exception.ResourceNotFoundException;
import br.com.ada.classes.meetingroom.model.PageResult;
import br.com.ada.classes.meetingroom.model.Room;
import br.com.ada.classes.meetingroom.resource.room.CreateRoomRequest;
import br.com.ada.classes.meetingroom.resource.room.UpdateRoomRequest;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class RoomService {

    private static final Logger LOG = Logger.getLogger(RoomService.class);

    public PageResult<Room> list(Integer minCapacity, Integer page, Integer size) {
        LOG.debugf("Consultando salas - minCapacity=%s, page=%d, size=%d", minCapacity, page, size);
        var query = (minCapacity != null)
                ? Room.find("capacity >= ?1", Sort.by("name"), minCapacity)
                : Room.findAll(Sort.by("name"));
        var result = query.page(Page.of(page, size));
        LOG.debugf("Consulta de salas retornou %d registros (total=%d)", result.list().size(), result.count());
        return new PageResult<>(result.list(), page, size, result.count());
    }

    public PageResult<Room> findByName(String name, Integer page, Integer size) {
        if (name == null || name.isBlank()) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        LOG.debugf("Consultando salas pelo nome '%s', page=%d, size=%d", name, page, size);
        String term = "%" + name.trim().toLowerCase() + "%";
        var query = Room.find("LOWER(name) LIKE ?1", Sort.by("name"), term);
        var result = query.page(Page.of(page, size));
        LOG.debugf("Consulta por nome retornou %d registros", result.list().size());
        return new PageResult<>(result.list(), page, size, result.count());
    }

    public Room findById(Long id) {
        LOG.debugf("Buscando sala por id=%d", id);
        return getRequiredRoom(id);
    }

    public Room create(CreateRoomRequest request) {
        LOG.infof("Criando sala - name='%s', capacity=%d", request.name(), request.capacity());
        validateUniqueName(request.name(), null);
        Room room = new Room();
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        room.persist();
        LOG.infof("Sala persistida com id=%d", room.id);
        return room;
    }

    public Room update(Long id, UpdateRoomRequest request) {
        LOG.infof("Atualizando sala id=%d - name='%s', capacity=%d", id, request.name(), request.capacity());
        Room room = getRequiredRoom(id);
        validateUniqueName(request.name(), id);
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        LOG.debugf("Sala id=%d atualizada com sucesso", id);
        return room;
    }

    public void delete(Long id) {
        LOG.infof("Excluindo sala id=%d", id);
        Room room = getRequiredRoom(id);
        room.delete();
        LOG.infof("Sala id=%d excluída do banco", id);
    }

    public Room getRequiredRoom(Long id) {
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
