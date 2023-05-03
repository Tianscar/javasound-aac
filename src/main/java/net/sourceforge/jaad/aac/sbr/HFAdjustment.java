package net.sourceforge.jaad.aac.sbr;

class HFAdjustment implements NoiseTable {

	private static final float[] h_smooth = {
		0.03183050093751f, 0.11516383427084f,
		0.21816949906249f, 0.30150283239582f,
		0.33333333333333f
	};
	private static final int[] phi_re = {1, 0, -1, 0};
	private static final int[] phi_im = {0, 1, 0, -1};
	private static final float[] limGain = {0.5f, 1.0f, 2.0f, 1e10f};
	private static final float EPS = 1e-12f;


	private float[][] G_lim_boost = new float[SBR.MAX_L_E][SBR.MAX_M];
	private float[][] Q_M_lim_boost = new float[SBR.MAX_L_E][SBR.MAX_M];
	private float[][] S_M_boost = new float[SBR.MAX_L_E][SBR.MAX_M];

	public static void hf_adjustment(SBR sbr, float[][][] Xsbr, Channel ch) {
		HFAdjustment adj = new HFAdjustment();

		if(ch.bs_frame_class== FrameClass.FIXFIX) {
			ch.l_A = -1;
		}
		else if(ch.bs_frame_class== FrameClass.VARFIX) {
			if(ch.bs_pointer>1)
				ch.l_A = ch.bs_pointer-1;
			else
				ch.l_A = -1;
		}
		else {
			if(ch.bs_pointer==0)
				ch.l_A = -1;
			else
				ch.l_A = ch.L_E+1-ch.bs_pointer;
		}

		adj.estimate_current_envelope(sbr, Xsbr, ch);

		adj.calculate_gain(sbr, ch);

		adj.hf_assembly(sbr, Xsbr, ch);
	}

	private static int get_S_mapped(SBR sbr, Channel ch, int l, int current_band) {
		if(ch.f[l]== FBT.HI_RES) {
			/* in case of using f_table_high we just have 1 to 1 mapping
			 * from bs_add_harmonic[l][k]
			 */
			if((l>=ch.l_A)
				||(ch.bs_add_harmonic_prev[current_band]!=0&&ch.bs_add_harmonic_flag_prev)) {
				return ch.bs_add_harmonic[current_band];
			}
		}
		else {

			/* in case of f_table_low we check if any of the HI_RES bands
			 * within this LO_RES band has bs_add_harmonic[l][k] turned on
			 * (note that borders in the LO_RES table are also present in
			 * the HI_RES table)
			 */

			/* find first HI_RES band in current LO_RES band */
			int lb = 2*current_band-((sbr.N_high&1)!=0 ? 1 : 0);
			/* find first HI_RES band in next LO_RES band */
			int ub = 2*(current_band+1)-((sbr.N_high&1)!=0 ? 1 : 0);

			/* check all HI_RES bands in current LO_RES band for sinusoid */
			for(int b = lb; b<ub; b++) {
				if((l>=ch.l_A)
					||(ch.bs_add_harmonic_prev[b]!=0&&ch.bs_add_harmonic_flag_prev)) {
					if(ch.bs_add_harmonic[b]==1)
						return 1;
				}
			}
		}

		return 0;
	}

