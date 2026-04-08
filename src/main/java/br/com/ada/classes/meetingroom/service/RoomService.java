package br.com.ada.classes.meetingroom.service;


import br.com.ada.classes.meetingroom.model.Room;
import br.com.ada.classes.meetingroom.resource.room.CreateRoomRequest;
import br.com.ada.classes.meetingroom.resource.room.UpdateRoomRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class RoomService {

    private final Map<Long, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Room> list(Integer minCapacity) {
        return rooms.values().stream()
                .filter(room -> minCapacity == null || room.getCapacity() >= minCapacity)
                .sorted(Comparator.comparing(Room::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Room::getId))
                .map(this::copy)
                .toList();
    }

    public List<Room> findByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        String searchTerm = name.toLowerCase().trim();
        return rooms.values().stream()
                .filter(room -> room.getName().toLowerCase().contains(searchTerm))
                .sorted(Comparator.comparing(Room::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::copy)
                .toList();
    }

    public Room findById(Long id) {
        return copy(getRequiredRoom(id));
    }

    public Room create(CreateRoomRequest request) {
        validateUniqueName(request.name(), null);
        long id = sequence.incrementAndGet();
        Room room = new Room(id, request.name().trim(), request.capacity());
        rooms.put(id, room);
        return copy(room);
    }

    public Room update(Long id, UpdateRoomRequest request) {
        Room existingRoom = getRequiredRoom(id);
        validateUniqueName(request.name(), id);
        existingRoom.setName(request.name().trim());
        existingRoom.setCapacity(request.capacity());
        return copy(existingRoom);
    }

    public void delete(Long id) {
        Room removedRoom = rooms.remove(id);
        if (removedRoom == null) {
            throw new NotFoundException("Room with id " + id + " was not found");
        }
    }

    public Room getRequiredRoom(Long id) {
        Room room = rooms.get(id);
        if (room == null) {
            throw new NotFoundException("Room with id " + id + " was not found");
        }
        return room;
    }

    private void validateUniqueName(String name, Long currentId) {
        String normalizedName = name.trim();
        boolean duplicated = rooms.values().stream()
                .anyMatch(room -> room.getName().equalsIgnoreCase(normalizedName) && !room.getId().equals(currentId));
        if (duplicated) {
            throw new BadRequestException("A room with this name already exists");
        }
    }

    private Room copy(Room room) {
        return new Room(room.getId(), room.getName(), room.getCapacity());
    }
}

