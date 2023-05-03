package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.sbr.PS;
import net.sourceforge.jaad.aac.syntax.BitStream;
import net.sourceforge.jaad.aac.tools.Utils;

import java.util.Arrays;

import static net.sourceforge.jaad.aac.ps.PSConstants.MAX_PS_ENVELOPES;
import static net.sourceforge.jaad.aac.ps.PSConstants.NO_ALLPASS_LINKS;
import static net.sourceforge.jaad.aac.ps.PSTables.*;

public class PSImpl implements PS {

	final IIDData iid = new IIDData();
	final ICCData icc = new ICCData();
	Extension ext = new Extension(iid);

	FBType fbt = FBType.T20;

	/* bitstream parameters */
	boolean var_borders;
	int num_env;
	int[] border_position = new int[MAX_PS_ENVELOPES+1];

	/* ps data was correctly read */
	boolean ps_data_available;
	/* a header has been read */
	boolean header_read;

	/* hybrid filterbank parameters */
	final Filterbank fb;

	static final int NR_ALLPASS_BANDS = 22;

	/* filter delay handling */
	int saved_delay;
	int[] delay_buf_index_ser = new int[NO_ALLPASS_LINKS];
	int[] num_sample_delay_ser = new int[NO_ALLPASS_LINKS];
	static final int SHORT_DELAY_BAND = 35;

	int[] delay_D = new int[64];
	int[] delay_buf_index_delay = new int[64];
	float[][][] delay_Qmf = new float[14][64][2]; /* 14 samples delay max, 64 QMF channels */
	float[][][] delay_SubQmf = new float[2][32][2]; /* 2 samples delay max (SubQmf is always allpass filtered) */
	float[][][][] delay_Qmf_ser = new float[NO_ALLPASS_LINKS][5][64][2]; /* 5 samples delay max (table 8.34), 64 QMF channels */
	float[][][][] delay_SubQmf_ser = new float[NO_ALLPASS_LINKS][5][32][2]; /* 5 samples delay max (table 8.34) */

	/* transients */
	static final float ALPHA_DECAY = 0.76592833836465f;;
	static final float ALPHA_SMOOTH = 0.25f;
	float[] P_PeakDecayNrg = new float[34];
	float[] P_prev = new float[34];
	float[] P_SmoothPeakDecayDiffNrg_prev = new float[34];

	/* mixing and phase */
	float[][] h11_prev = new float[50][2];
	float[][] h12_prev = new float[50][2];
	float[][] h21_prev = new float[50][2];
	float[][] h22_prev = new float[50][2];
	int phase_hist;

	public PSImpl(int frameLen) {

		fb = new Filterbank(frameLen);

		ps_data_available = false;

		/* delay stuff*/
		saved_delay = 0;

		for(int i = 0; i<64; i++) {
			delay_buf_index_delay[i] = 0;
		}

		for(int i = 0; i<NO_ALLPASS_LINKS; i++) {
			delay_buf_index_ser[i] = 0;
			/* THESE ARE CONSTANTS NOW */
			num_sample_delay_ser[i] = delay_length_d[i];
		}

		/* THESE ARE CONSTANT NOW IF PS IS INDEPENDANT OF SAMPLERATE */
		Arrays.fill(delay_D, 0, SHORT_DELAY_BAND, 14);
		Arrays.fill(delay_D, SHORT_DELAY_BAND, delay_D.length, 1);

		/* mixing and phase */
		for(int i = 0; i<50; i++) {
			h11_prev[i][0] = 1;
			h12_prev[i][1] = 1;
			h11_prev[i][0] = 1;
			h12_prev[i][1] = 1;
		}

		phase_hist = 0;
	}

	@Override
	public boolean isDataAvailable() {
		return ps_data_available;
	}

