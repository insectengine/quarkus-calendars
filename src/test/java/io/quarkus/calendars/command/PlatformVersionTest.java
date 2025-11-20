package io.quarkus.calendars.command;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformVersionTest {

    @Test
    void shouldCompareMajorVersions() {
        PlatformVersion v3 = new PlatformVersion("3.29.0", LocalDate.now());
        PlatformVersion v4 = new PlatformVersion("4.0.0", LocalDate.now());

        assertThat(v4.compareTo(v3)).isGreaterThan(0);
        assertThat(v3.compareTo(v4)).isLessThan(0);
    }

    @Test
    void shouldCompareMinorVersions() {
        PlatformVersion v320 = new PlatformVersion("3.20.9", LocalDate.now());
        PlatformVersion v322 = new PlatformVersion("3.22.0", LocalDate.now());

        assertThat(v322.compareTo(v320)).isGreaterThan(0);
        assertThat(v320.compareTo(v322)).isLessThan(0);
    }

    @Test
    void shouldCompareMicroVersions() {
        PlatformVersion v3200 = new PlatformVersion("3.20.0", LocalDate.now());
        PlatformVersion v3209 = new PlatformVersion("3.20.9", LocalDate.now());

        assertThat(v3209.compareTo(v3200)).isGreaterThan(0);
        assertThat(v3200.compareTo(v3209)).isLessThan(0);
    }

    @Test
    void shouldComparePatchVersions() {
        PlatformVersion v32090 = new PlatformVersion("3.20.9", LocalDate.now());
        PlatformVersion v32091 = new PlatformVersion("3.20.9.1", LocalDate.now());

        assertThat(v32091.compareTo(v32090)).isGreaterThan(0);
        assertThat(v32090.compareTo(v32091)).isLessThan(0);
    }

    @Test
    void shouldCompareClassifiers_FinalIsGreaterThanCR() {
        PlatformVersion vCR = new PlatformVersion("3.22.0.CR1", LocalDate.now());
        PlatformVersion vFinal = new PlatformVersion("3.22.0.Final", LocalDate.now());

        assertThat(vFinal.compareTo(vCR)).isGreaterThan(0);
        assertThat(vCR.compareTo(vFinal)).isLessThan(0);
    }

    @Test
    void shouldCompareClassifiers_NoClassifierIsGreaterThanCR() {
        PlatformVersion vCR = new PlatformVersion("3.22.0.CR1", LocalDate.now());
        PlatformVersion vNoClassifier = new PlatformVersion("3.22.0", LocalDate.now());

        assertThat(vNoClassifier.compareTo(vCR)).isGreaterThan(0);
        assertThat(vCR.compareTo(vNoClassifier)).isLessThan(0);
    }

    @Test
    void shouldHandleEqualVersions() {
        PlatformVersion v1 = new PlatformVersion("3.22.0.Final", LocalDate.now());
        PlatformVersion v2 = new PlatformVersion("3.22.0.Final", LocalDate.now());

        assertThat(v1.compareTo(v2)).isEqualTo(0);
    }

    @Test
    void shouldHandleFinalAsImplicitClassifier() {
        PlatformVersion vExplicit = new PlatformVersion("3.22.0.Final", LocalDate.now());
        PlatformVersion vImplicit = new PlatformVersion("3.22.0", LocalDate.now());

        // Both should be considered equal (no classifier = Final)
        assertThat(vExplicit.compareTo(vImplicit)).isEqualTo(0);
    }

    // User-specified test cases

    @Test
    void shouldSatisfy_3_22_0_greaterThan_3_20_9() {
        PlatformVersion v3209 = new PlatformVersion("3.20.9", LocalDate.now());
        PlatformVersion v3220 = new PlatformVersion("3.22.0", LocalDate.now());

        assertThat(v3220.compareTo(v3209))
                .as("3.22.0 should be greater than 3.20.9")
                .isGreaterThan(0);
    }

    @Test
    void shouldSatisfy_3_22_0_greaterThan_3_20_9_1() {
        PlatformVersion v32091 = new PlatformVersion("3.20.9.1", LocalDate.now());
        PlatformVersion v3220 = new PlatformVersion("3.22.0", LocalDate.now());

        assertThat(v3220.compareTo(v32091))
                .as("3.22.0 should be greater than 3.20.9.1")
                .isGreaterThan(0);
    }

    @Test
    void shouldSatisfy_3_22_0_greaterThan_3_22_0_CR1() {
        PlatformVersion v3220CR1 = new PlatformVersion("3.22.0.CR1", LocalDate.now());
        PlatformVersion v3220 = new PlatformVersion("3.22.0", LocalDate.now());

        assertThat(v3220.compareTo(v3220CR1))
                .as("3.22.0 should be greater than 3.22.0.CR1")
                .isGreaterThan(0);
    }

    @Test
    void shouldSatisfy_4_0_0_greaterThan_3_29_0() {
        PlatformVersion v3290 = new PlatformVersion("3.29.0", LocalDate.now());
        PlatformVersion v400 = new PlatformVersion("4.0.0", LocalDate.now());

        assertThat(v400.compareTo(v3290))
                .as("4.0.0 should be greater than 3.29.0")
                .isGreaterThan(0);
    }

    @Test
    void shouldSatisfy_4_0_0_greaterThan_3_29_0_1() {
        PlatformVersion v32901 = new PlatformVersion("3.29.0.1", LocalDate.now());
        PlatformVersion v400 = new PlatformVersion("4.0.0", LocalDate.now());

        assertThat(v400.compareTo(v32901))
                .as("4.0.0 should be greater than 3.29.0.1")
                .isGreaterThan(0);
    }

    @Test
    void shouldSortVersionsCorrectly() {
        List<PlatformVersion> versions = new ArrayList<>();
        LocalDate now = LocalDate.now();

        versions.add(new PlatformVersion("3.22.0.CR1", now));
        versions.add(new PlatformVersion("4.0.0", now));
        versions.add(new PlatformVersion("3.20.9", now));
        versions.add(new PlatformVersion("3.22.0", now));
        versions.add(new PlatformVersion("3.20.9.1", now));
        versions.add(new PlatformVersion("3.29.0", now));
        versions.add(new PlatformVersion("3.29.0.1", now));

        Collections.sort(versions);

        assertThat(versions)
                .extracting(PlatformVersion::version)
                .containsExactly(
                        "3.20.9",
                        "3.20.9.1",
                        "3.22.0.CR1",
                        "3.22.0",
                        "3.29.0",
                        "3.29.0.1",
                        "4.0.0"
                );
    }

    @Test
    void shouldHandleComplexVersionSorting() {
        List<PlatformVersion> versions = new ArrayList<>();
        LocalDate now = LocalDate.now();

        versions.add(new PlatformVersion("3.21.0", now));
        versions.add(new PlatformVersion("3.21.0.CR1", now));
        versions.add(new PlatformVersion("3.21.1", now));
        versions.add(new PlatformVersion("3.20.4", now));
        versions.add(new PlatformVersion("3.20.3.1", now));

        Collections.sort(versions);

        assertThat(versions)
                .extracting(PlatformVersion::version)
                .containsExactly(
                        "3.20.3.1",
                        "3.20.4",
                        "3.21.0.CR1",
                        "3.21.0",
                        "3.21.1"
                );
    }

    @Test
    void shouldCheckIsAtLeast() {
        PlatformVersion v3220 = new PlatformVersion("3.22.0", LocalDate.now());
        PlatformVersion v3200 = new PlatformVersion("3.20.0", LocalDate.now());
        PlatformVersion v4000 = new PlatformVersion("4.0.0", LocalDate.now());

        assertThat(v3220.isAtLeast("3.20.0")).isTrue();
        assertThat(v3220.isAtLeast("3.22.0")).isTrue();
        assertThat(v3220.isAtLeast("3.23.0")).isFalse();
        assertThat(v3220.isAtLeast("4.0.0")).isFalse();

        assertThat(v3200.isAtLeast("3.20.0")).isTrue();
        assertThat(v3200.isAtLeast("3.19.0")).isTrue();
        assertThat(v3200.isAtLeast("3.21.0")).isFalse();

        assertThat(v4000.isAtLeast("3.20.0")).isTrue();
        assertThat(v4000.isAtLeast("4.0.0")).isTrue();
        assertThat(v4000.isAtLeast("4.1.0")).isFalse();
    }

    @Test
    void shouldHandleTwoDigitVersions() {
        PlatformVersion v329 = new PlatformVersion("3.29", LocalDate.now());
        PlatformVersion v3290 = new PlatformVersion("3.29.0", LocalDate.now());
        PlatformVersion v330 = new PlatformVersion("3.30", LocalDate.now());

        // 3.29 should equal 3.29.0 (micro defaults to 0)
        assertThat(v329.compareTo(v3290)).isEqualTo(0);

        // 3.30 should be greater than 3.29
        assertThat(v330.compareTo(v329)).isGreaterThan(0);
    }

    @Test
    void shouldHandleMultipleCRVersions() {
        List<PlatformVersion> versions = new ArrayList<>();
        LocalDate now = LocalDate.now();

        versions.add(new PlatformVersion("3.22.0.CR2", now));
        versions.add(new PlatformVersion("3.22.0.CR1", now));
        versions.add(new PlatformVersion("3.22.0", now));

        Collections.sort(versions);

        // CR1 < CR2 < Final (no classifier)
        assertThat(versions)
                .extracting(PlatformVersion::version)
                .containsExactly(
                        "3.22.0.CR1",
                        "3.22.0.CR2",
                        "3.22.0"
                );
    }

    @Test
    void shouldHandleFinalExplicitly() {
        PlatformVersion vFinal = new PlatformVersion("3.22.0.Final", LocalDate.now());
        PlatformVersion vNoClassifier = new PlatformVersion("3.22.0", LocalDate.now());
        PlatformVersion vCR = new PlatformVersion("3.22.0.CR1", LocalDate.now());

        // Final and no classifier should be equal
        assertThat(vFinal.compareTo(vNoClassifier)).isEqualTo(0);

        // Both should be greater than CR
        assertThat(vFinal.compareTo(vCR)).isGreaterThan(0);
        assertThat(vNoClassifier.compareTo(vCR)).isGreaterThan(0);
    }

    @Test
    void shouldPreserveDateInRecord() {
        LocalDate date = LocalDate.of(2025, 3, 26);
        PlatformVersion version = new PlatformVersion("3.20.0", date);

        assertThat(version.version()).isEqualTo("3.20.0");
        assertThat(version.date()).isEqualTo(date);
    }
}
