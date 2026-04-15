package br.com.ada.classes.meetingroom.resource.auth;

public record TokenResponse(

        String token,
        String username,
        String name,
        String role

) {
}
