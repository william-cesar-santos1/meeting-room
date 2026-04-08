package br.com.ada.classes.meetingroom.model;

import java.time.LocalDateTime;

public class Reservation {

    private Long id;
    private Long roomId;
    private String guestName;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    public Reservation() {
    }

    public Reservation(Long id, Long roomId, String guestName, LocalDateTime startAt, LocalDateTime endAt) {
        this.id = id;
        this.roomId = roomId;
        this.guestName = guestName;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }
}