	@Override
	public void decode(BitStream ld) {

		/* check for new PS header */
		if(ld.readBool()) {
			header_read = true;

			iid.readMode(ld);
			icc.readMode(ld);
			ext.readMode(ld);

			fbt = FBType.T20
					.max(iid.fbType())
					.max(icc.fbType());
		}

		var_borders = ld.readBit()!=0;
		int tmp = ld.readBits(2);

		num_env = num_env_tab[var_borders?1:0][tmp];

		if(var_borders) {
			for(int n = 1; n<num_env+1; n++) {
				border_position[n] = ld.readBits(5)+1;
			}
		}

		iid.readData(ld, num_env);
		icc.readData(ld, num_env);
		ext.readData(ld, num_env);

		ps_data_available = true;
	}

	/* parse the bitstream data decoded in ps_data() */
	private void ps_data_decode() {

		/* ps data not available, use data from previous frame */
		if(!ps_data_available) {
			num_env = 0;
		}

		iid.decode(num_env);
		icc.decode(num_env);
		ext.decode(num_env);

		/* handle error case */
		if(num_env==0) {
			/* force to 1 */
			num_env = 1;
		}

		/* update previous indices */

		iid.update(num_env);
		icc.update(num_env);
		ext.update(num_env);

		ps_data_available = false;

		if(!var_borders) {
			border_position[0] = 0;
			for(int env = 1; env<num_env; env++) {
				border_position[env] = (env*fb.len)/num_env;
			}
			border_position[num_env] = fb.len;
		}
		else {
			border_position[0] = 0;

			if(border_position[num_env]<fb.len) {
				iid.restore(num_env);
				icc.restore(num_env);
				ext.restore(num_env);

				++num_env;
				border_position[num_env] = fb.len;
			}

			int bpl = border_position[0];
			for(int env = 1; env<num_env; env++) {
				int bp = border_position[env];
				int max = fb.len-(num_env-env);
				bpl = Utils.clip(bp, bpl+1, max);
				if(bpl!=bp)
					border_position[env] = bpl;
			}
		}

		/* make sure that the indices of all parameters can be mapped
		 * to the same hybrid synthesis filterbank
		 */
		if(fbt==FBType.T34) {
			iid.mapTo34(num_env);
			icc.mapTo34(num_env);
			ext.mapTo34(num_env);
		}
	}

