package net.sourceforge.jaad.aac.sbr;

class AnalysisFilterbank extends Filterbank {

	AnalysisFilterbank(int channels) {
		super(channels);
	}

	void sbr_qmf_analysis_32(int numTimeSlotsRate, float[] input,
		float[][][] X, int offset, int kx) {
		float[] u = new float[64];
		float[] in_real = new float[32], in_imag = new float[32];
		float[] out_real = new float[32], out_imag = new float[32];

		/* qmf subsample l */
		for(int l = 0, in=0; l<numTimeSlotsRate; l++) {

			/* add new samples to input buffer x */
			for(int n = 32-1; n>=0; n--) {
				this.v[this.v_index +n] = this.v[this.v_index +n+320] = input[in++];
			}

			/* window and summation to create array u */
			for(int n = 0; n<64; n++) {
				u[n] = (this.v[this.v_index +n]*qmf_c[2*n])
					+(this.v[this.v_index +n+64]*qmf_c[2*(n+64)])
					+(this.v[this.v_index +n+128]*qmf_c[2*(n+128)])
					+(this.v[this.v_index +n+192]*qmf_c[2*(n+192)])
					+(this.v[this.v_index +n+256]*qmf_c[2*(n+256)]);
			}

			/* update ringbuffer index */
			this.v_index -= 32;
			if(this.v_index <0)
				this.v_index = (320-32);

			/* calculate 32 subband samples by introducing X */
			// Reordering of data moved from DCT_IV to here
			in_imag[31] = u[1];
			in_real[0] = u[0];
			for(int n = 1; n<31; n++) {
				in_imag[31-n] = u[n+1];
				in_real[n] = -u[64-n];
			}
			in_imag[0] = u[32];
			in_real[31] = -u[33];

			// dct4_kernel is DCT_IV without reordering which is done before and after FFT
			DCT.dct4_kernel(in_real, in_imag, out_real, out_imag);

			// Reordering of data moved from DCT_IV to here
			for(int n = 0; n<16; n++) {
				if(2*n+1<kx) {
					X[l+offset][2*n][0] = 2.0f*out_real[n];
					X[l+offset][2*n][1] = 2.0f*out_imag[n];
					X[l+offset][2*n+1][0] = -2.0f*out_imag[31-n];
					X[l+offset][2*n+1][1] = -2.0f*out_real[31-n];
				}
				else {
					if(2*n<kx) {
						X[l+offset][2*n][0] = 2.0f*out_real[n];
						X[l+offset][2*n][1] = 2.0f*out_imag[n];
					}
					else {
						X[l+offset][2*n][0] = 0;
						X[l+offset][2*n][1] = 0;
					}
					X[l+offset][2*n+1][0] = 0;
					X[l+offset][2*n+1][1] = 0;
				}
			}
		}
	}
}
