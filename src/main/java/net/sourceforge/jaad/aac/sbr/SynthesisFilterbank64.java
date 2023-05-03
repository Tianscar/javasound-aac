package net.sourceforge.jaad.aac.sbr;

class SynthesisFilterbank64 extends SynthesisFilterbank {

	SynthesisFilterbank64() {
		super(64);
	}

	void synthesis(int numTimeSlotsRate, float[][][] X, float[] output) {
		float[] in_real1 = new float[32], in_imag1 = new float[32], out_real1 = new float[32], out_imag1 = new float[32];
		float[] in_real2 = new float[32], in_imag2 = new float[32], out_real2 = new float[32], out_imag2 = new float[32];
		float[][] pX;
		float scale = 1.f/64.f;
		int out = 0;

		/* qmf subsample l */
		for(int l = 0; l<numTimeSlotsRate; l++) {
			/* shift buffer v */
			/* buffer is not shifted, we use double ringbuffer */
			//memmove(qmfs.v + 128, qmfs.v, (1280-128)*sizeof(real_t));

			/* calculate 128 samples */
			pX = X[l];

			in_imag1[31] = scale*pX[1][0];
			in_real1[0] = scale*pX[0][0];
			in_imag2[31] = scale*pX[63-1][1];
			in_real2[0] = scale*pX[63-0][1];
			for(int k = 1; k<31; k++) {
				in_imag1[31-k] = scale*pX[2*k+1][0];
				in_real1[     k] = scale*pX[2*k][0];
				in_imag2[31-k] = scale*pX[63-(2*k+1)][1];
				in_real2[     k] = scale*pX[63-(2*k)][1];
			}
			in_imag1[0] = scale*pX[63][0];
			in_real1[31] = scale*pX[62][0];
			in_imag2[0] = scale*pX[63-63][1];
			in_real2[31] = scale*pX[63-62][1];

			// dct4_kernel is DCT_IV without reordering which is done before and after FFT
			DCT.dct4_kernel(in_real1, in_imag1, out_real1, out_imag1);
			DCT.dct4_kernel(in_real2, in_imag2, out_real2, out_imag2);

			int pring_buffer_1 = v_index; //*v
			int pring_buffer_3 = pring_buffer_1+1280;
			//        ptemp_1 = x1;
			//        ptemp_2 = x2;

			for(int n = 0; n<32; n++) {
				// pring_buffer_3 and pring_buffer_4 are needed only for double ring buffer
				v[pring_buffer_1+2*n] = v[pring_buffer_3+2*n] = out_real2[n]-out_real1[n];
				v[pring_buffer_1+127-2*n] = v[pring_buffer_3+127-2*n] = out_real2[n]+out_real1[n];
				v[pring_buffer_1+2*n+1] = v[pring_buffer_3+2*n+1] = out_imag2[31-n]+out_imag1[31-n];
				v[pring_buffer_1+127-(2*n+1)] = v[pring_buffer_3+127-(2*n+1)] = out_imag2[31-n]-out_imag1[31-n];
			}

			pring_buffer_1 = v_index; //*v

			/* calculate 64 output samples and window */
			for(int k = 0; k<64; k++) {
				output[out++]
					= (v[pring_buffer_1+k+0]*qmf_c[k+0])
					+(v[pring_buffer_1+k+192]*qmf_c[k+64])
					+(v[pring_buffer_1+k+256]*qmf_c[k+128])
					+(v[pring_buffer_1+k+(256+192)]*qmf_c[k+192])
					+(v[pring_buffer_1+k+512]*qmf_c[k+256])
					+(v[pring_buffer_1+k+(512+192)]*qmf_c[k+320])
					+(v[pring_buffer_1+k+768]*qmf_c[k+384])
					+(v[pring_buffer_1+k+(768+192)]*qmf_c[k+448])
					+(v[pring_buffer_1+k+1024]*qmf_c[k+512])
					+(v[pring_buffer_1+k+(1024+192)]*qmf_c[k+576]);
			}

			/* update ringbuffer index */
			this.v_index -= 128;
			if(this.v_index<0)
				this.v_index = (1280-128);
		}
	}
}
