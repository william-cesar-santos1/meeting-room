package br.com.ada.classes.meetingroom.integration.holiday;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "brasil-api")
@Path("/api/feriados/v1")
public interface HolidayClient {

    @GET
    @Path("/{year}")
    List<HolidayResponse> getHolidays(@PathParam("year") int year);
}

