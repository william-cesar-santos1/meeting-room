package br.com.ada.classes.meetingroom.integration.holiday;

import br.com.ada.classes.meetingroom.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class HolidayClientDelegate {

    private static final Logger LOG = Logger.getLogger(HolidayClientDelegate.class);

    @Inject
    @RestClient
    HolidayClient holidayClient;

    @Inject
    MeterRegistry meterRegistry;

    @WithSpan("HolidayClientDelegate.fetchNationalHolidays")
    @Retry(abortOn = BusinessException.class)
    @Fallback(HolidayFallbackHandler.class)
    public Set<LocalDate> fetchNationalHolidays(@SpanAttribute("holiday.year") int year) {
        LOG.infof("Chamando BrasilAPI para feriados do ano %d", year);
        Span.current().setAttribute("holiday.source", "brasilapi");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<HolidayResponse> response = holidayClient.getHolidays(year);

            Set<LocalDate> nationalDates = response.stream()
                    .filter(h -> "national".equalsIgnoreCase(h.type()))
                    .map(h -> LocalDate.parse(h.date()))
                    .collect(Collectors.toSet());

            sample.stop(Timer.builder("brasil_api_request_duration_seconds")
                    .description("Tempo de resposta da BrasilAPI de feriados")
                    .tag("year", String.valueOf(year))
                    .tag("status", "success")
                    .publishPercentileHistogram(true)
                    .register(meterRegistry));

            meterRegistry.counter("brasil_api_requests_total",
                            "year", String.valueOf(year),
                            "status", "success")
                    .increment();

            Span.current().setAttribute("holiday.national.count", nationalDates.size());
            LOG.infof("BrasilAPI retornou %d feriados nacionais para o ano %d", nationalDates.size(), year);
            return nationalDates;

        } catch (Exception e) {
            sample.stop(Timer.builder("brasil_api_request_duration_seconds")
                    .description("Tempo de resposta da BrasilAPI de feriados")
                    .tag("year", String.valueOf(year))
                    .tag("status", "error")
                    .publishPercentileHistogram(true)
                    .register(meterRegistry));

            meterRegistry.counter("brasil_api_requests_total",
                            "year", String.valueOf(year),
                            "status", "error")
                    .increment();

            Span.current().setStatus(StatusCode.ERROR, e.getMessage());
            Span.current().recordException(e);
            LOG.errorf(e, "Erro ao chamar BrasilAPI para feriados do ano %d (tentativa será refeita pelo @Retry)", year);
            throw e;
        }
    }
}
