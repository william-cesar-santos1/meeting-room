package br.com.ada.classes.meetingroom.resource.exceptionmapper;

import br.com.ada.classes.meetingroom.exception.AccessDeniedException;
import br.com.ada.classes.meetingroom.exception.AuthenticationException;
import br.com.ada.classes.meetingroom.exception.BusinessException;
import br.com.ada.classes.meetingroom.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof ResourceNotFoundException e) {
            return buildResponse(Response.Status.NOT_FOUND, "Not Found", e.getMessage());
        }
        if (exception instanceof BusinessException e) {
            return buildUnprocessable(List.of(e.getMessage()));
        }
        if (exception instanceof AccessDeniedException e) {
            return buildResponse(Response.Status.FORBIDDEN, "Forbidden", e.getMessage());
        }
        if (exception instanceof AuthenticationException e) {
            return buildResponse(Response.Status.UNAUTHORIZED, "Unauthorized", e.getMessage());
        }
        if (exception instanceof ConstraintViolationException e) {
            List<String> messages = e.getConstraintViolations()
                    .stream()
                    .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                    .sorted()
                    .toList();
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "Bad Request", messages))
                    .build();
        }
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    private Response buildUnprocessable(List<String> messages) {
        return Response.status(UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable Entity", messages))
                .build();
    }

    private Response buildResponse(Response.Status status, String error, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(status.getStatusCode(), error, message))
                .build();
    }
}
