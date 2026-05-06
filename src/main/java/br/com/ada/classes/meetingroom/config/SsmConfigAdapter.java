package br.com.ada.classes.meetingroom.config;

import io.quarkiverse.amazon.ssm.runtime.SsmConfigConfig;

import java.util.List;
import java.util.Optional;

public class SsmConfigAdapter implements SsmConfigConfig {

    private final MeetingRoomSsmConfig delegate;

    public SsmConfigAdapter(MeetingRoomSsmConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public Optional<String> path() {
        return delegate.path();
    }

    @Override
    public Optional<List<String>> names() {
        return Optional.empty();
    }

    @Override
    public boolean recursive() {
        return delegate.recursive();
    }

    @Override
    public boolean withDecryption() {
        return delegate.withDecryption();
    }

    @Override
    public int updateIntervalMinutes() {
        return delegate.updateIntervalMinutes();
    }

}
