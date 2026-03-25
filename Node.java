/**
 * One location in the disaster-aid network (HQ, warehouse, hospital, etc.).
 */
public class Node {
    final int id;
    final String name;
    /** e.g. "HQ", "Supply", "Medical" */
    final String type;
    /** If false, the node is treated as unavailable (for future use / demos). */
    boolean active;

    public Node(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.active = true;
    }
}
