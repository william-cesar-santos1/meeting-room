package br.com.ada.classes.meetingroom.resource.room;
public record RoomResponse(
        Long id,
        String name,
        int capacity
) {
}
