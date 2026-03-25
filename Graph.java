import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Simple undirected graph stored as adjacency + capacity matrices.
 * - weight = road length / time (used by Dijkstra)
 * - capacity = how much supply can pass (used by max flow)
 */
public class Graph {
    public static final int NODE_COUNT = 7;

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    /** Current usable graph (blocked roads become 0). */
    private final int[][] adj = new int[NODE_COUNT][NODE_COUNT];
    private final int[][] capacity = new int[NODE_COUNT][NODE_COUNT];

    /** Copy from the last {@link #snapshotBaseline()} — used for reset / unblock. */
    private int[][] baselineAdj;
    private int[][] baselineCap;

    public Graph() {
        for (int i = 0; i < NODE_COUNT; i++) {
            Arrays.fill(adj[i], 0);
            Arrays.fill(capacity[i], 0);
        }
    }

    /** Validates id in range [0, NODE_COUNT). Returns null if OK, else a short error for the UI. */
    public static String validateNodeId(int id) {
        if (id < 0 || id >= NODE_COUNT) {
            return "Node id must be between 0 and " + (NODE_COUNT - 1) + " (you entered " + id + ").";
        }
        return null;
    }

    public synchronized void addNode(Node n) {
        nodes.add(n);
    }

    /**
     * Undirected edge: sets both adj[u][v] and adj[v][u] (same for capacity).
     */
    public synchronized void addEdge(int u, int v, int weight, int cap) {
        if (validateNodeId(u) != null || validateNodeId(v) != null) {
            throw new IllegalArgumentException("Invalid node id for edge.");
        }
        if (u == v) {
            throw new IllegalArgumentException("Self-loop edges are not supported.");
        }
        edges.add(new Edge(u, v, weight, cap));
        adj[u][v] = weight;
        adj[v][u] = weight;
        capacity[u][v] = cap;
        capacity[v][u] = cap;
    }

    /** Call after you finish building the map — remembers weights/caps for reset/unblock. */
    public synchronized void snapshotBaseline() {
        baselineAdj = copyMatrix(adj);
        baselineCap = copyMatrix(capacity);
    }

    /** Restores every road to the last baseline (undo all blocks since snapshot). */
    public synchronized void resetAllRoadsToBaseline() {
        if (baselineAdj == null) {
            return;
        }
        for (int i = 0; i < NODE_COUNT; i++) {
            adj[i] = Arrays.copyOf(baselineAdj[i], NODE_COUNT);
            capacity[i] = Arrays.copyOf(baselineCap[i], NODE_COUNT);
        }
        for (Edge e : edges) {
            e.blocked = false;
        }
    }

    /**
     * Marks a road as blocked (no travel, no flow).
     * @return a user-facing message (may be a warning if the pair was not found)
     */
    public synchronized String blockRoad(int u, int v) {
        if (validateNodeId(u) != null) {
            return validateNodeId(u);
        }
        if (validateNodeId(v) != null) {
            return validateNodeId(v);
        }
        boolean found = false;
        for (Edge e : edges) {
            if ((e.u == u && e.v == v) || (e.u == v && e.v == u)) {
                found = true;
                e.blocked = true;
                adj[u][v] = 0;
                adj[v][u] = 0;
                capacity[u][v] = 0;
                capacity[v][u] = 0;
            }
        }
        if (!found) {
            return "Warning: No road exists between N-" + u + " and N-" + v + " (nothing changed).";
        }
        return "Road N-" + u + " <-> N-" + v + " is now BLOCKED.";
    }

    /** Re-opens a road using the last saved baseline weights/capacities. */
    public synchronized String restoreRoad(int u, int v) {
        if (baselineAdj == null) {
            return "Warning: Baseline not set — call snapshot after building the graph.";
        }
        if (validateNodeId(u) != null) {
            return validateNodeId(u);
        }
        if (validateNodeId(v) != null) {
            return validateNodeId(v);
        }
        int w = baselineAdj[u][v];
        int c = baselineCap[u][v];
        if (w <= 0 && c <= 0) {
            return "Warning: This edge was never in the original map.";
        }
        boolean found = false;
        for (Edge e : edges) {
            if ((e.u == u && e.v == v) || (e.u == v && e.v == u)) {
                found = true;
                e.blocked = false;
            }
        }
        if (!found) {
            return "Warning: No matching edge record (map may be inconsistent).";
        }
        adj[u][v] = w;
        adj[v][u] = w;
        capacity[u][v] = c;
        capacity[v][u] = c;
        return "Road N-" + u + " <-> N-" + v + " is OPEN again.";
    }

    public synchronized List<Node> getNodesView() {
        return new ArrayList<>(nodes);
    }

    public synchronized List<Edge> getEdgesView() {
        return new ArrayList<>(edges);
    }

