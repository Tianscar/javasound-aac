package net.sourceforge.jaad.aac.filterbank;

import net.sourceforge.jaad.aac.AACException;

import java.util.Arrays;

class FFT implements FFTTables {

	private final int length;
	private final float[][] roots;
	private final float[][] rev;

	FFT(int length) {
		this.length = length;

		switch(length) {
			case 64:
				roots = FFT_TABLE_64;
				break;
			case 512:
				roots = FFT_TABLE_512;
				break;
			case 60:
				roots = FFT_TABLE_60;
				break;
			case 480:
				roots = FFT_TABLE_480;
				break;
			default:
				throw new AACException("unexpected FFT length: "+length);
		}

		//processing buffers
		rev = new float[length][2];
	}

	void processForward(float[][] in) {
		process(in, true);
	}

	public static void dump(float[][] in) {
		for(int i=0; i<in.length; ++i) {
			float[] c = in[i];
			System.out.format("cpx(%f, %f),\n", c[0], c[1]);
		}
	}

	void process(float[][] in, boolean forward) {

		//bit-reversal
		int ii = 0;
		for(int i = 0; i<length; i++) {
			rev[i][0] = in[ii][0];
			rev[i][1] = in[ii][1];
			int k = length>>1;
			while(ii>=k&&k>0) {
				ii -= k;
				k >>= 1;
			}
			ii += k;
		}

		for(int i = 0; i<length; i++) {
			in[i][0] = rev[i][0];
			in[i][1] = rev[i][1];
		}

		//bottom base-4 round
		for(int i = 0; i<length; i += 4) {
			// a = i0 + i1
			float aRe = in[i][0]+in[i+1][0];
			float aIm = in[i][1]+in[i+1][1];
			// b = i2 + i3
			float bRe = in[i+2][0]+in[i+3][0];
			float bIm = in[i+2][1]+in[i+3][1];
			// c = i0 - i1
			float cRe = in[i][0]-in[i+1][0];
			float cIm = in[i][1]-in[i+1][1];
			// d = i2 - i3
			float dRe = in[i+2][0]-in[i+3][0];
			float dIm = in[i+2][1]-in[i+3][1];

			in[i][0] = aRe+bRe;
			in[i][1] = aIm+bIm;

			in[i+2][0] = aRe-bRe;
			in[i+2][1] = aIm-bIm;

			// e1 = c + i*d
			float e1Re = cRe-dIm;
			float e1Im = cIm+dRe;
			// e2 = c - i*d
			float e2Re = cRe+dIm;
			float e2Im = cIm-dRe;

			if(forward) {
				in[i+1][0] = e2Re;
				in[i+1][1] = e2Im;
				in[i+3][0] = e1Re;
				in[i+3][1] = e1Im;
			}
			else {
				in[i+1][0] = e1Re;
				in[i+1][1] = e1Im;
				in[i+3][0] = e2Re;
				in[i+3][1] = e2Im;
			}
		}

		final int imOff = (forward ? 2 : 1);

		//iterations from bottom to top
		for(int i = 4; i<length; i <<= 1) {
			final int shift = i<<1;
			final int m = length/shift;
			for(int j = 0; j<length; j += shift) {
				for(int k = 0; k<i; k++) {
					int km = k*m;
					float rootRe = roots[km][0];
					float rootIm = roots[km][imOff];

					float[] v0 = in[j + k];
					float[] v1 = in[i + k + j];
					
					float zRe = v1[0]*rootRe-v1[1]*rootIm;
					float zIm = v1[0]*rootIm+v1[1]*rootRe;

					v1[0] = v0[0]-zRe;
					v1[1] = v0[1]-zIm;
					v0[0] = v0[0]+zRe;
					v0[1] = v0[1]+zIm;
				}
			}
		}
	}

	public static float[][] copyOf(float[][] array) {
		float[][] result = new float[array.length][2];
		for(int i=0; i<array.length; ++i) {
			result[i] = Arrays.copyOf(array[i], array[i].length);
		}

		return result;
	}
}
