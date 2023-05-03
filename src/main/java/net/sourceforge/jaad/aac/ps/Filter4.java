package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 10.09.21
 * Time: 19:11
 */

/**
 * Type A complex filter, Q[p] = 4
 */
public class Filter4 implements Filter {

	private static final float[] p4_13_34 = {
			-0.05908211155639f,
			-0.04871498374946f,
			0.0f,
			0.07778723915851f,
			0.16486303567403f,
			0.23279856662996f,
			0.25f
	};

	public static final Filter4 f = new Filter4(p4_13_34);

	final float[] filter;

 	Filter4(float[] filter) {
     this.filter = filter;
 }

	@Override
 	public int resolution() {
     	return 4;
 	}

	@Override
	/* real filter, size 2 */
	public int filter(int frame_len, float[][] buffer, float[][][] result) {

		float[] input_re1 = new float[2], input_re2 = new float[2];
		float[] input_im1 = new float[2], input_im2 = new float[2];

		for (int i = 0; i < frame_len; i++) {
			input_re1[0] = -(filter[2] * (buffer[i + 2][0] + buffer[i + 10][0]))
					+ (filter[6] * buffer[i + 6][0]);
			input_re1[1] = (-0.70710678118655f
					* ((filter[1] * (buffer[i + 1][0] + buffer[i + 11][0]))
					+ (filter[3] * (buffer[i + 3][0] + buffer[i + 9][0]))
					- (filter[5] * (buffer[i + 5][0] + buffer[i + 7][0]))));

			input_im1[0] = (filter[0] * (buffer[i + 0][1] - buffer[i + 12][1]))
					- (filter[4] * (buffer[i + 4][1] - buffer[i + 8][1]));
			input_im1[1] = (0.70710678118655f
					* ((filter[1] * (buffer[i + 1][1] - buffer[i + 11][1]))
					- (filter[3] * (buffer[i + 3][1] - buffer[i + 9][1]))
					- (filter[5] * (buffer[i + 5][1] - buffer[i + 7][1]))));

			input_re2[0] = (filter[0] * (buffer[i + 0][0] - buffer[i + 12][0]))
					- (filter[4] * (buffer[i + 4][0] - buffer[i + 8][0]));
			input_re2[1] = (0.70710678118655f
					* ((filter[1] * (buffer[i + 1][0] - buffer[i + 11][0]))
					- (filter[3] * (buffer[i + 3][0] - buffer[i + 9][0]))
					- (filter[5] * (buffer[i + 5][0] - buffer[i + 7][0]))));

			input_im2[0] = -(filter[2] * (buffer[i + 2][1] + buffer[i + 10][1]))
					+ (filter[6] * buffer[i + 6][1]);
			input_im2[1] = (-0.70710678118655f
					* ((filter[1] * (buffer[i + 1][1] + buffer[i + 11][1]))
					+ (filter[3] * (buffer[i + 3][1] + buffer[i + 9][1]))
					- (filter[5] * (buffer[i + 5][1] + buffer[i + 7][1]))));

			/* q == 0 */
			result[i][0][0] = input_re1[0] + input_re1[1] + input_im1[0] + input_im1[1];
			result[i][0][1] = -input_re2[0] - input_re2[1] + input_im2[0] + input_im2[1];

			/* q == 1 */
			result[i][1][0] = input_re1[0] - input_re1[1] - input_im1[0] + input_im1[1];
			result[i][1][1] = input_re2[0] - input_re2[1] + input_im2[0] - input_im2[1];

			/* q == 2 */
			result[i][2][0] = input_re1[0] - input_re1[1] + input_im1[0] - input_im1[1];
			result[i][2][1] = -input_re2[0] + input_re2[1] + input_im2[0] - input_im2[1];

			/* q == 3 */
			result[i][3][0] = input_re1[0] + input_re1[1] - input_im1[0] - input_im1[1];
			result[i][3][1] = input_re2[0] + input_re2[1] + input_im2[0] + input_im2[1];
		}

		return resolution();
	}
}
