package com.gfos.ideaboard.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashMap;
import java.util.Map;

@ApplicationPath("/api")
public class ApplicationConfig extends Application {

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>();
        // Jackson JSON Provider aktivieren
        props.put("jersey.config.server.provider.packages",
                "com.gfos.ideaboard.resource," +
                "com.gfos.ideaboard.config," +
                "com.gfos.ideaboard.security," +
                "com.gfos.ideaboard.exception");
        return props;
    }
}
