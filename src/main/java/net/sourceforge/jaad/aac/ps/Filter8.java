package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 10.09.21
 * Time: 19:11
 */

/**
 *  Type A complex filter, Q[p] = 8
 */
public class Filter8 implements Filter {

	/* tables */
 /* filters are mirrored in coef 6, second half left out */
 private static final float[] p8_13_20 = {
         0.00746082949812f,
         0.02270420949825f,
         0.04546865930473f,
         0.07266113929591f,
         0.09885108575264f,
         0.11793710567217f,
         0.125f
 };

 private static final float[] p8_13_34 = {
         0.01565675600122f,
         0.03752716391991f,
         0.05417891378782f,
         0.08417044116767f,
         0.10307344158036f,
         0.12222452249753f,
         0.125f
 	};

	public static final Filter8 f20 = new Filter8(p8_13_20);

 	public static final Filter8 f34 = new Filter8(p8_13_34);

	final float[] filter;

 	Filter8(float[] filter) {
     this.filter = filter;
 }

	@Override
 	public int resolution() {
     	return 8;
 	}

	@Override
	/* real filter, size 2 */
	public int filter(int frame_len, float[][] buffer, float[][][] result) {

		float[] input_re1 = new float[4], input_re2 = new float[4];
		float[] input_im1 = new float[4], input_im2 = new float[4];
		float[] x = new float[4];

		for(int i = 0; i<frame_len; i++) {
			input_re1[0] = (filter[6]*buffer[6+i][0]);
			input_re1[1] = (filter[5]*(buffer[5+i][0]+buffer[7+i][0]));
			input_re1[2] = -(filter[0]*(buffer[0+i][0]+buffer[12+i][0]))+(filter[4]*(buffer[4+i][0]+buffer[8+i][0]));
			input_re1[3] = -(filter[1]*(buffer[1+i][0]+buffer[11+i][0]))+(filter[3]*(buffer[3+i][0]+buffer[9+i][0]));

			input_im1[0] = (filter[5]*(buffer[7+i][1]-buffer[5+i][1]));
			input_im1[1] = (filter[0]*(buffer[12+i][1]-buffer[0+i][1]))+(filter[4]*(buffer[8+i][1]-buffer[4+i][1]));
			input_im1[2] = (filter[1]*(buffer[11+i][1]-buffer[1+i][1]))+(filter[3]*(buffer[9+i][1]-buffer[3+i][1]));
			input_im1[3] = (filter[2]*(buffer[10+i][1]-buffer[2+i][1]));

			for(int n = 0; n<4; n++) {
				x[n] = input_re1[n]-input_im1[3-n];
			}
			DCT3_4_unscaled(x, x);
			result[i][7][0] = x[0];
			result[i][5][0] = x[2];
			result[i][3][0] = x[3];
			result[i][1][0] = x[1];

			for(int n = 0; n<4; n++) {
				x[n] = input_re1[n]+input_im1[3-n];
			}
			DCT3_4_unscaled(x, x);
			result[i][6][0] = x[1];
			result[i][4][0] = x[3];
			result[i][2][0] = x[2];
			result[i][0][0] = x[0];

			input_im2[0] = (filter[6]*buffer[6+i][1]);
			input_im2[1] = (filter[5]*(buffer[5+i][1]+buffer[7+i][1]));
			input_im2[2] = -(filter[0]*(buffer[0+i][1]+buffer[12+i][1]))+(filter[4]*(buffer[4+i][1]+buffer[8+i][1]));
			input_im2[3] = -(filter[1]*(buffer[1+i][1]+buffer[11+i][1]))+(filter[3]*(buffer[3+i][1]+buffer[9+i][1]));

			input_re2[0] = (filter[5]*(buffer[7+i][0]-buffer[5+i][0]));
			input_re2[1] = (filter[0]*(buffer[12+i][0]-buffer[0+i][0]))+(filter[4]*(buffer[8+i][0]-buffer[4+i][0]));
			input_re2[2] = (filter[1]*(buffer[11+i][0]-buffer[1+i][0]))+(filter[3]*(buffer[9+i][0]-buffer[3+i][0]));
			input_re2[3] = (filter[2]*(buffer[10+i][0]-buffer[2+i][0]));

			for(int n = 0; n<4; n++) {
				x[n] = input_im2[n]+input_re2[3-n];
			}
			DCT3_4_unscaled(x, x);
			result[i][7][1] = x[0];
			result[i][5][1] = x[2];
			result[i][3][1] = x[3];
			result[i][1][1] = x[1];

			for(int n = 0; n<4; n++) {
				x[n] = input_im2[n]-input_re2[3-n];
			}
			DCT3_4_unscaled(x, x);
			result[i][6][1] = x[1];
			result[i][4][1] = x[3];
			result[i][2][1] = x[2];
			result[i][0][1] = x[0];
		}

		return resolution();
	}

	static void DCT3_4_unscaled(float[] y, float[] x) {
			float f0, f1, f2, f3, f4, f5, f6, f7, f8;

			f0 = (x[2]*0.7071067811865476f);
			f1 = x[0]-f0;
			f2 = x[0]+f0;
			f3 = x[1]+x[3];
			f4 = (x[1]*1.3065629648763766f);
			f5 = (f3*(-0.9238795325112866f));
			f6 = (x[3]*(-0.5411961001461967f));
			f7 = f4+f5;
			f8 = f6-f5;
			y[3] = f2-f8;
			y[0] = f2+f8;
			y[2] = f1-f7;
			y[1] = f1+f7;
		}
}
