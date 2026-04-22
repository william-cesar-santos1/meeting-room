package br.com.ada.classes.meetingroom.integration.holiday;

import br.com.ada.classes.meetingroom.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
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

    @RestClient
    HolidayClient holidayClient;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "meeting-room.integration.brasil-api.fallback.allow-continue", defaultValue = "true")
    boolean fallbackAllowContinue;

    @Retry(abortOn = BusinessException.class)
    @Fallback(HolidayFallbackHandler.class)
    public Set<LocalDate> fetchNationalHolidays(int year) {
        LOG.infof("Chamando BrasilAPI para feriados do ano %d", year);

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
                    .register(meterRegistry));

            meterRegistry.counter("brasil_api_requests_total",
                            "year", String.valueOf(year),
                            "status", "success")
                    .increment();

            LOG.infof("BrasilAPI retornou %d feriados nacionais para o ano %d", nationalDates.size(), year);
            return nationalDates;

        } catch (Exception e) {
            sample.stop(Timer.builder("brasil_api_request_duration_seconds")
                    .description("Tempo de resposta da BrasilAPI de feriados")
                    .tag("year", String.valueOf(year))
                    .tag("status", "error")
                    .register(meterRegistry));

            meterRegistry.counter("brasil_api_requests_total",
                            "year", String.valueOf(year),
                            "status", "error")
                    .increment();

            LOG.errorf(e, "Erro ao chamar BrasilAPI para feriados do ano %d (tentativa será refeita pelo @Retry)", year);
            throw e;
        }
    }

    boolean isFallbackAllowContinue() {
        return fallbackAllowContinue;
    }

    public static class HolidayFallbackHandler implements FallbackHandler<Set<LocalDate>> {

        @Inject
        HolidayClientDelegate delegate;

        @Override
        public Set<LocalDate> handle(ExecutionContext context) {
            int year = (int) context.getParameters()[0];

            delegate.meterRegistry.counter("brasil_api_fallback_total",
                            "year", String.valueOf(year))
                    .increment();

            if (delegate.isFallbackAllowContinue()) {
                LOG.warnf("Fallback ativado para BrasilAPI (ano=%d): allow-continue=true — validação de feriado ignorada", year);
                return Set.of();
            } else {
                LOG.errorf("Fallback ativado para BrasilAPI (ano=%d): allow-continue=false — reserva bloqueada", year);
                throw new BusinessException(
                        "Não foi possível verificar os feriados nacionais. Tente novamente mais tarde."
                );
            }
        }
    }
}

