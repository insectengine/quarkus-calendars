package io.quarkus.calendars.command;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Quarkus platform version with its release date.
 */
public record PlatformVersion(String version, LocalDate date) implements Comparable<PlatformVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(.+))?$");

    /**
     * Compares versions based on their numerical components and classifiers.
     * Versions are sorted by major.minor.micro.patch, then by classifier.
     * Final comes after CR versions.
     */
    @Override
    public int compareTo(PlatformVersion other) {
        VersionComponents thisComponents = parseVersion(this.version);
        VersionComponents otherComponents = parseVersion(other.version);

        // Compare numerical parts
        int result = Integer.compare(thisComponents.major, otherComponents.major);
        if (result != 0) return result;

        result = Integer.compare(thisComponents.minor, otherComponents.minor);
        if (result != 0) return result;

        result = Integer.compare(thisComponents.micro, otherComponents.micro);
        if (result != 0) return result;

        result = Integer.compare(thisComponents.patch, otherComponents.patch);
        if (result != 0) return result;

        // Compare classifiers
        return compareClassifiers(thisComponents.classifier, otherComponents.classifier);
    }

    private VersionComponents parseVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            // Return a default for unparseable versions
            return new VersionComponents(0, 0, 0, 0, version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int micro = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        int patch = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;
        String classifier = matcher.group(5) != null ? matcher.group(5) : "";

        return new VersionComponents(major, minor, micro, patch, classifier);
    }

    private int compareClassifiers(String c1, String c2) {
        // No classifier or "Final" is the most recent
        boolean c1IsFinal = c1.isEmpty() || c1.equalsIgnoreCase("Final");
        boolean c2IsFinal = c2.isEmpty() || c2.equalsIgnoreCase("Final");

        if (c1IsFinal && c2IsFinal) return 0;
        if (c1IsFinal) return 1; // c1 is more recent
        if (c2IsFinal) return -1; // c2 is more recent

        // Both have non-Final classifiers, compare lexicographically
        return c1.compareToIgnoreCase(c2);
    }

    /**
     * Checks if this version is at least the specified version.
     */
    public boolean isAtLeast(String minVersion) {
        return compareTo(new PlatformVersion(minVersion, LocalDate.MIN)) >= 0;
    }

    private record VersionComponents(int major, int minor, int micro, int patch, String classifier) {}
}
