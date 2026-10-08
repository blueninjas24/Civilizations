package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PopulationDebugCommandTest {
    @TempDir Path directory;
    private final List<String> messages = new ArrayList<>();
    private CommandSender sender(boolean allowed) {
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> allowed;
                    case "sendMessage" -> { messages.add((String) args[0]); yield null; }
                    default -> throw new AssertionError("Unexpected server access: " + method.getName());
                });
    }
    private PopulationSession session() {
        return new PopulationSession(directory.toFile(), Logger.getLogger("DebugCommandTest"),
                PopulationSession.Settings.prototype(), System::currentTimeMillis);
    }
    @Test void nonAdminsCannotReadOrCompleteDiagnostics() {
        try (var session = session()) {
            var command = new PopulationDebugCommand(session);
            command.onCommand(sender(false), null, "civ", new String[]{"debug", "status"});
            assertEquals(List.of("You do not have permission to inspect Civilizations."), messages);
            assertTrue(command.onTabComplete(sender(false), null, "civ", new String[]{"debug", ""}).isEmpty());
        }
    }
    @Test void startupStatusAndSettlementRequestsNeverWaitOrOpenDatabase() {
        try (var session = session()) {
            var command = new PopulationDebugCommand(session);
            command.onCommand(sender(true), null, "civ", new String[]{"debug", "status"});
            assertTrue(messages.contains("Civilizations population: not ready")); messages.clear();
            command.onCommand(sender(true), null, "civ", new String[]{"debug", "settlement", "00000000-0000-0000-0000-000000000001"});
            assertEquals(List.of("Population is still starting; retry shortly."), messages);
            assertFalse(directory.resolve("civilizations.db").toFile().exists());
        }
    }
    @Test void malformedIdsAndMutationRequestsHaveNoSideEffects() {
        try (var session = session()) {
            var command = new PopulationDebugCommand(session);
            command.onCommand(sender(true), null, "civ", new String[]{"debug", "settlement", "bad-id"});
            assertEquals(List.of("Settlement ID must be a UUID."), messages); messages.clear();
            command.onCommand(sender(true), null, "civ", new String[]{"debug", "simulate", "1000"});
            assertEquals(List.of("Usage: /civ debug status | observation | settlement <uuid> | explain <uuid> | economy <uuid>"), messages);
            assertEquals(0, session.status().targetMonths());
            assertEquals(List.of("status", "settlement"),
                    command.onTabComplete(sender(true), null, "civ", new String[]{"debug", "s"}));
        }
    }
    @Test void observationReportsItsMainThreadAdapterWithoutWorkerOrDatabaseAccess() {
        try (var session = session()) {
            new PopulationDebugCommand(session, () -> "Partial frame: housing unknown")
                    .onCommand(sender(true), null, "civ", new String[]{"debug", "observation"});
            assertEquals(List.of("Partial frame: housing unknown"), messages);
        }
    }
    @Test void explanationRespectsPermissionsAndDoesNotReadDatabaseOnServerThread() {
        try (var session = session()) {
            var command = new PopulationDebugCommand(session);
            command.onCommand(sender(false), null, "civ", new String[]{"debug", "explain", "bad-id"});
            assertEquals(List.of("You do not have permission to inspect Civilizations."), messages); messages.clear();
            command.onCommand(sender(true), null, "civ", new String[]{"debug", "explain", "bad-id"});
            assertEquals(List.of("Settlement ID must be a UUID."), messages); messages.clear();
            command.onCommand(sender(true), null, "civ", new String[]{"debug", "explain", "00000000-0000-0000-0000-000000000001"});
            assertEquals(List.of("Population is still starting; retry shortly."), messages);
            assertFalse(directory.resolve("civilizations.db").toFile().exists());
            assertEquals(List.of("explain", "economy"), command.onTabComplete(sender(true), null, "civ", new String[]{"debug", "e"}));
        }
    }
}
