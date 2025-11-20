package io.quarkus.calendars.command;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import io.quarkus.calendars.model.ReleaseEvent;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@CommandLine.Command(
    name = "release-sync",
    description = "Generate YAML files for missing Quarkus Platform releases from Maven Central"
)
public class ReleaseSyncCommand implements Callable<Integer> {

    private static final String MAVEN_REPO_URL = "https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/";
    private static final String RELEASES_DIR = "quarkus-releases";
    private static final String MIN_VERSION = "3.20.0";

    // Pattern to match lines like: <a href="2.0.0.Final/" title="2.0.0.Final/">2.0.0.Final/</a>                                      2021-06-30 13:00         -
    private static final Pattern LINE_PATTERN = Pattern.compile("<a href=\"([^/\"]+)/\".*?(\\d{4}-\\d{2}-\\d{2})\\s+\\d{2}:\\d{2}\\s+-");

    @Inject
    YAMLMapper yamlMapper;

    @Override
    public Integer call() {
        try {
            Log.info("Fetching platform versions from Maven Central...\n");

            // Step 1: Fetch and parse Maven repository page
            List<PlatformVersion> versions = fetchPlatformVersions();
            Log.info("Found " + versions.size() + " total versions\n");

            // Step 2: Sort versions
            Collections.sort(versions);

            // Step 3: Filter versions >= 3.20.0
            List<PlatformVersion> filteredVersions = versions.stream()
                .filter(v -> v.isAtLeast(MIN_VERSION))
                .toList();

            Log.info("Processing " + filteredVersions.size() + " versions >= " + MIN_VERSION + "\n");

            // Step 4: Load existing YAML files
            Map<String, LocalDate> existingReleases = loadExistingReleases();

            // Step 5: Process each version
            int created = 0;
            int updated = 0;
            int unchanged = 0;

            for (PlatformVersion version : filteredVersions) {
                String versionStr = version.version();
                LocalDate existingDate = existingReleases.get(versionStr);

                if (existingDate == null) {
                    // Create new YAML file
                    createReleaseYaml(version);
                    created++;
                    Log.info("✓ Created: " + versionStr + " (" + version.date() + ")");
                } else if (!existingDate.equals(version.date())) {
                    // Update existing file with new date
                    updateReleaseYaml(version, existingDate);
                    updated++;
                    Log.warn("⚠ Updated: " + versionStr + " (date changed from " + existingDate + " to " + version.date() + ")");
                } else {
                    // Already exists with correct date
                    unchanged++;
                }
            }

            Log.info("\nSummary:");
            Log.info("  Created: " + created);
            Log.info("  Updated: " + updated);
            Log.info("  Unchanged: " + unchanged);

            return 0;

        } catch (Exception e) {
            Log.error("Error during release sync: " + e.getMessage(), e);
            return 1;
        }
    }

    private List<PlatformVersion> fetchPlatformVersions() throws IOException {
        HttpTransport httpTransport = new NetHttpTransport();
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(MAVEN_REPO_URL));

        String content = request.execute().parseAsString();
        return parseVersionsFromHtml(content);
    }

    private List<PlatformVersion> parseVersionsFromHtml(String html) {
        List<PlatformVersion> versions = new ArrayList<>();
        String[] lines = html.split("\n");

        for (String line : lines) {
            Matcher matcher = LINE_PATTERN.matcher(line);
            if (matcher.find()) {
                String version = matcher.group(1);
                String dateStr = matcher.group(2);

                try {
                    LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    versions.add(new PlatformVersion(version, date));
                } catch (Exception e) {
                    Log.warn("Failed to parse date for version " + version + ": " + dateStr);
                }
            }
        }

        return versions;
    }

    private Map<String, LocalDate> loadExistingReleases() {
        Map<String, LocalDate> releases = new HashMap<>();
        Path dir = Paths.get(RELEASES_DIR);

        if (!Files.exists(dir)) {
            Log.warn("Directory " + RELEASES_DIR + " does not exist, will create it");
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                Log.error("Failed to create directory: " + e.getMessage());
            }
            return releases;
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> yamlFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                .filter(path -> path.getFileName().toString().startsWith("quarkus-platform-"))
                .toList();

            for (Path yamlFile : yamlFiles) {
                try {
                    ReleaseEvent event = yamlMapper.readValue(yamlFile.toFile(), ReleaseEvent.class);
                    String version = extractVersionFromFilename(yamlFile.getFileName().toString());
                    if (version != null && event.getDate() != null) {
                        releases.put(version, event.getDate());
                    }
                } catch (IOException e) {
                    Log.warn("Failed to parse existing file " + yamlFile + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Log.error("Failed to read existing releases: " + e.getMessage());
        }

        return releases;
    }

    private String extractVersionFromFilename(String filename) {
        // Extract version from filename like "quarkus-platform-3.24.0-cr1-release.yaml"
        // or "quarkus-platform-3.24.1-release.yaml"
        if (filename.startsWith("quarkus-platform-") && filename.endsWith("-release.yaml")) {
            String versionPart = filename.substring("quarkus-platform-".length(), filename.length() - "-release.yaml".length());
            // Remove -cr1, -final, etc. suffixes and reconstruct the version
            if (versionPart.endsWith("-cr1")) {
                return versionPart.substring(0, versionPart.length() - "-cr1".length()) + ".CR1";
            } else if (versionPart.endsWith("-final")) {
                return versionPart.substring(0, versionPart.length() - "-final".length()) + ".Final";
            } else {
                // Regular version without classifier
                return versionPart + ".Final";
            }
        }
        return null;
    }

    private void createReleaseYaml(PlatformVersion version) throws IOException {
        ReleaseEvent event = new ReleaseEvent(getReleaseTitle(version.version()), version.date());
        Path filePath = getYamlFilePath(version.version());

        // Ensure parent directory exists
        Files.createDirectories(filePath.getParent());

        yamlMapper.writeValue(filePath.toFile(), event);
    }

    private void updateReleaseYaml(PlatformVersion version, LocalDate oldDate) throws IOException {
        Path filePath = getYamlFilePath(version.version());
        ReleaseEvent event = new ReleaseEvent(getReleaseTitle(version.version()), version.date());
        yamlMapper.writeValue(filePath.toFile(), event);
    }

    private String getReleaseTitle(String version) {
        if (version.toUpperCase().contains("CR")) {
            return "Quarkus Platform " + version + " Pre-Release";
        } else {
            return "Quarkus Platform " + version + " Release";
        }
    }

    private Path getYamlFilePath(String version) {
        String filename = "quarkus-platform-" + normalizeVersionForFilename(version) + "-release.yaml";
        return Paths.get(RELEASES_DIR, filename);
    }

    private String normalizeVersionForFilename(String version) {
        // Convert "3.24.0.CR1" to "3.24.0-cr1"
        // Convert "3.24.1.Final" to "3.24.1"
        String normalized = version.toLowerCase();

        if (normalized.endsWith(".final")) {
            normalized = normalized.substring(0, normalized.length() - ".final".length());
        } else if (normalized.contains(".cr")) {
            normalized = normalized.replace(".cr", "-cr");
        }

        return normalized;
    }
}
