package br.com.ada.classes.meetingroom.integration.holiday;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HolidayResponse(
        String date,
        String localName,
        String name,
        String type
) {}

