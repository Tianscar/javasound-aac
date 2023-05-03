package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 10.09.21
 * Time: 19:11
 */

/**
 * Type B real filter, Q[p] = 2
 */
public class Filter2 implements Filter {

    private static float[] p2_13_20 = {
            0.0f,
            0.01899487526049f,
            0.0f,
            -0.07293139167538f,
            0.0f,
            0.30596630545168f,
            0.5f
    };

    public static final Filter2 f = new Filter2(p2_13_20);

    final float[] filter;

    Filter2(float[] filter) {
        this.filter = filter;
    }

    @Override
    public int resolution() {
        return 2;
    }

    @Override
    /* real filter, size 2 */
    public int filter(int frame_len, float[][] buffer, float[][][] result) {

        for (int i = 0; i < frame_len; i++) {
            float r0 = (filter[0] * (buffer[0 + i][0] + buffer[12 + i][0]));
            float r1 = (filter[1] * (buffer[1 + i][0] + buffer[11 + i][0]));
            float r2 = (filter[2] * (buffer[2 + i][0] + buffer[10 + i][0]));
            float r3 = (filter[3] * (buffer[3 + i][0] + buffer[9 + i][0]));
            float r4 = (filter[4] * (buffer[4 + i][0] + buffer[8 + i][0]));
            float r5 = (filter[5] * (buffer[5 + i][0] + buffer[7 + i][0]));
            float r6 = (filter[6] * buffer[6 + i][0]);
            float i0 = (filter[0] * (buffer[0 + i][1] + buffer[12 + i][1]));
            float i1 = (filter[1] * (buffer[1 + i][1] + buffer[11 + i][1]));
            float i2 = (filter[2] * (buffer[2 + i][1] + buffer[10 + i][1]));
            float i3 = (filter[3] * (buffer[3 + i][1] + buffer[9 + i][1]));
            float i4 = (filter[4] * (buffer[4 + i][1] + buffer[8 + i][1]));
            float i5 = (filter[5] * (buffer[5 + i][1] + buffer[7 + i][1]));
            float i6 = (filter[6] * buffer[6 + i][1]);

            /* q = 0 */
            result[i][0][0] = r0 + r1 + r2 + r3 + r4 + r5 + r6;
            result[i][0][1] = i0 + i1 + i2 + i3 + i4 + i5 + i6;

            /* q = 1 */
            result[i][1][0] = r0 - r1 + r2 - r3 + r4 - r5 + r6;
            result[i][1][1] = i0 - i1 + i2 - i3 + i4 - i5 + i6;
        }

        return resolution();
    }
}