	private void estimate_current_envelope(SBR sbr,
		float[][][] Xsbr, Channel ch) {
		float nrg, div;

		if(sbr.hdr.bs_interpol_freq) {
			for(int l = 0; l<ch.L_E; l++) {

				int l_i = ch.t_E[l];
				int u_i = ch.t_E[l+1];

				div = (float) (u_i-l_i);

				if(div==0)
					div = 1;

				for(int m = 0; m<sbr.M; m++) {
					nrg = 0;

					for(int i = l_i+sbr.tHFAdj; i<u_i+sbr.tHFAdj; i++) {
						nrg += (Xsbr[i][m+sbr.kx][0]*Xsbr[i][m+sbr.kx][0])
							+(Xsbr[i][m+sbr.kx][1]*Xsbr[i][m+sbr.kx][1]);
					}

					ch.E_curr[m][l] = nrg/div;
				}
			}
		}
		else {
			for(int l = 0; l<ch.L_E; l++) {
				for(int p = 0; p<sbr.n[ch.f[l]]; p++) {
					int k_l = sbr.f_table_res[ch.f[l]][p];
					int k_h = sbr.f_table_res[ch.f[l]][p+1];

					for(int k = k_l; k<k_h; k++) {
						nrg = 0;

						int l_i = ch.t_E[l];
						int u_i = ch.t_E[l+1];

						div = (float) ((u_i-l_i)*(k_h-k_l));

						if(div==0)
							div = 1;

						for(int i = l_i+sbr.tHFAdj; i<u_i+sbr.tHFAdj; i++) {
							for(int j = k_l; j<k_h; j++) {
								nrg += (Xsbr[i][j][0]*Xsbr[i][j][0])
									+(Xsbr[i][j][1]*Xsbr[i][j][1]);
							}
						}

						ch.E_curr[k-sbr.kx][l] = nrg/div;
					}
				}
			}
		}
	}

	private void hf_assembly(SBR sbr,
		float[][][] Xsbr, Channel ch) {

		int fIndexNoise = 0;
		int fIndexSine = 0;
		boolean assembly_reset = false;

		float G_filt, Q_filt;

		int h_SL;

		if(sbr.reset) {
			assembly_reset = true;
			fIndexNoise = 0;
		}
		else {
			fIndexNoise = ch.index_noise_prev;
		}
		fIndexSine = ch.psi_is_prev;

		for(int l = 0; l<ch.L_E; l++) {
			boolean no_noise = (l==ch.l_A||l==ch.prevEnvIsShort);

			h_SL = (sbr.hdr.bs_smoothing_mode) ? 0 : 4;
			h_SL = (no_noise ? 0 : h_SL);

			if(assembly_reset) {
				for(int n = 0; n<4; n++) {
					System.arraycopy(this.G_lim_boost[l], 0, ch.G_temp_prev[n], 0, sbr.M);
					System.arraycopy(this.Q_M_lim_boost[l], 0, ch.Q_temp_prev[n], 0, sbr.M);
				}
				/* reset ringbuffer index */
				ch.GQ_ringbuf_index = 4;
				assembly_reset = false;
			}

			for(int i = ch.t_E[l]; i<ch.t_E[l+1]; i++) {
				/* load new values into ringbuffer */
				System.arraycopy(this.G_lim_boost[l], 0, ch.G_temp_prev[ch.GQ_ringbuf_index], 0, sbr.M);
				System.arraycopy(this.Q_M_lim_boost[l], 0, ch.Q_temp_prev[ch.GQ_ringbuf_index], 0, sbr.M);

				for(int m = 0; m<sbr.M; m++) {
					float[] psi = new float[2];

					G_filt = 0;
					Q_filt = 0;

					if(h_SL!=0) {
						int ri = ch.GQ_ringbuf_index;
						for(int n = 0; n<=4; n++) {
							float curr_h_smooth = h_smooth[n];
							ri++;
							if(ri>=5)
								ri -= 5;
							G_filt += (ch.G_temp_prev[ri][m]*curr_h_smooth);
							Q_filt += (ch.Q_temp_prev[ri][m]*curr_h_smooth);
						}
					}
					else {
						G_filt = ch.G_temp_prev[ch.GQ_ringbuf_index][m];
						Q_filt = ch.Q_temp_prev[ch.GQ_ringbuf_index][m];
					}

					Q_filt = (this.S_M_boost[l][m]!=0||no_noise) ? 0 : Q_filt;

					/* add noise to the output */
					fIndexNoise = (fIndexNoise+1)&511;

					/* the smoothed gain values are applied to Xsbr */
					/* V is defined, not calculated */
					Xsbr[i+sbr.tHFAdj][m+sbr.kx][0] = G_filt*Xsbr[i+sbr.tHFAdj][m+sbr.kx][0]
						+(Q_filt*NOISE_TABLE[fIndexNoise][0]);
					//if(sbr.bs_extension_id==3&&sbr.bs_extension_data==42)
					//	Xsbr[i+sbr.tHFAdj][m+sbr.kx][0] = 16428320;  // 0xFAAD20
					Xsbr[i+sbr.tHFAdj][m+sbr.kx][1] = G_filt*Xsbr[i+sbr.tHFAdj][m+sbr.kx][1]
						+(Q_filt*NOISE_TABLE[fIndexNoise][1]);

					{
						int rev = (((m+sbr.kx)&1)!=0 ? -1 : 1);
						psi[0] = this.S_M_boost[l][m]*phi_re[fIndexSine];
						Xsbr[i+sbr.tHFAdj][m+sbr.kx][0] += psi[0];

						psi[1] = rev*this.S_M_boost[l][m]*phi_im[fIndexSine];
						Xsbr[i+sbr.tHFAdj][m+sbr.kx][1] += psi[1];
					}
				}

				fIndexSine = (fIndexSine+1)&3;

				/* update the ringbuffer index used for filtering G and Q with h_smooth */
				ch.GQ_ringbuf_index++;
				if(ch.GQ_ringbuf_index>=5)
					ch.GQ_ringbuf_index = 0;
			}
		}

		ch.index_noise_prev = fIndexNoise;
		ch.psi_is_prev = fIndexSine;
	}

