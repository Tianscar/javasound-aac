package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 10.09.21
 * Time: 19:11
 */

/**
 * Type A complex filter, Q[p] = 12
 */
public class Filter12 implements Filter {

    private static final float[] p12_13_34 = {
            0.04081179924692f,
            0.03812810994926f,
            0.05144908135699f,
            0.06399831151592f,
            0.07428313801106f,
            0.08100347892914f,
            0.08333333333333f
    };

    public static final Filter12 f = new Filter12(p12_13_34);

    final float[] filter;

    Filter12(float[] filter) {
        this.filter = filter;
    }

    @Override
    public int resolution() {
        return 12;
    }

    @Override
    /* real filter, size 2 */
    public int filter(int frame_len, float[][] buffer, float[][][] result) {
        float[] input_re1 = new float[6], input_re2 = new float[6];
        float[] input_im1 = new float[6], input_im2 = new float[6];
        float[] out_re1 = new float[6], out_re2 = new float[6];
        float[] out_im1 = new float[6], out_im2 = new float[6];

        for (int i = 0; i < frame_len; i++) {
            for (int n = 0; n < 6; n++) {
                if (n == 0) {
                    input_re1[0] = (buffer[6 + i][0] * filter[6]);
                    input_re2[0] = (buffer[6 + i][1] * filter[6]);
                } else {
                    input_re1[6 - n] = ((buffer[n + i][0] + buffer[12 - n + i][0]) * filter[n]);
                    input_re2[6 - n] = ((buffer[n + i][1] + buffer[12 - n + i][1]) * filter[n]);
                }
                input_im2[n] = ((buffer[n + i][0] - buffer[12 - n + i][0]) * filter[n]);
                input_im1[n] = ((buffer[n + i][1] - buffer[12 - n + i][1]) * filter[n]);
            }

            DCT3_6_unscaled(out_re1, input_re1);
            DCT3_6_unscaled(out_re2, input_re2);

            DCT3_6_unscaled(out_im1, input_im1);
            DCT3_6_unscaled(out_im2, input_im2);

            for (int n = 0; n < 6; n += 2) {
                result[i][n][0] = out_re1[n] - out_im1[n];
                result[i][n][1] = out_re2[n] + out_im2[n];
                result[i][n + 1][0] = out_re1[n + 1] + out_im1[n + 1];
                result[i][n + 1][1] = out_re2[n + 1] - out_im2[n + 1];

                result[i][10 - n][0] = out_re1[n + 1] - out_im1[n + 1];
                result[i][10 - n][1] = out_re2[n + 1] + out_im2[n + 1];
                result[i][11 - n][0] = out_re1[n] + out_im1[n];
                result[i][11 - n][1] = out_re2[n] - out_im2[n];
            }
        }

        return resolution();
    }

    static void DCT3_6_unscaled(float[] y, float[] x) {
        float f0, f1, f2, f3, f4, f5, f6, f7;

        f0 = (x[3] * 0.70710678118655f);
        f1 = x[0] + f0;
        f2 = x[0] - f0;
        f3 = ((x[1] - x[5]) * 0.70710678118655f);
        f4 = (x[2] * 0.86602540378444f) + (x[4] * 0.5f);
        f5 = f4 - x[4];
        f6 = (x[1] * 0.96592582628907f) + (x[5] * 0.25881904510252f);
        f7 = f6 - f3;
        y[0] = f1 + f6 + f4;
        y[1] = f2 + f3 - x[4];
        y[2] = f7 + f2 - f5;
        y[3] = f1 - f7 - f5;
        y[4] = f1 - f3 - x[4];
        y[5] = f2 - f6 + f4;
    }
}
