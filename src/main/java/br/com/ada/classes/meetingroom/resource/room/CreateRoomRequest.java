package br.com.ada.classes.meetingroom.resource.room;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
public record CreateRoomRequest(
        @NotBlank(message = "Room name is required")
        String name,
        @Min(value = 1, message = "Capacity must be greater than zero")
        int capacity
) {
}
