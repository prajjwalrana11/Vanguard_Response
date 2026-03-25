import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Keeps the last few actions so you can see what the system did (audit trail).
 */
public class Logger {
    private static final int MAX_LOGS = 20;
    private final LinkedList<String> logs = new LinkedList<>();
    private final Object lock = new Object();

    public void add(String message) {
        String line = new Date() + " | " + message;
        synchronized (lock) {
            logs.addFirst(line);
            while (logs.size() > MAX_LOGS) {
                logs.removeLast();
            }
        }
    }

    /** Newest first. Safe copy for the API layer. */
    public List<String> snapshot() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(logs));
        }
    }

    public void printToConsole() {
        List<String> s = snapshot();
        if (s.isEmpty()) {
            System.out.println("(no actions logged yet)");
            return;
        }
        System.out.println("Recent actions (newest first):");
        System.out.println("-".repeat(56));
        for (String line : s) {
            System.out.println(" • " + line);
        }
        System.out.println("-".repeat(56));
    }
}
