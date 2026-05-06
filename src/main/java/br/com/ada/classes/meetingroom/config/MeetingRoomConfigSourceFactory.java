package br.com.ada.classes.meetingroom.config;

import io.quarkiverse.amazon.ssm.runtime.SsmConfigSourceFactory;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Collections;
import java.util.OptionalInt;

public class MeetingRoomConfigSourceFactory implements ConfigSourceFactory.ConfigurableConfigSourceFactory<MeetingRoomSsmConfig> {

    private static final SsmConfigSourceFactory DELEGATE_FACTORY = new SsmConfigSourceFactory();

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context, MeetingRoomSsmConfig config) {
        var ssmSourceConfig = DELEGATE_FACTORY.getConfigSources(context, new SsmConfigAdapter(config));
        var iterator = ssmSourceConfig.iterator();
        if (iterator.hasNext()) {
            ssmSourceConfig = Collections.singletonList(
                    new MeetingRoomSsmConfigSourceDecorator(
                            iterator.next(),
                            config.path().orElse("").replace("/", ".")
                    )
            );
        }
        return ssmSourceConfig;
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(500);
    }

}