	private void calculate_gain(SBR sbr, Channel ch) {

		int current_t_noise_band = 0;
		int S_mapped;

		float[] Q_M_lim = new float[SBR.MAX_M];
		float[] G_lim = new float[SBR.MAX_M];
		float G_boost;
		float[] S_M = new float[SBR.MAX_M];

		for(int l = 0; l<ch.L_E; l++) {
			int current_f_noise_band = 0;
			int current_res_band = 0;
			int current_res_band2 = 0;
			int current_hi_res_band = 0;

			float delta = (l==ch.l_A||l==ch.prevEnvIsShort) ? 0 : 1;

			S_mapped = get_S_mapped(sbr, ch, l, current_res_band2);

			if(ch.t_E[l+1]>ch.t_Q[current_t_noise_band+1]) {
				current_t_noise_band++;
			}

			for(int k = 0; k<sbr.N_L[sbr.hdr.bs_limiter_bands]; k++) {
				float G_max;
				float den = 0;
				float acc1 = 0;
				float acc2 = 0;
				int current_res_band_size = 0;

				int ml1, ml2;

				ml1 = sbr.f_table_lim[sbr.hdr.bs_limiter_bands][k];
				ml2 = sbr.f_table_lim[sbr.hdr.bs_limiter_bands][k+1];


				/* calculate the accumulated E_orig and E_curr over the limiter band */
				for(int m = ml1; m<ml2; m++) {
					if((m+sbr.kx)==sbr.f_table_res[ch.f[l]][current_res_band+1]) {
						current_res_band++;
					}
					acc1 += ch.E_orig[current_res_band][l];
					acc2 += ch.E_curr[m][l];
				}


				/* calculate the maximum gain */
				/* ratio of the energy of the original signal and the energy
				 * of the HF generated signal
				 */
				G_max = ((EPS+acc1)/(EPS+acc2))*limGain[sbr.hdr.bs_limiter_gains];
				G_max = Math.min(G_max, 1e10f);

				for(int m = ml1; m<ml2; m++) {
					float Q_M, G;
					float Q_div, Q_div2;
					int S_index_mapped;


					/* check if m is on a noise band border */
					if((m+sbr.kx)==sbr.f_table_noise[current_f_noise_band+1]) {
						/* step to next noise band */
						current_f_noise_band++;
					}


					/* check if m is on a resolution band border */
					if((m+sbr.kx)==sbr.f_table_res[ch.f[l]][current_res_band2+1]) {
						/* step to next resolution band */
						current_res_band2++;

						/* if we move to a new resolution band, we should check if we are
						 * going to add a sinusoid in this band
						 */
						S_mapped = get_S_mapped(sbr, ch, l, current_res_band2);
					}


					/* check if m is on a HI_RES band border */
					if((m+sbr.kx)==sbr.f_table_res[FBT.HI_RES][current_hi_res_band+1]) {
						/* step to next HI_RES band */
						current_hi_res_band++;
					}


					/* find S_index_mapped
					 * S_index_mapped can only be 1 for the m in the middle of the
					 * current HI_RES band
					 */
					S_index_mapped = 0;
					if((l>=ch.l_A)
						||(ch.bs_add_harmonic_prev[current_hi_res_band]!=0&&ch.bs_add_harmonic_flag_prev)) {
						/* find the middle subband of the HI_RES frequency band */
						if((m+sbr.kx)==(sbr.f_table_res[FBT.HI_RES][current_hi_res_band+1]+sbr.f_table_res[FBT.HI_RES][current_hi_res_band])>>1)
							S_index_mapped = ch.bs_add_harmonic[current_hi_res_band];
					}


					/* Q_div: [0..1] (1/(1+Q_mapped)) */
					Q_div = ch.Q_div[current_f_noise_band][current_t_noise_band];


					/* Q_div2: [0..1] (Q_mapped/(1+Q_mapped)) */
					Q_div2 = ch.Q_div2[current_f_noise_band][current_t_noise_band];


					/* Q_M only depends on E_orig and Q_div2:
					 * since N_Q <= N_Low <= N_High we only need to recalculate Q_M on
					 * a change of current noise band
					 */
					Q_M = ch.E_orig[current_res_band2][l]*Q_div2;


					/* S_M only depends on E_orig, Q_div and S_index_mapped:
					 * S_index_mapped can only be non-zero once per HI_RES band
					 */
					if(S_index_mapped==0) {
						S_M[m] = 0;
					}
					else {
						S_M[m] = ch.E_orig[current_res_band2][l]*Q_div;

						/* accumulate sinusoid part of the total energy */
						den += S_M[m];
					}


					/* calculate gain */
					/* ratio of the energy of the original signal and the energy
					 * of the HF generated signal
					 */
					G = ch.E_orig[current_res_band2][l]/(1.0f+ch.E_curr[m][l]);
					if((S_mapped==0)&&(delta==1))
						G *= Q_div;
					else if(S_mapped==1)
						G *= Q_div2;


					/* limit the additional noise energy level */
					/* and apply the limiter */
					if(G_max>G) {
						Q_M_lim[m] = Q_M;
						G_lim[m] = G;
					}
					else {
						Q_M_lim[m] = Q_M*G_max/G;
						G_lim[m] = G_max;
					}


					/* accumulate the total energy */
					den += ch.E_curr[m][l]*G_lim[m];
					if((S_index_mapped==0)&&(l!=ch.l_A))
						den += Q_M_lim[m];
				}

				/* G_boost: [0..2.51188643] */
				G_boost = (acc1+EPS)/(den+EPS);
				G_boost = Math.min(G_boost, 2.51188643f /* 1.584893192 ^ 2 */);

				for(int m = ml1; m<ml2; m++) {
					/* apply compensation to gain, noise floor sf's and sinusoid levels */
					this.G_lim_boost[l][m] = (float) Math.sqrt(G_lim[m]*G_boost);
					this.Q_M_lim_boost[l][m] = (float) Math.sqrt(Q_M_lim[m]*G_boost);

					if(S_M[m]!=0) {
						this.S_M_boost[l][m] = (float) Math.sqrt(S_M[m]*G_boost);
					}
					else {
						this.S_M_boost[l][m] = 0;
					}
				}
			}
		}
	}

}
