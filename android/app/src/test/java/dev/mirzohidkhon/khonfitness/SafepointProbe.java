package dev.mirzohidkhon.khonfitness;

/** Blocks a safepoint: a compiled counted loop with no safepoint poll spins while the main thread asks for a GC. */
public class SafepointProbe {
    static volatile double sink = 1.1;

    static double block(int count, double value) {
        for (int i = 0; i < count; i++) {
            value = Math.sin(value) + Math.cos(value) + (i & 1) * 0.1;
            value = Math.sin(value) + Math.cos(value);
            value = Math.sin(value) + Math.cos(value);
        }
        return value;
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20000; i++) sink = block(32, sink);
        Thread blocker = new Thread(() -> sink = block(Integer.MAX_VALUE, sink), "safepoint-probe");
        blocker.start();
        Thread.sleep(500L);
        System.gc();
        blocker.join();
    }
}
