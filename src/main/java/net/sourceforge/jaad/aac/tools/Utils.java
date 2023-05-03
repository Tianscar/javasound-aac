package net.sourceforge.jaad.aac.tools;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 27.12.20
 * Time: 18:59
 */
public interface Utils {

    static boolean[] copyOf(boolean[] array) {
        return array==null ? null : java.util.Arrays.copyOf(array, array.length);
    }

    static int[] copyOf(int[] array) {
        return array==null ? null : java.util.Arrays.copyOf(array, array.length);
    }

    static float[] copyOf(float[] array) {
        return array==null ? null : java.util.Arrays.copyOf(array, array.length);
    }

    static void copyRange(int[] array, int srcPos, int destPos, int length) {
        System.arraycopy(array, srcPos, array, destPos, length);
    }

    static int clip(int idx, int min, int max) {
        idx = Math.max(idx, min);
        idx = Math.min(idx, max);
        return idx;
    }
}