	/* decorrelate the mono signal using an allpass filter */
	private void ps_decorrelate(float[][][] X_left, float[][][] X_right,
		float[][][] X_hybrid_left, float[][][] X_hybrid_right) {
		float[][] P = new float[32][34];
		float[][] G_TransientRatio = new float[32][34];

		/* clear the energy values */
		for(int n = 0; n<32; n++) {
			for(int bk = 0; bk<34; bk++) {
				P[n][bk] = 0;
			}
		}

		/* calculate the energy in each parameter band b(k) */
		for(int gr = 0; gr<fbt.num_groups; gr++) {
			/* select the parameter index b(k) to which this group belongs */
			int bk = fbt.bk(gr);

			/* select the upper subband border for this group */
			int maxsb = fbt.maxsb(gr);
			float[][][] Xl = gr<fbt.num_hybrid_groups ? X_hybrid_left : X_left;

			for(int n = border_position[0]; n<border_position[num_env]; n++) {
				float[] p = P[n];
				for(int sb = fbt.group_border[gr]; sb<maxsb; sb++) {
					float[] xl = Xl[n][sb];
					float re = xl[0];
					float im = xl[1];
					/* accumulate energy */
					p[bk] += (re*re)+(im*im);
				}
			}
		}

		/* calculate transient reduction ratio for each parameter band b(k) */
		for(int bk = 0; bk<fbt.nr_par_bands; bk++) {
			for(int n = border_position[0]; n<border_position[num_env]; n++) {
				float gamma = 1.5f;

				P_PeakDecayNrg[bk] = (P_PeakDecayNrg[bk]* ALPHA_DECAY);
				if(P_PeakDecayNrg[bk]<P[n][bk])
					P_PeakDecayNrg[bk] = P[n][bk];

				/* apply smoothing filter to peak decay energy */
				float P_SmoothPeakDecayDiffNrg = P_SmoothPeakDecayDiffNrg_prev[bk];
				P_SmoothPeakDecayDiffNrg += ((P_PeakDecayNrg[bk]-P[n][bk]-P_SmoothPeakDecayDiffNrg_prev[bk])* ALPHA_SMOOTH);
				P_SmoothPeakDecayDiffNrg_prev[bk] = P_SmoothPeakDecayDiffNrg;

				/* apply smoothing filter to energy */
				float nrg = P_prev[bk];
				nrg += ((P[n][bk]-P_prev[bk])* ALPHA_SMOOTH);
				P_prev[bk] = nrg;

				/* calculate transient ratio */
				if((P_SmoothPeakDecayDiffNrg*gamma)<=nrg) {
					G_TransientRatio[n][bk] = 1.0f;
				}
				else {
					G_TransientRatio[n][bk] = (nrg/(P_SmoothPeakDecayDiffNrg*gamma));
				}
			}
		}

		int temp_delay = 0;
		int[] temp_delay_ser = new int[NO_ALLPASS_LINKS];
		float[] g_DecaySlope_filt = new float[NO_ALLPASS_LINKS];

		/* apply stereo decorrelation filter to the signal */
		for(int gr = 0; gr<fbt.num_groups; gr++) {
			int maxsb = fbt.maxsb(gr);

			float[][][] Xl = gr<fbt.num_hybrid_groups ? X_hybrid_left : X_left;
			float[][][] Xr = gr<fbt.num_hybrid_groups ? X_hybrid_right : X_right;
			float[][][][] delay_ser = gr<fbt.num_hybrid_groups ? delay_SubQmf_ser : delay_Qmf_ser;
			float[][][] qFractAllpassQmf = gr<fbt.num_hybrid_groups ? fbt.qFractAllpassSubQmf : Q_Fract_allpass_Qmf;

			/* QMF channel */
			for(int sb = fbt.group_border[gr]; sb<maxsb; sb++) {
				float g_DecaySlope;

				/* g_DecaySlope: [0..1] */
				if(gr<fbt.num_hybrid_groups||sb<=fbt.decay_cutoff) {
					g_DecaySlope = 1.0f;
				}
				else {
					int decay = fbt.decay_cutoff-sb;
					if(decay<=-20 /* -1/DECAY_SLOPE */) {
						g_DecaySlope = 0;
					}
					else {
						/* decay(int)*decay_slope(frac) = g_DecaySlope(frac) */
						g_DecaySlope = 1.0f+DECAY_SLOPE*decay;
					}
				}

				/* calculate g_DecaySlope_filt for every m multiplied by filter_a[m] */
				for(int m = 0; m<NO_ALLPASS_LINKS; m++) {
					g_DecaySlope_filt[m] = g_DecaySlope*filter_a[m];
				}

				/* set delay indices */
				temp_delay = saved_delay;
				for(int n = 0; n<NO_ALLPASS_LINKS; n++) {
					temp_delay_ser[n] = delay_buf_index_ser[n];
				}

				for(int n = border_position[0]; n<border_position[num_env]; n++) {

					float r0Re, r0Im;

					float re = Xl[n][sb][0];
					float im = Xl[n][sb][1];

					if(sb> NR_ALLPASS_BANDS &&gr>=fbt.num_hybrid_groups) {
						/* delay */
						float[] delay = delay_Qmf[delay_buf_index_delay[sb]][sb];
						/* never hybrid subbands here, always QMF subbands */
						r0Re = delay[0];
						r0Im = delay[1];
						delay[0] = re;
						delay[1] = im;
					}
					else {

						float[] delayQmf = gr<fbt.num_hybrid_groups ? delay_SubQmf[temp_delay][sb] : delay_Qmf[temp_delay][sb];

						/* allpass filter */
						//int m;
						float[] Phi_Fract = gr<fbt.num_hybrid_groups ? fbt.phiFract[sb] : Phi_Fract_Qmf[sb];

						float tmp0Re = delayQmf[0];
						float tmp0Im = delayQmf[1];

						delayQmf[0] = re;
						delayQmf[1] = im;

						/* z^(-2) * Phi_Fract[k] */
						r0Re = (tmp0Re*Phi_Fract[0])+(tmp0Im*Phi_Fract[1]);
						r0Im = (tmp0Im*Phi_Fract[0])-(tmp0Re*Phi_Fract[1]);

						for(int m = 0; m<NO_ALLPASS_LINKS; m++) {

							float[] qFractAllpass = qFractAllpassQmf[sb][m];
							float[] delay = delay_ser[m][temp_delay_ser[m]][sb];

							tmp0Re = delay[0];
							tmp0Im = delay[1];

							/* delay by a fraction */
							/* z^(-d(m)) * qFractAllpass[k,m] */
							float tmpRe = (tmp0Re*qFractAllpass[0])+(tmp0Im*qFractAllpass[1]);
							float tmpIm = (tmp0Im*qFractAllpass[0])-(tmp0Re*qFractAllpass[1]);

							/* -a(m) * g_DecaySlope[k] */
							tmpRe -= g_DecaySlope_filt[m]*r0Re;
							tmpIm -= g_DecaySlope_filt[m]*r0Im;

							/* -a(m) * g_DecaySlope[k] * qFractAllpass[k,m] * z^(-d(m)) */
							delay[0] = r0Re+(g_DecaySlope_filt[m]*tmpRe);
							delay[1] = r0Im+(g_DecaySlope_filt[m]*tmpIm);

							/* store for next iteration (or as output value if last iteration) */
							r0Re = tmpRe;
							r0Im = tmpIm;
						}
					}

					/* select b(k) for reading the transient ratio */
					final int bk = fbt.bk(gr);

					/* duck if a past transient is found */
					Xr[n][sb][0]  = (G_TransientRatio[n][bk]*r0Re);
					Xr[n][sb][1] = (G_TransientRatio[n][bk]*r0Im);

					/* Update delay buffer index */
					if(++temp_delay>=2) {
						temp_delay = 0;
					}

					/* update delay indices */
					if(sb> NR_ALLPASS_BANDS &&gr>=fbt.num_hybrid_groups) {
						/* delay_D depends on the samplerate, it can hold the values 14 and 1 */
						if(++delay_buf_index_delay[sb]>=delay_D[sb]) {
							delay_buf_index_delay[sb] = 0;
						}
					}

					for(int m = 0; m<NO_ALLPASS_LINKS; m++) {
						if(++temp_delay_ser[m]>=num_sample_delay_ser[m]) {
							temp_delay_ser[m] = 0;
						}
					}
				}
			}
		}

		/* update delay indices */
		saved_delay = temp_delay;
		System.arraycopy(temp_delay_ser, 0, delay_buf_index_ser, 0, NO_ALLPASS_LINKS);
	}

