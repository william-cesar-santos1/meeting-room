package br.com.ada.classes.meetingroom.integration.holiday;

import br.com.ada.classes.meetingroom.exception.BusinessException;
import br.com.ada.classes.meetingroom.exception.IntegrationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.Set;

@Dependent
public class HolidayFallbackHandler implements FallbackHandler<Set<LocalDate>> {

    private static final Logger LOG = Logger.getLogger(HolidayFallbackHandler.class);

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "meeting-room.integration.brasil-api.fallback.allow-continue", defaultValue = "true")
    boolean fallbackAllowContinue;

    @Override
    public Set<LocalDate> handle(ExecutionContext context) {
        int year = (int) context.getParameters()[0];

        Span.current().setAttribute("holiday.fallback", true);
        Span.current().setAttribute("holiday.fallback.allow_continue", fallbackAllowContinue);

        if (fallbackAllowContinue) {
            meterRegistry.counter("brasil_api_fallback_total",
                            "year", String.valueOf(year),
                            "outcome", "allow_continue")
                    .increment();
            LOG.warnf("Fallback ativado para BrasilAPI (ano=%d): allow-continue=true — validação de feriado ignorada", year);
            return Set.of();
        } else {
            meterRegistry.counter("brasil_api_fallback_total",
                            "year", String.valueOf(year),
                            "outcome", "blocked")
                    .increment();
            LOG.errorf("Fallback ativado para BrasilAPI (ano=%d): allow-continue=false — reserva bloqueada", year);
                Span.current().setStatus(StatusCode.ERROR, "BrasilAPI indisponível — reserva bloqueada");
                throw new IntegrationException(
                        "Não foi possível verificar os feriados nacionais. Tente novamente mais tarde."
                );
        }
    }
}

