package net.sourceforge.jaad.aac.ps;

class Filterbank implements PSTables {

	public final int len;

	private final float[][] work;
	private final float[][][] buffer;
	private final float[][][] temp;

	Filterbank(int len) {
		this.len = len;
		work = new float[(this.len +12)][2];
		buffer = new float[5][this.len][2];
		temp = new float[this.len][12][2];
	}

	void hybrid_analysis(float[][][] X, float[][][] X_hybrid, FBType fbt) {

		for(int band = 0, offset = 0; band<fbt.decay_cutoff; band++) {
			/* build working buffer */
			//memcpy(this.work, this.buffer[band], 12*sizeof(qmf_t));
			for(int i = 0; i<12; i++) {
				work[i][0] = buffer[band][i][0];
				work[i][1] = buffer[band][i][1];
			}

			/* add new samples */
			for(int n = 0; n<this.len; n++) {
				this.work[12+n][0] = X[n+6 /*delay*/][band][0];
				this.work[12+n][1] = X[n+6 /*delay*/][band][1];
			}

			/* store samples */
			//memcpy(this.buffer[band], this.work+this.frame_len, 12*sizeof(qmf_t));
			for(int i = 0; i<12; i++) {
				buffer[band][i][0] = work[len +i][0];
				buffer[band][i][1] = work[len +i][1];
			}

			Filter f = fbt.filters[band];

			int resolution = f.filter(len, work, temp);

			for(int n = 0; n<this.len; n++) {
				for(int k = 0; k<f.resolution(); k++) {
					X_hybrid[n][offset+k][0] = this.temp[n][k][0];
					X_hybrid[n][offset+k][1] = this.temp[n][k][1];
				}
			}
			offset += resolution;
		}

		/* group hybrid channels */
		if(fbt!=FBType.T34) {
			for(int n = 0; n< len; n++) {
				X_hybrid[n][3][0] += X_hybrid[n][4][0];
				X_hybrid[n][3][1] += X_hybrid[n][4][1];
				X_hybrid[n][4][0] = 0;
				X_hybrid[n][4][1] = 0;

				X_hybrid[n][2][0] += X_hybrid[n][5][0];
				X_hybrid[n][2][1] += X_hybrid[n][5][1];
				X_hybrid[n][5][0] = 0;
				X_hybrid[n][5][1] = 0;
			}
		}
	}

	void hybrid_synthesis(float[][][] X, float[][][] X_hybrid, FBType fbt) {

		for(int band = 0, offset = 0; band<fbt.decay_cutoff; band++) {
			int resolution = fbt.filters[band].resolution();

			for(int n = 0; n<this.len; n++) {
				X[n][band][0] = 0;
				X[n][band][1] = 0;

				for(int k = 0; k<resolution; k++) {
					X[n][band][0] += X_hybrid[n][offset+k][0];
					X[n][band][1] += X_hybrid[n][offset+k][1];
				}
			}
			offset += resolution;
		}
	}
}