	private static float magnitude_c(float[] c) {
		return (float) Math.sqrt(c[0]*c[0]+c[1]*c[1]);
	}

	private void ps_mix_phase(float[][][] X_left, float[][][] X_right,
		float[][][] X_hybrid_left, float[][][] X_hybrid_right) {

		float[] h11 = new float[2], h12 = new float[2], h21 = new float[2], h22 = new float[2];
		float[] H11 = new float[2], H12 = new float[2], H21 = new float[2], H22 = new float[2];
		float[] deltaH11 = new float[2], deltaH12 = new float[2], deltaH21 = new float[2], deltaH22 = new float[2];
		float[] tempLeft = new float[2];
		float[] tempRight = new float[2];
		float[] phaseLeft = new float[2];
		float[] phaseRight = new float[2];

		IIDTables tables = iid.mode().tables();

		for(int gr = 0; gr<fbt.num_groups; gr++) {
			final int bk = fbt.bk(gr);

			/* use one channel per group in the subqmf domain */
			final int maxsb = (gr<fbt.num_hybrid_groups) ? fbt.group_border[gr]+1 : fbt.group_border[gr+1];

			for(int env = 0; env<num_env; env++) {

				int iid_index = iid.envs[env].index[bk];
				int iid_sign = iid_index<0 ? -1 : 1;
				iid_index = Math.abs(iid_index);

				int icc_index = icc.envs[env].index[bk];

				if(icc.mode().id<3) {
					/* type 'A' mixing as described in 8.6.4.6.2.1 */

					/*
					 c_1 = sqrt(2.0 / (1.0 + pow(10.0, quant_iid[no_iid_steps + iid_index] / 10.0)));
					 c_2 = sqrt(2.0 / (1.0 + pow(10.0, quant_iid[no_iid_steps - iid_index] / 10.0)));
					 alpha = 0.5 * acos(quant_rho[icc_index]);
					 beta = alpha * ( c_1 - c_2 ) / sqrt(2.0);
					 */
					//printf("%d\n", ps.iid_index[env][bk]);

					/* calculate the scalefactors c_1 and c_2 from the intensity differences */
					float[] sf_iid = tables.sf;
					int num_steps = tables.num_steps;
					float c_1 = sf_iid[num_steps+iid_index];
					float c_2 = sf_iid[num_steps-iid_index];

					/* calculate alpha and beta using the ICC parameters */
					float cosa = cos_alphas[icc_index];
					float sina = sin_alphas[icc_index];

					float cosb = tables.cos_betas[iid_index][icc_index];
					float sinb = tables.sin_betas[iid_index][icc_index] * iid_sign;

					float ab1 = (cosb*cosa);
					float ab2 = (sinb*sina);
					float ab3 = (sinb*cosa);
					float ab4 = (cosb*sina);

					/* h_xy: COEF */
					h11[0] = (c_2*(ab1-ab2));
					h12[0] = (c_1*(ab1+ab2));
					h21[0] = (c_2*(ab3+ab4));
					h22[0] = (c_1*(ab3-ab4));
				}
				else {
					/* type 'B' mixing as described in 8.6.4.6.2.2 */

					int num_steps = tables.num_steps;
					
					float cosa = tables.sincos_alphas_b[num_steps+iid_index][icc_index];
					float sina = tables.sincos_alphas_b[2*num_steps-(num_steps+iid_index)][icc_index];
					float cosg = tables.cos_gammas[iid_index][icc_index];
					float sing = tables.sin_gammas[iid_index][icc_index];

					h11[0] = (COEF_SQRT2*(cosa*cosg));
					h12[0] = (COEF_SQRT2*(sina*cosg));
					h21[0] = (COEF_SQRT2*(-cosa*sing));
					h22[0] = (COEF_SQRT2*(sina*sing));
				}

				/* calculate phase rotation parameters H_xy */
				/* note that the imaginary part of these parameters are only calculated when
				 IPD and OPD are enabled
				 */
				int nr_ipdopd_par = ext.nr_par();

				if(bk<nr_ipdopd_par) {

					var ipd_prev =  ext.data.ipd.prev[bk][phase_hist];
					var opd_prev =  ext.data.opd.prev[bk][phase_hist];

					/* previous value */
					tempLeft[0] = (ipd_prev[0]*0.25f);
					tempLeft[1] = (ipd_prev[1]*0.25f);
					tempRight[0] = (opd_prev[0]*0.25f);
					tempRight[1] = (opd_prev[1]*0.25f);

					/* save current value */
					int ipd_index = Math.abs(ext.data.ipd.envs[env].index[bk]);
					int opd_index = Math.abs(ext.data.ipd.envs[env].index[bk]);

					ipd_prev[0] = ipdopd_cos_tab[ipd_index];
					ipd_prev[1] = ipdopd_sin_tab[ipd_index];
					opd_prev[0] = ipdopd_cos_tab[opd_index];
					opd_prev[1] = ipdopd_sin_tab[opd_index];

					/* add current value */
					tempLeft[0] += ipd_prev[0];
					tempLeft[1] += ipd_prev[1];
					tempRight[0] += opd_prev[0];
					tempRight[1] += opd_prev[1];

					++phase_hist;
					phase_hist %= 2;

					ipd_prev =  ext.data.opd.prev[bk][phase_hist];
					opd_prev =  ext.data.opd.prev[bk][phase_hist];

					/* get value before previous */
					tempLeft[0] += (ipd_prev[0]*0.5f);
					tempLeft[1] += (ipd_prev[1]*0.5f);
					tempRight[0] += (opd_prev[0]*0.5f);
					tempRight[1] += (opd_prev[1]*0.5f);

					float xy = magnitude_c(tempRight);
					float pq = magnitude_c(tempLeft);

					if(xy!=0) {
						phaseLeft[0] = (tempRight[0]/xy);
						phaseLeft[1] = (tempRight[1]/xy);
					}
					else {
						phaseLeft[0] = 0;
						phaseLeft[1] = 0;
					}

					float xypq = (xy*pq);

					if(xypq!=0) {
						float tmp1 = (tempRight[0]*tempLeft[0])+(tempRight[1]*tempLeft[1]);
						float tmp2 = (tempRight[1]*tempLeft[0])-(tempRight[0]*tempLeft[1]);

						phaseRight[0] = (tmp1/xypq);
						phaseRight[1] = (tmp2/xypq);
					}
					else {
						phaseRight[0] = 0;
						phaseRight[1] = 0;
					}

					/* MUL_F(COEF, REAL) = COEF */
					h11[1] = (h11[0]*phaseLeft[1]);
					h12[1] = (h12[0]*phaseRight[1]);
					h21[1] = (h21[0]*phaseLeft[1]);
					h22[1] = (h22[0]*phaseRight[1]);

					h11[0] = (h11[0]*phaseLeft[0]);
					h12[0] = (h12[0]*phaseRight[0]);
					h21[0] = (h21[0]*phaseLeft[0]);
					h22[0] = (h22[0]*phaseRight[0]);
				}

				/* length of the envelope n_e+1 - n_e (in time samples) */
				/* 0 < L <= 32: integer */
				final float L = (float) (border_position[env+1]-border_position[env]);

				/* obtain final H_xy by means of linear interpolation */
				deltaH11[0] = (h11[0]-h11_prev[gr][0])/L;
				deltaH12[0] = (h12[0]-h12_prev[gr][0])/L;
				deltaH21[0] = (h21[0]-h21_prev[gr][0])/L;
				deltaH22[0] = (h22[0]-h22_prev[gr][0])/L;

				H11[0] = h11_prev[gr][0];
				H12[0] = h12_prev[gr][0];
				H21[0] = h21_prev[gr][0];
				H22[0] = h22_prev[gr][0];

				h11_prev[gr][0] = h11[0];
				h12_prev[gr][0] = h12[0];
				h21_prev[gr][0] = h21[0];
				h22_prev[gr][0] = h22[0];

				/* only calculate imaginary part when needed */
				if(bk<nr_ipdopd_par) {
					/* obtain final H_xy by means of linear interpolation */
					deltaH11[1] = (h11[1]-h11_prev[gr][1])/L;
					deltaH12[1] = (h12[1]-h12_prev[gr][1])/L;
					deltaH21[1] = (h21[1]-h21_prev[gr][1])/L;
					deltaH22[1] = (h22[1]-h22_prev[gr][1])/L;

					H11[1] = h11_prev[gr][1];
					H12[1] = h12_prev[gr][1];
					H21[1] = h21_prev[gr][1];
					H22[1] = h22_prev[gr][1];

					if(fbt.bkm(gr)) {
						deltaH11[1] = -deltaH11[1];
						deltaH12[1] = -deltaH12[1];
						deltaH21[1] = -deltaH21[1];
						deltaH22[1] = -deltaH22[1];

						H11[1] = -H11[1];
						H12[1] = -H12[1];
						H21[1] = -H21[1];
						H22[1] = -H22[1];
					}

					h11_prev[gr][1] = h11[1];
					h12_prev[gr][1] = h12[1];
					h21_prev[gr][1] = h21[1];
					h22_prev[gr][1] = h22[1];
				}

				/* apply H_xy to the current envelope band of the decorrelated subband */
				for(int n = border_position[env]; n<border_position[env+1]; n++) {
					/* addition finalises the interpolation over every n */
					H11[0] += deltaH11[0];
					H12[0] += deltaH12[0];
					H21[0] += deltaH21[0];
					H22[0] += deltaH22[0];
					if(bk<nr_ipdopd_par) {
						H11[1] += deltaH11[1];
						H12[1] += deltaH12[1];
						H21[1] += deltaH21[1];
						H22[1] += deltaH22[1];
					}

					/* channel is an alias to the subband */
					for(int sb = fbt.group_border[gr]; sb<maxsb; sb++) {
						float[] inLeft = new float[2], inRight = new float[2];

						/* load decorrelated samples */
						if(gr<fbt.num_hybrid_groups) {
							inLeft[0] = X_hybrid_left[n][sb][0];
							inLeft[1] = X_hybrid_left[n][sb][1];
							inRight[0] = X_hybrid_right[n][sb][0];
							inRight[1] = X_hybrid_right[n][sb][1];
						}
						else {
							inLeft[0] = X_left[n][sb][0];
							inLeft[1] = X_left[n][sb][1];
							inRight[0] = X_right[n][sb][0];
							inRight[1] = X_right[n][sb][1];
						}

						/* apply mixing */
						tempLeft[0] = (H11[0]*inLeft[0])+(H21[0]*inRight[0]);
						tempLeft[1] = (H11[0]*inLeft[1])+(H21[0]*inRight[1]);
						tempRight[0] = (H12[0]*inLeft[0])+(H22[0]*inRight[0]);
						tempRight[1] = (H12[0]*inLeft[1])+(H22[0]*inRight[1]);

						/* only perform imaginary operations when needed */
						if(bk<nr_ipdopd_par) {
							/* apply rotation */
							tempLeft[0] -= (H11[1]*inLeft[1])+(H21[1]*inRight[1]);
							tempLeft[1] += (H11[1]*inLeft[0])+(H21[1]*inRight[0]);
							tempRight[0] -= (H12[1]*inLeft[1])+(H22[1]*inRight[1]);
							tempRight[1] += (H12[1]*inLeft[0])+(H22[1]*inRight[0]);
						}

						/* store final samples */
						if(gr<fbt.num_hybrid_groups) {
							X_hybrid_left[n][sb][0] = tempLeft[0];
							X_hybrid_left[n][sb][1] = tempLeft[1];
							X_hybrid_right[n][sb][0] = tempRight[0];
							X_hybrid_right[n][sb][1] = tempRight[1];
						}
						else {
							X_left[n][sb][0] = tempLeft[0];
							X_left[n][sb][1] = tempLeft[1];
							X_right[n][sb][0] = tempRight[0];
							X_right[n][sb][1] = tempRight[1];
						}
					}
				}
			}
		}
	}

	/* main Parametric Stereo decoding function */
	@Override
	public void process(float[][][] X_left, float[][][] X_right) {
		float[][][] X_hybrid_left = new float[32][32][2];
		float[][][] X_hybrid_right = new float[32][32][2];

		/* delta decoding of the bitstream data */
		ps_data_decode();

		/* Perform further analysis on the lowest subbands to get a higher
		 * frequency resolution
		 */
		fb.hybrid_analysis(X_left, X_hybrid_left, fbt);

		/* decorrelate mono signal */
		ps_decorrelate(X_left, X_right, X_hybrid_left, X_hybrid_right);

		/* apply mixing and phase parameters */
		ps_mix_phase(X_left, X_right, X_hybrid_left, X_hybrid_right);

		/* hybrid synthesis, to rebuild the SBR QMF matrices */
		fb.hybrid_synthesis(X_left, X_hybrid_left, fbt);

		fb.hybrid_synthesis(X_right, X_hybrid_right, fbt);
	}
}
