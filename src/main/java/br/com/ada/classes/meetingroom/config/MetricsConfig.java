package br.com.ada.classes.meetingroom.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MetricsConfig implements MeterFilter {

    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().startsWith("http.server.requests")
                || id.getName().startsWith("http.client.requests")) {
            return DistributionStatisticConfig.builder()
                    .percentilesHistogram(true)
                    .build()
                    .merge(config);
        }
        return config;
    }
}