    public synchronized void printNodesToConsole() {
        System.out.printf("%-5s %-22s %-12s %s%n", "ID", "Location", "Type", "Status");
        System.out.println("-".repeat(56));
        for (Node n : nodes) {
            System.out.printf(
                    "N-%-3d %-22s %-12s %s%n",
                    n.id, n.name, n.type, n.active ? "Active" : "Down");
        }
        System.out.println("-".repeat(56));
        System.out.println("Tip: use these ids (0–" + (NODE_COUNT - 1) + ") in path / flow / block commands.");
    }

    public synchronized void printEdgesToConsole() {
        System.out.printf("%-16s %8s %10s  %s%n", "Connection", "Weight", "Capacity", "Status");
        System.out.println("-".repeat(56));
        for (Edge e : edges) {
            String ends = "N-" + e.u + " <-> N-" + e.v;
            String status = e.blocked ? "BLOCKED (no travel / flow)" : "OPEN";
            System.out.printf("%-16s %8d %10d  %s%n", ends, e.weight, e.capacity, status);
        }
        System.out.println("-".repeat(56));
        int open = 0;
        for (Edge e : edges) {
            if (!e.blocked) {
                open++;
            }
        }
        System.out.println("Open roads: " + open + " / " + edges.size());
    }

    /** Result of shortest-path run — easy for the web layer to turn into JSON. */
    public static final class ShortestPathResult {
        public final boolean ok;
        public final List<Integer> path;
        public final int distance;
        public final String message;

        ShortestPathResult(boolean ok, List<Integer> path, int distance, String message) {
            this.ok = ok;
            this.path = path;
            this.distance = distance;
            this.message = message;
        }
    }

    /**
     * Dijkstra on non-negative weights. Blocked roads have weight 0 in {@code adj}, so they are skipped.
     */
    public synchronized ShortestPathResult dijkstra(int src, int dst) {
        String err = validateNodeId(src);
        if (err != null) {
            return new ShortestPathResult(false, List.of(), 0, err);
        }
        err = validateNodeId(dst);
        if (err != null) {
            return new ShortestPathResult(false, List.of(), 0, err);
        }

        int[] dist = new int[NODE_COUNT];
        int[] parent = new int[NODE_COUNT];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        dist[src] = 0;
        pq.add(new int[] {0, src});

        while (!pq.isEmpty()) {
            int u = pq.poll()[1];

            for (int v = 0; v < NODE_COUNT; v++) {
                int w = adj[u][v];
                if (w > 0 && dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    parent[v] = u;
                    pq.add(new int[] {dist[v], v});
                }
            }
        }

        if (dist[dst] == Integer.MAX_VALUE) {
            return new ShortestPathResult(
                    false,
                    List.of(),
                    0,
                    "No path found — roads may be blocked or disconnected. Try RESET MAP or RESTORE ROAD.");
        }

        LinkedList<Integer> path = new LinkedList<>();
        for (int v = dst; v != -1; v = parent[v]) {
            path.addFirst(v);
        }
        return new ShortestPathResult(true, new ArrayList<>(path), dist[dst], "OK");
    }

    private boolean bfsMaxFlow(int s, int t, int[] parent, int[][] residual) {
        Arrays.fill(parent, -1);
        Queue<Integer> q = new LinkedList<>();
        q.add(s);
        parent[s] = -2;

        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v = 0; v < NODE_COUNT; v++) {
                if (parent[v] == -1 && residual[u][v] > 0) {
                    parent[v] = u;
                    if (v == t) {
                        return true;
                    }
                    q.add(v);
                }
            }
        }
        return false;
    }

    /** Edmonds–Karp style max flow on the current capacity matrix. */
    public synchronized int maxFlow(int source, int sink) {
        if (validateNodeId(source) != null || validateNodeId(sink) != null) {
            return -1;
        }

        int[][] residual = new int[NODE_COUNT][NODE_COUNT];
        for (int i = 0; i < NODE_COUNT; i++) {
            residual[i] = Arrays.copyOf(capacity[i], NODE_COUNT);
        }

        int[] parent = new int[NODE_COUNT];
        int total = 0;

        while (bfsMaxFlow(source, sink, parent, residual)) {
            int pathFlow = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, residual[u][v]);
            }
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                residual[u][v] -= pathFlow;
                residual[v][u] += pathFlow;
            }
            total += pathFlow;
        }
        return total;
    }

    private static int[][] copyMatrix(int[][] m) {
        int[][] out = new int[NODE_COUNT][NODE_COUNT];
        for (int i = 0; i < NODE_COUNT; i++) {
            out[i] = Arrays.copyOf(m[i], NODE_COUNT);
        }
        return out;
    }
}
