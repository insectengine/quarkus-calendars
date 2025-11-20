package io.quarkus.calendars;

import io.quarkus.calendars.command.CheckFormatCommand;
import io.quarkus.calendars.command.ReconcileCommand;
import io.quarkus.calendars.command.ReleaseSyncCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "quarkus-calendar",
    mixinStandardHelpOptions = true,
    version = "1.0.0-SNAPSHOT",
    description = "Quarkus Calendar Manager - Manage calendar events through YAML files",
    subcommands = {
        CheckFormatCommand.class,
        ReconcileCommand.class,
        ReleaseSyncCommand.class
    }
)
public class QuarkusCalendarCommand {
}
