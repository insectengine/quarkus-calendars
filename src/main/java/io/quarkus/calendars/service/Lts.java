package io.quarkus.calendars.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class Lts {

    @ConfigProperty(name = "lts.versions")
    Optional<List<String>> versions;


    public List<String> getVersions() {
        return versions.orElseGet(List::of);
    }

    public boolean isLts(String version) {
        for (String s : getVersions()) {
            if (version.startsWith(s.trim())) {
                return true;
            }
        }
        return false;
    }


}
