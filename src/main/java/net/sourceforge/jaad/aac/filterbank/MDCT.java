package net.sourceforge.jaad.aac.filterbank;

import net.sourceforge.jaad.aac.AACException;

class MDCT implements MDCTTables {

	private final int N, N2, N4, N8;
	private final float[][] sincos;
	private final FFT fft;
	private final float[][] buf;

	MDCT(int length) {
		N = length;
		N2 = length>>1;
		N4 = length>>2;
		N8 = length>>3;
		switch(length) {
			case 2048:
				sincos = MDCT_TABLE_2048;
				break;
			case 256:
				sincos = MDCT_TABLE_128;
				break;
			case 1920:
				sincos = MDCT_TABLE_1920;
				break;
			case 240:
				sincos = MDCT_TABLE_240;
			default:
				throw new AACException("unsupported MDCT length: "+length);
		}
		fft = new FFT(N4);
		buf = new float[N4][2];
	}

	void process(float[] in, int inOff, float[] out, int outOff) {

		//pre-IFFT complex multiplication
		for(int k = 0; k<N4; k++) {
			buf[k][1] = (in[inOff+2*k]*sincos[k][0])+(in[inOff+N2-1-2*k]*sincos[k][1]);
			buf[k][0] = (in[inOff+N2-1-2*k]*sincos[k][0])-(in[inOff+2*k]*sincos[k][1]);
		}

		//complex IFFT, non-scaling
		fft.process(buf, false);

		//post-IFFT complex multiplication
		for(int k = 0; k<N4; k++) {
			float t0 = buf[k][0];
			float t1 = buf[k][1];
			buf[k][1] = (t1*sincos[k][0])+(t0*sincos[k][1]);
			buf[k][0] = (t0*sincos[k][0])-(t1*sincos[k][1]);
		}

		//reordering
		for(int k = 0; k<N8; k += 2) {
			out[outOff+2*k] = buf[N8+k][1];
			out[outOff+2+2*k] = buf[N8+1+k][1];

			out[outOff+1+2*k] = -buf[N8-1-k][0];
			out[outOff+3+2*k] = -buf[N8-2-k][0];

			out[outOff+N4+2*k] = buf[k][0];
			out[outOff+N4+2+2*k] = buf[1+k][0];

			out[outOff+N4+1+2*k] = -buf[N4-1-k][1];
			out[outOff+N4+3+2*k] = -buf[N4-2-k][1];

			out[outOff+N2+2*k] = buf[N8+k][0];
			out[outOff+N2+2+2*k] = buf[N8+1+k][0];

			out[outOff+N2+1+2*k] = -buf[N8-1-k][1];
			out[outOff+N2+3+2*k] = -buf[N8-2-k][1];

			out[outOff+N2+N4+2*k] = -buf[k][1];
			out[outOff+N2+N4+2+2*k] = -buf[1+k][1];

			out[outOff+N2+N4+1+2*k] = buf[N4-1-k][0];
			out[outOff+N2+N4+3+2*k] = buf[N4-2-k][0];
		}
	}

	void processForward(float[] in, float[] out) {
		//pre-FFT complex multiplication
		for(int k = 0; k<N8; k++) {
			int n = k<<1;

			float t0 = in[N-N4-1-n]+in[N-N4+n];
			float t1 = in[N4+n]-in[N4-1-n];
			float sc[] = sincos[k];

			buf[k][0] = N*((t0*sc[0])+(t1*sc[1]));
			buf[k][1] = N*((t1*sc[0])-(t0*sc[1]));

			t0 = in[N2-1-n]-in[n];
			t1 = in[N2+n]+in[N-1-n];
			sc = sincos[k+N8];
			
			buf[k+N8][0] = N*((t0*sc[0])+(t1*sc[1]));
			buf[k+N8][1] = N*((t1*sc[0])-(t0*sc[1]));
		}

		//complex FFT, non-scaling
		fft.processForward(buf);

		//post-FFT complex multiplication
		for(int k = 0; k<N4; k++) {
			int n = k<<1;

			float sc[] = sincos[k];
			float t0 = (buf[k][0]*sc[0])+(buf[k][1]*sc[1]);
			float t1 = (buf[k][1]*sc[0])-(buf[k][0]*sc[1]);

			out[n] = -t0;
			out[N2-1-n] = t1;
			out[N2+n] = -t1;
			out[N-1-n] = t0;
		}
	}
}
