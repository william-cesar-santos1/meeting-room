package br.com.ada.classes.meetingroom.integration.holiday;

import br.com.ada.classes.meetingroom.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@ApplicationScoped
public class HolidayService {

    private static final Logger LOG = Logger.getLogger(HolidayService.class);

    @Inject
    HolidayClientDelegate holidayClientDelegate;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "meeting-room.validation.holiday.enabled", defaultValue = "true")
    boolean holidayValidationEnabled;

    @ConfigProperty(name = "meeting-room.validation.weekend.enabled", defaultValue = "true")
    boolean weekendValidationEnabled;

    @WithSpan("HolidayService.validateDate")
    public void validateDate(@SpanAttribute("holiday.date") LocalDate date) {
        if (weekendValidationEnabled) {
            validateWeekend(date);
        } else {
            LOG.debug("Validação de fim de semana desabilitada via feature toggle");
        }

        if (holidayValidationEnabled) {
            validateHoliday(date);
        } else {
            LOG.debug("Validação de feriado desabilitada via feature toggle");
        }
    }

    private void validateWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            LOG.infof("Tentativa de reserva em fim de semana: %s (%s)", date, dayOfWeek);
            throw new BusinessException(
                    "Não é permitido reservar salas aos finais de semana (" + dayOfWeek + ")"
            );
        }
    }

    private void validateHoliday(LocalDate date) {
        String year = String.valueOf(date.getYear());
        meterRegistry.counter("holiday_cache_requests_total",
                "year", year).increment();

        Set<LocalDate> holidays = fetchNationalHolidays(date.getYear());

        if (holidays.contains(date)) {
            LOG.infof("Tentativa de reserva em feriado nacional: %s", date);
            throw new BusinessException(
                    "Não é permitido reservar salas em feriados nacionais (" + date + ")"
            );
        }
    }

    @CacheResult(cacheName = "national-holidays")
    @WithSpan("HolidayService.fetchNationalHolidays")
    public Set<LocalDate> fetchNationalHolidays(@SpanAttribute("holiday.year") int year) {
        LOG.debugf("Cache miss para feriados do ano %d — delegando ao HolidayClientDelegate", year);
        Span.current().setAttribute("holiday.cache.hit", false);
        meterRegistry.counter("holiday_cache_miss_total",
                "year", String.valueOf(year)).increment();
        return holidayClientDelegate.fetchNationalHolidays(year);
    }
}
