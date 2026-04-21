package br.com.ada.classes.meetingroom.resource.exceptionmapper;

import java.util.List;

public record ErrorResponse(int status, String error, List<String> messages) {

    public ErrorResponse(int status, String error, String message) {
        this(status, error, List.of(message));
    }
}

