
public class Edge {
    final int u;
    final int v;
    final int weight;
    final int capacity;
    boolean blocked;

    public Edge(int u, int v, int weight, int capacity) {
        this.u = u;
        this.v = v;
        this.weight = weight;
        this.capacity = capacity;
        this.blocked = false;
    }
}
