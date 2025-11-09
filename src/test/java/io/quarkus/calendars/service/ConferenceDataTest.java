package io.quarkus.calendars.service;

import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.ReconciliationAction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for conference data (hangout links) functionality.
 * These tests verify that conference data is properly created for supported platforms
 * and not created for unsupported platforms.
 */
@QuarkusTest
@TestProfile(ConferenceDataTest.MockProfile.class)
class ConferenceDataTest {

    public static class MockProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "quarkus.arc.selected-alternatives", "io.quarkus.calendars.service.MockGoogleCalendarService",
                "google.calendar.calendars.releases.id", "test-releases@calendar.com",
                "google.calendar.calendars.calls.id", "test-calls@calendar.com"
            );
        }
    }

    @Inject
    CalendarReconciliation reconciliation;

    @Inject
    MockGoogleCalendarService mockCalendarService;

    @Inject
    LocalEventLoader localEventLoader;

    private static final String CALLS_CALENDAR_ID = "test-calls@calendar.com";

    @BeforeEach
    void setUp() {
        mockCalendarService.reset();
    }

    @Test
    void shouldCreateConferenceDataForGoogleMeet() {
        // Given: A CallEvent with a Google Meet link
        String googleMeetLink = "https://meet.google.com/abc-defg-hij";
        CallEvent callEvent = new CallEvent(
            "Test Call",
            "Test Description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            googleMeetLink
        );

        // When: We reconcile the event (creating it in the mock calendar)
        List<CallEvent> localEvents = List.of(callEvent);
        reconciliation.reconcile(localEvents, List.of(), CALLS_CALENDAR_ID, false);

        // Then: The created event should have conference data
        List<Event> events = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Test Call");
        assertThat(events).hasSize(1);

        Event createdEvent = events.get(0);
        assertThat(createdEvent.getConferenceData()).isNotNull();
        assertThat(createdEvent.getConferenceData().getEntryPoints()).isNotEmpty();

        EntryPoint entryPoint = createdEvent.getConferenceData().getEntryPoints().get(0);
        assertThat(entryPoint.getEntryPointType()).isEqualTo("video");
        assertThat(entryPoint.getUri()).isEqualTo(googleMeetLink);
        assertThat(entryPoint.getLabel()).isEqualTo("Join with Google Meet");

        // Verify conferenceSolution
        assertThat(createdEvent.getConferenceData().getConferenceSolution()).isNotNull();
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getName()).isEqualTo("Google Meet");
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getKey()).isNotNull();
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getKey().getType()).isEqualTo("addOn");

        // The link should also be in the description
        assertThat(createdEvent.getDescription()).contains("Join: " + googleMeetLink);
    }

    @Test
    void shouldCreateConferenceDataForZoom() {
        // Given: A CallEvent with a Zoom link
        String zoomLink = "https://zoom.us/j/123456789";
        CallEvent callEvent = new CallEvent(
            "Zoom Call",
            "Test Description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            zoomLink
        );

        // When: We reconcile the event
        List<CallEvent> localEvents = List.of(callEvent);
        reconciliation.reconcile(localEvents, List.of(), CALLS_CALENDAR_ID, false);

        // Then: The created event should have conference data with Zoom label
        List<Event> events = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Zoom Call");
        assertThat(events).hasSize(1);

        Event createdEvent = events.get(0);
        assertThat(createdEvent.getConferenceData()).isNotNull();
        assertThat(createdEvent.getConferenceData().getEntryPoints()).isNotEmpty();

        EntryPoint entryPoint = createdEvent.getConferenceData().getEntryPoints().get(0);
        assertThat(entryPoint.getEntryPointType()).isEqualTo("video");
        assertThat(entryPoint.getUri()).isEqualTo(zoomLink);
        assertThat(entryPoint.getLabel()).isEqualTo("Join Zoom Meeting");

        // Verify conferenceSolution
        assertThat(createdEvent.getConferenceData().getConferenceSolution()).isNotNull();
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getName()).isEqualTo("Zoom");
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getKey()).isNotNull();
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getKey().getType()).isEqualTo("addOn");

        // The link should also be in the description
        assertThat(createdEvent.getDescription()).contains("Join: " + zoomLink);
    }

    @Test
    void shouldCreateConferenceDataForMicrosoftTeams() {
        // Given: A CallEvent with a Microsoft Teams link
        String teamsLink = "https://teams.microsoft.com/l/meetup-join/19%3ameeting_abc";
        CallEvent callEvent = new CallEvent(
            "Teams Call",
            "Test Description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            teamsLink
        );

        // When: We reconcile the event
        List<CallEvent> localEvents = List.of(callEvent);
        reconciliation.reconcile(localEvents, List.of(), CALLS_CALENDAR_ID, false);

        // Then: The created event should have conference data with Teams label
        List<Event> events = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Teams Call");
        assertThat(events).hasSize(1);

        Event createdEvent = events.get(0);
        assertThat(createdEvent.getConferenceData()).isNotNull();
        assertThat(createdEvent.getConferenceData().getEntryPoints()).isNotEmpty();

        EntryPoint entryPoint = createdEvent.getConferenceData().getEntryPoints().get(0);
        assertThat(entryPoint.getEntryPointType()).isEqualTo("video");
        assertThat(entryPoint.getUri()).isEqualTo(teamsLink);
        assertThat(entryPoint.getLabel()).isEqualTo("Join Microsoft Teams Meeting");

        // Verify conferenceSolution
        assertThat(createdEvent.getConferenceData().getConferenceSolution()).isNotNull();
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getName()).isEqualTo("Microsoft Teams");
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getKey()).isNotNull();
        assertThat(createdEvent.getConferenceData().getConferenceSolution().getKey().getType()).isEqualTo("addOn");

        // The link should also be in the description
        assertThat(createdEvent.getDescription()).contains("Join: " + teamsLink);
    }

    @Test
    void shouldNotCreateConferenceDataForUnsupportedPlatform() {
        // Given: A CallEvent with a YouTube link (unsupported platform)
        String youtubeLink = "https://www.youtube.com/watch?v=abc123";
        CallEvent callEvent = new CallEvent(
            "YouTube Stream",
            "Test Description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            youtubeLink
        );

        // When: We reconcile the event
        List<CallEvent> localEvents = List.of(callEvent);
        reconciliation.reconcile(localEvents, List.of(), CALLS_CALENDAR_ID, false);

        // Then: The created event should NOT have conference data
        List<Event> events = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "YouTube Stream");
        assertThat(events).hasSize(1);

        Event createdEvent = events.get(0);
        assertThat(createdEvent.getConferenceData()).isNull();

        // But the link should still be in the description
        assertThat(createdEvent.getDescription()).contains("Join: " + youtubeLink);
    }

    @Test
    void shouldNotCreateConferenceDataForCustomLink() {
        // Given: A CallEvent with a custom link (unsupported platform)
        String customLink = "https://example.com/my-custom-meeting-room";
        CallEvent callEvent = new CallEvent(
            "Custom Meeting",
            "Test Description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            customLink
        );

        // When: We reconcile the event
        List<CallEvent> localEvents = List.of(callEvent);
        reconciliation.reconcile(localEvents, List.of(), CALLS_CALENDAR_ID, false);

        // Then: The created event should NOT have conference data
        List<Event> events = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Custom Meeting");
        assertThat(events).hasSize(1);

        Event createdEvent = events.get(0);
        assertThat(createdEvent.getConferenceData()).isNull();

        // But the link should still be in the description
        assertThat(createdEvent.getDescription()).contains("Join: " + customLink);
    }

    @Test
    void shouldAddConferenceDataWhenUpdatingEventWithLink() {
        // Given: An event exists without conference data
        Event existingEvent = mockCalendarService.createMockTimedEvent(
            "Existing Call",
            "Old description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            50,
            null  // No call link initially
        );
        mockCalendarService.addEvent(CALLS_CALENDAR_ID, existingEvent);

        // When: We update the event with a Google Meet link
        String googleMeetLink = "https://meet.google.com/xyz-abcd-efg";
        CallEvent updatedCallEvent = new CallEvent(
            "Existing Call",
            "Updated description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            googleMeetLink
        );

        List<CallEvent> localEvents = List.of(updatedCallEvent);
        List<Event> remoteEvents = List.of(existingEvent);
        reconciliation.reconcile(localEvents, remoteEvents, CALLS_CALENDAR_ID, false);

        // Then: The updated event should now have conference data
        List<Event> events = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Existing Call");
        assertThat(events).hasSize(1);

        Event updatedEvent = events.get(0);
        assertThat(updatedEvent.getConferenceData()).isNotNull();
        assertThat(updatedEvent.getConferenceData().getEntryPoints()).isNotEmpty();

        EntryPoint entryPoint = updatedEvent.getConferenceData().getEntryPoints().get(0);
        assertThat(entryPoint.getEntryPointType()).isEqualTo("video");
        assertThat(entryPoint.getUri()).isEqualTo(googleMeetLink);
        assertThat(entryPoint.getLabel()).isEqualTo("Join with Google Meet");

        // Verify conferenceSolution
        assertThat(updatedEvent.getConferenceData().getConferenceSolution()).isNotNull();
        assertThat(updatedEvent.getConferenceData().getConferenceSolution().getName()).isEqualTo("Google Meet");

        // The link should also be in the description
        assertThat(updatedEvent.getDescription()).contains("Join: " + googleMeetLink);
    }

    @Test
    void shouldUpdateConferenceDataWhenLinkChanges() {
        // Given: An event exists with a Zoom link
        String oldZoomLink = "https://zoom.us/j/111111111";
        Event existingEvent = mockCalendarService.createMockTimedEvent(
            "Team Meeting",
            "Weekly sync\n\nJoin: " + oldZoomLink,
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            50,
            oldZoomLink
        );
        mockCalendarService.addEvent(CALLS_CALENDAR_ID, existingEvent);

        // When: We update the event with a Google Meet link
        String newGoogleMeetLink = "https://meet.google.com/new-link-xyz";
        CallEvent updatedCallEvent = new CallEvent(
            "Team Meeting",
            "Weekly sync",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            newGoogleMeetLink
        );

        List<CallEvent> localEvents = List.of(updatedCallEvent);
        List<Event> remoteEvents = List.of(existingEvent);
        reconciliation.reconcile(localEvents, remoteEvents, CALLS_CALENDAR_ID, false);

        // Then: The event should have updated conference data with the new link
        List<Event> events = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Team Meeting");
        assertThat(events).hasSize(1);

        Event updatedEvent = events.get(0);
        assertThat(updatedEvent.getConferenceData()).isNotNull();
        assertThat(updatedEvent.getConferenceData().getEntryPoints()).isNotEmpty();

        EntryPoint entryPoint = updatedEvent.getConferenceData().getEntryPoints().get(0);
        assertThat(entryPoint.getEntryPointType()).isEqualTo("video");
        assertThat(entryPoint.getUri()).isEqualTo(newGoogleMeetLink);
        assertThat(entryPoint.getLabel()).isEqualTo("Join with Google Meet");

        // Verify conferenceSolution
        assertThat(updatedEvent.getConferenceData().getConferenceSolution()).isNotNull();
        assertThat(updatedEvent.getConferenceData().getConferenceSolution().getName()).isEqualTo("Google Meet");

        // The new link should be in the description
        assertThat(updatedEvent.getDescription()).contains("Join: " + newGoogleMeetLink);
    }

    @Test
    void shouldHandleMixedPlatforms() {
        // Given: Multiple events with different platform links
        CallEvent googleMeetCall = new CallEvent(
            "Google Meet Call",
            "Description 1",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(10, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        CallEvent zoomCall = new CallEvent(
            "Zoom Call",
            "Description 2",
            LocalDate.of(2026, 6, 16),
            LocalTime.of(11, 0),
            Duration.ofMinutes(50),
            "https://zoom.us/j/123456789"
        );

        CallEvent youtubeStream = new CallEvent(
            "YouTube Stream",
            "Description 3",
            LocalDate.of(2026, 6, 17),
            LocalTime.of(12, 0),
            Duration.ofMinutes(50),
            "https://www.youtube.com/watch?v=xyz"
        );

        // When: We reconcile all events
        List<CallEvent> localEvents = List.of(googleMeetCall, zoomCall, youtubeStream);
        reconciliation.reconcile(localEvents, List.of(), CALLS_CALENDAR_ID, false);

        // Then: Only supported platforms should have conference data
        List<Event> allEvents = mockCalendarService.getAllEvents(CALLS_CALENDAR_ID);
        assertThat(allEvents).hasSize(3);

        Event googleMeetEvent = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Google Meet Call").get(0);
        assertThat(googleMeetEvent.getConferenceData()).isNotNull();
        assertThat(googleMeetEvent.getConferenceData().getEntryPoints().get(0).getLabel()).isEqualTo("Join with Google Meet");
        assertThat(googleMeetEvent.getConferenceData().getConferenceSolution()).isNotNull();
        assertThat(googleMeetEvent.getConferenceData().getConferenceSolution().getName()).isEqualTo("Google Meet");

        Event zoomEvent = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Zoom Call").get(0);
        assertThat(zoomEvent.getConferenceData()).isNotNull();
        assertThat(zoomEvent.getConferenceData().getEntryPoints().get(0).getLabel()).isEqualTo("Join Zoom Meeting");
        assertThat(zoomEvent.getConferenceData().getConferenceSolution()).isNotNull();
        assertThat(zoomEvent.getConferenceData().getConferenceSolution().getName()).isEqualTo("Zoom");

        Event youtubeEvent = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "YouTube Stream").get(0);
        assertThat(youtubeEvent.getConferenceData()).isNull();
    }

    @Test
    void shouldNotUpdateEventOnSecondReconcileWhenConferenceDataAlreadySet() {
        // Given: A CallEvent with a Google Meet link
        String googleMeetLink = "https://meet.google.com/abc-defg-hij";
        CallEvent callEvent = new CallEvent(
            "Stable Call",
            "Test Description",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            googleMeetLink
        );

        // When: We reconcile the first time (creates the event)
        List<CallEvent> localEvents = List.of(callEvent);
        reconciliation.reconcile(localEvents, List.of(), CALLS_CALENDAR_ID, false);

        // Get the created event
        List<Event> createdEvents = mockCalendarService.getEventsByTitle(CALLS_CALENDAR_ID, "Stable Call");
        assertThat(createdEvents).hasSize(1);
        Event createdEvent = createdEvents.get(0);

        // Verify conferenceData was set correctly
        assertThat(createdEvent.getConferenceData()).isNotNull();
        assertThat(createdEvent.getConferenceData().getEntryPoints()).isNotEmpty();
        assertThat(createdEvent.getConferenceData().getEntryPoints().get(0).getUri()).isEqualTo(googleMeetLink);

        // When: We reconcile the second time with the same event
        List<Event> remoteEvents = mockCalendarService.getAllEvents(CALLS_CALENDAR_ID);
        List<io.quarkus.calendars.model.ReconciliationAction> actions =
            reconciliation.reconcile(localEvents, remoteEvents, CALLS_CALENDAR_ID, true); // dry-run to see actions

        // Then: No update should be needed
        assertThat(actions).isEmpty();
    }
}
