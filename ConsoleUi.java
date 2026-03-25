final class ConsoleUi {
    static final int WIDTH = 64;

    private ConsoleUi() {}

    static void line() {
        System.out.println("-".repeat(WIDTH));
    }

    static void title(String text) {
        line();
        System.out.println("  " + text);
        line();
    }

    static void blank() {
        System.out.println();
    }

    static void say(String text) {
        System.out.println(text);
    }

    static void printMapSketch() {
        blank();
        title("Network sketch (same data as inside the program)");
        System.out.println(
                """
                          [0] Command Base (HQ)
                         / |\\
                        /  | \\
                       /   |   \\____ [6] Bridge
                      /    |         /
                     v     v       v
               [1] Wh-A  [3] Hospital    [4] Shelter North
                |       /  \\____________/
                |      /                    \\
                v     v                      |
               [4]----+                        |
                \\                            /
                 \\______ [2] Wh-B __ [5] Shelter South
                (edges are undirected; see menu 2 for weights)""");
        line();
    }
}
