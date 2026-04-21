package br.com.ada.classes.meetingroom.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException ofId(String resource, Long id) {
        return new ResourceNotFoundException(resource + " with id " + id + " was not found");
    }
}

