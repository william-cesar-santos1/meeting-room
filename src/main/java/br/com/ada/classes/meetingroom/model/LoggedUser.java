package br.com.ada.classes.meetingroom.model;

public record LoggedUser(Long id, String username, String name, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}

