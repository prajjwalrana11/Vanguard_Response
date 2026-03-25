import java.util.Scanner;

/**
 * Disaster Response Logistics — console demo.
 *
 * <p>Compile: {@code javac *.java} &nbsp; Run: {@code java MainSystem}
 *
 * <p>Windows: double-click {@code RUN.bat} in this folder.
 */
public class MainSystem {

    private static final String PROJECT_LINE =
            "Vanguard Response (demo) — shortest routes + max supply flow on a small network";

    static void buildDemoGraph(Graph g) {
        g.addNode(new Node(0, "Command Base", "HQ"));
        g.addNode(new Node(1, "Warehouse A", "Supply"));
        g.addNode(new Node(2, "Warehouse B", "Supply"));
        g.addNode(new Node(3, "Hospital", "Medical"));
        g.addNode(new Node(4, "Shelter North", "Shelter"));
        g.addNode(new Node(5, "Shelter South", "Shelter"));
        g.addNode(new Node(6, "Bridge", "Transit"));

        g.addEdge(0, 1, 5, 4);
        g.addEdge(0, 6, 3, 6);
        g.addEdge(0, 3, 8, 3);
        g.addEdge(1, 4, 6, 5);
        g.addEdge(1, 3, 4, 4);
        g.addEdge(2, 4, 3, 5);
        g.addEdge(2, 5, 5, 5);
        g.addEdge(3, 5, 7, 3);
        g.addEdge(4, 6, 2, 6);

        g.snapshotBaseline();
    }

    public static void main(String[] args) {
        Graph graph = new Graph();
        Logger logger = new Logger();
        buildDemoGraph(graph);
        logger.add("System started — demo map loaded.");

        try (Scanner sc = new Scanner(System.in)) {
            printWelcome();
            runConsoleMenu(graph, logger, sc);
        }
    }

    static void printWelcome() {
        ConsoleUi.blank();
        ConsoleUi.title("DISASTER RESPONSE LOGISTICS — CONSOLE DEMO");
        ConsoleUi.say("");
        ConsoleUi.say("  " + PROJECT_LINE);
        ConsoleUi.say("");
        ConsoleUi.say("  What it does:");
        ConsoleUi.say("    • Dijkstra  → fastest / cheapest route between two sites");
        ConsoleUi.say("    • Max flow  → how much aid can move (capacity limits)");
        ConsoleUi.say("    • Block road → simulate flood / bridge closure; restore or reset");
        ConsoleUi.say("");
        ConsoleUi.line();
    }

    static void runConsoleMenu(Graph g, Logger logger, Scanner sc) {
        while (true) {
            try {
                ConsoleUi.blank();
                ConsoleUi.say("MAIN MENU");
                ConsoleUi.say("  1  Show all locations (nodes)");
                ConsoleUi.say("  2  Show all roads (edges)   — weight = cost, capacity = supply");
                ConsoleUi.say("  3  Shortest path            — Dijkstra");
                ConsoleUi.say("  4  Maximum flow              — Edmonds–Karp style");
                ConsoleUi.say("  5  Block a road              — disaster / closure");
                ConsoleUi.say("  6  Restore one road         — reopen from saved map");
                ConsoleUi.say("  7  Reset entire map         — undo all blocks");
                ConsoleUi.say("  8  Show activity log");
                ConsoleUi.say("  9  GUIDED DEMO (2 min)      — good before presentation");
                ConsoleUi.say(" 10  Show network sketch");
                ConsoleUi.say("  0  Exit");
                ConsoleUi.blank();
                ConsoleUi.say("Team: Codigo Supremo  |  Course: B.Tech CSE project prototype");
                ConsoleUi.blank();
                System.out.print("Enter choice (0-10): ");

                if (!sc.hasNextInt()) {
                    ConsoleUi.say("Please type a whole number.");
                    sc.nextLine();
                    continue;
                }

                int ch = sc.nextInt();
                sc.nextLine(); // consume newline after menu number

                if (ch == 0) {
                    ConsoleUi.title("Thank you — good luck with your presentation.");
                    break;
                }

                boolean needsInputFlush = false;

                switch (ch) {
                    case 1 -> {
                        ConsoleUi.title("LOCATIONS");
                        g.printNodesToConsole();
                        logger.add("Viewed nodes.");
                    }
                    case 2 -> {
                        ConsoleUi.title("ROADS");
                        g.printEdgesToConsole();
                        logger.add("Viewed edges.");
                    }
                    case 3 -> {
                        ConsoleUi.title("SHORTEST PATH (Dijkstra)");
                        int s = askNode(sc, "Source id (0-" + (Graph.NODE_COUNT - 1) + "): ");
                        int d = askNode(sc, "Destination id: ");
                        needsInputFlush = true;
                        Graph.ShortestPathResult r = g.dijkstra(s, d);
                        if (!r.ok) {
                            ConsoleUi.say(">> " + r.message);
                        } else {
                            ConsoleUi.say(">> Path: " + pathToString(r.path));
                            ConsoleUi.say(">> Total cost (sum of weights): " + r.distance);
                        }
                        logger.add("Shortest path query " + s + " -> " + d);
                    }
                    case 4 -> {
                        ConsoleUi.title("MAXIMUM FLOW");
                        int s = askNode(sc, "Source id (where aid starts): ");
                        int t = askNode(sc, "Sink id (where aid must arrive): ");
                        needsInputFlush = true;
                        int flow = g.maxFlow(s, t);
                        if (flow < 0) {
                            ConsoleUi.say(">> Invalid node id.");
                        } else {
                            ConsoleUi.say(">> Maximum units per time (capacity network): " + flow);
                        }
                        logger.add("Max flow query " + s + " -> " + t + " = " + flow);
                    }
                    case 5 -> {
                        ConsoleUi.title("BLOCK ROAD");
                        int u = askNode(sc, "First endpoint u: ");
                        int v = askNode(sc, "Second endpoint v: ");
                        needsInputFlush = true;
                        String msg = g.blockRoad(u, v);
                        ConsoleUi.say(">> " + msg);
                        logger.add("Block " + u + "-" + v);
                    }
                    case 6 -> {
                        ConsoleUi.title("RESTORE ROAD");
                        int u = askNode(sc, "First endpoint u: ");
                        int v = askNode(sc, "Second endpoint v: ");
                        needsInputFlush = true;
                        String msg = g.restoreRoad(u, v);
                        ConsoleUi.say(">> " + msg);
                        logger.add("Restore " + u + "-" + v);
                    }
                    case 7 -> {
                        ConsoleUi.title("RESET MAP");
                        g.resetAllRoadsToBaseline();
                        ConsoleUi.say(">> All roads restored to the original demo map.");
                        logger.add("Full reset");
                    }
                    case 8 -> {
                        ConsoleUi.title("ACTIVITY LOG");
                        logger.printToConsole();
                    }
                    case 9 -> runGuidedDemo(g, logger, sc);
                    case 10 -> ConsoleUi.printMapSketch();
                    default -> ConsoleUi.say("Unknown choice. Try 0-10.");
                }

                if (needsInputFlush) {
                    sc.nextLine();
                }

                if (ch >= 1 && ch <= 10 && ch != 9) {
                    ConsoleUi.blank();
                    System.out.print("Press Enter to return to menu...");
                    sc.nextLine();
                }
            } catch (Exception e) {
                ConsoleUi.say("Something went wrong: " + (e.getMessage() != null ? e.getMessage() : e));
                sc.nextLine();
            }
        }
    }

