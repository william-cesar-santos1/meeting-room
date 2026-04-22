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

import java.util.List;

@ApplicationScoped
public class RoomService {

    public PageResult<Room> list(Integer minCapacity, int page, int size) {
        var query = (minCapacity != null)
                ? Room.find("capacity >= ?1", Sort.by("name"), minCapacity)
                : Room.findAll(Sort.by("name"));
        var result = query.page(Page.of(page, size));
        return new PageResult<>(result.list(), page, size, result.count());
    }

    public PageResult<Room> findByName(String name, int page, int size) {
        if (name == null || name.isBlank()) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        String term = "%" + name.trim().toLowerCase() + "%";
        var query = Room.find("LOWER(name) LIKE ?1", Sort.by("name"), term);
        var result = query.page(Page.of(page, size));
        return new PageResult<>(result.list(), page, size, result.count());
    }

    public Room findById(Long id) {
        return getRequiredRoom(id);
    }

    public Room create(CreateRoomRequest request) {
        validateUniqueName(request.name(), null);
        Room room = new Room();
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        room.persist();
        return room;
    }

    public Room update(Long id, UpdateRoomRequest request) {
        Room room = getRequiredRoom(id);
        validateUniqueName(request.name(), id);
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
        return room;
    }

    public void delete(Long id) {
        Room room = getRequiredRoom(id);
        room.delete();
    }

    public Room getRequiredRoom(Long id) {
        Room room = Room.findById(id);
        if (room == null) {
            throw ResourceNotFoundException.ofId("Room", id);
        }
        return room;
    }

    private void validateUniqueName(String name, Long currentId) {
        String normalized = name.trim();
        long count = (currentId == null)
                ? Room.count("LOWER(name) = LOWER(?1)", normalized)
                : Room.count("LOWER(name) = LOWER(?1) AND id != ?2", normalized, currentId);
        if (count > 0) {
            throw new BusinessException("A room with this name already exists");
        }
    }

}
