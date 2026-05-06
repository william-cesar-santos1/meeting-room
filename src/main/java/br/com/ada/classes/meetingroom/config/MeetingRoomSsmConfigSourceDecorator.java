package br.com.ada.classes.meetingroom.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MeetingRoomSsmConfigSourceDecorator implements ConfigSource {

    private final ConfigSource delegate;
    private final String appName;
    private final String environment;

    public MeetingRoomSsmConfigSourceDecorator(
            ConfigSource delegate,
            String prefix
    ) {
        this.delegate = delegate;
        this.appName = prefix.split("\\.")[0];
        this.environment = prefix.split("\\.")[1];
    }

    @Override
    public String getValue(String propertyName) {
        String translatedName = translateName(propertyName);
        return delegate.getValue(translatedName);
    }

    @Override
    public String getName() {
        return "MeetingRoomSsmConfigSourceDecorator(" + delegate.getName() + ")";
    }

    @Override
    public Map<String, String> getProperties() {
        return delegate.getProperties().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> revertName(e.getKey()),
                        Map.Entry::getValue
                ));
    }

    @Override
    public Set<String> getPropertyNames() {
        return delegate.getPropertyNames().stream()
                .map(this::revertName)
                .collect(Collectors.toSet());
    }

    @Override
    public int getOrdinal() {
        return delegate.getOrdinal() + 100;
    }

    private String translateName(String name) {
        String newParameterName = null;
        if (name.startsWith("%")) {
            var environment = name.substring(1, name.indexOf('.'));
            if (environment.isEmpty()) {
                newParameterName = appName + "."  + name.substring(name.indexOf('.') + 1);
            } else {
                newParameterName = appName + "." + environment + "." + name.substring(name.indexOf('.') + 1);
            }
        } else {
            newParameterName = appName + "." + environment + "." + name.substring(name.indexOf('.') + 1);
        }
        return newParameterName;
    }

    private String revertName(String name) {
        int first = name.indexOf('.');
        int second = name.indexOf('.', first + 1);
        return name.substring(second + 1);
    }

}