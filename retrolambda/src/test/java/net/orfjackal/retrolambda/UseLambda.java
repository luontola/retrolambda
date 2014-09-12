package net.orfjackal.retrolambda;

/**
 *
 * @author Jaroslav Tulach
 */
public final class UseLambda {

    public static void invoke(int times, Runnable r) {
        for (int i = 0; i < times; i++) {
            r.run();
        }
    }

    public static int fourtyTwo() {
        int[] arr = {0};
        invoke(21, () -> arr[0] += 2);
        return arr[0];
    }
    
}