    static int askNode(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextInt();
    }

    static String pathToString(java.util.List<Integer> path) {
        if (path == null || path.isEmpty()) {
            return "(empty)";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                b.append(" -> ");
            }
            b.append("N-").append(path.get(i));
        }
        return b.toString();
    }

    /**
     * Scripted walkthrough: shortest path, max flow, block a critical link, show impact, restore.
     */
    static void runGuidedDemo(Graph g, Logger logger, Scanner sc) {
        g.resetAllRoadsToBaseline();
        logger.add("Guided demo started (map reset to original roads).");

        ConsoleUi.blank();
        ConsoleUi.title("GUIDED DEMO — follow the story, press Enter between steps");
        ConsoleUi.say("Use this before your viva: it shows Dijkstra, max flow, and a blocked road.");

        pauseStep(sc, "\n[Step 1/6] Current locations — ");
        g.printNodesToConsole();

        pauseStep(sc, "\n[Step 2/6] Current roads — ");
        g.printEdgesToConsole();

        pauseStep(sc, "\n[Step 3/6] Shortest path: HQ (0) -> Hospital (3) — ");
        Graph.ShortestPathResult r1 = g.dijkstra(0, 3);
        ConsoleUi.say(
                r1.ok
                        ? ">> Best route: " + pathToString(r1.path) + "  |  cost = " + r1.distance
                        : ">> " + r1.message);
        logger.add("Demo: shortest 0->3");

        pauseStep(sc, "\n[Step 4/6] Max flow: Command Base (0) -> Shelter South (5) — ");
        int f1 = g.maxFlow(0, 5);
        ConsoleUi.say(">> Max flow = " + f1 + " (units limited by road capacities)");
        logger.add("Demo: max flow 0->5");

        pauseStep(
                sc,
                "\n[Step 5/6] Disaster: block N-4 <-> N-6 (Shelter North <-> Bridge). "
                        + "Press Enter to apply — ");
        ConsoleUi.say(">> " + g.blockRoad(4, 6));
        logger.add("Demo: block 4-6");

        pauseStep(sc, "\n[Step 6/6] Shortest path again: HQ (0) -> Shelter North (4) — ");
        Graph.ShortestPathResult r2 = g.dijkstra(0, 4);
        ConsoleUi.say(
                r2.ok
                        ? ">> New best route: "
                                + pathToString(r2.path)
                                + "  |  cost = "
                                + r2.distance
                                + "\n   (Before blocking 4-6, the cheap route often used the bridge link.)"
                        : ">> " + r2.message);

        pauseStep(sc, "\nCleanup: restore N-4 <-> N-6 — Press Enter — ");
        ConsoleUi.say(">> " + g.restoreRoad(4, 6));
        logger.add("Demo: restore 4-6 (end of guided tour)");

        ConsoleUi.line();
        ConsoleUi.say("Demo finished. You can run option 3 / 4 yourself with other node pairs.");
        ConsoleUi.line();
    }

    static void pauseStep(Scanner sc, String message) {
        System.out.print(message + "Press Enter...");
        sc.nextLine();
    }
}
