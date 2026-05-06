package br.com.ada.classes.meetingroom.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus.meeting-room.ssm.config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MeetingRoomSsmConfig {

    @WithDefault("false")
    boolean enabled();

    Optional<String> path();

    @WithDefault("true")
    boolean recursive();

    @WithDefault("true")
    boolean withDecryption();

    @WithDefault("5")
    int updateIntervalMinutes();

}
