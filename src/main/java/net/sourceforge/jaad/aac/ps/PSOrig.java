package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.sbr.PS;
import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.Arrays;

import static net.sourceforge.jaad.aac.ps.Huffman.*;
import static net.sourceforge.jaad.aac.ps.PSConstants.*;
import static net.sourceforge.jaad.aac.ps.PSTables.*;

public class PSOrig implements PS {

	final int numTimeSlotsRate;

	/* bitstream parameters */
	boolean enable_iid, enable_icc, enable_ext;
	int iid_mode;
	int icc_mode;
	int nr_iid_par;
	int nr_ipdopd_par;
	int nr_icc_par;
	int frame_class;
	int num_env;
	int[] border_position = new int[MAX_PS_ENVELOPES+1];
	boolean[] iid_dt = new boolean[MAX_PS_ENVELOPES];
	boolean[] icc_dt = new boolean[MAX_PS_ENVELOPES];
	boolean enable_ipdopd;
	int ipd_mode;
	boolean[] ipd_dt = new boolean[MAX_PS_ENVELOPES];
	boolean[] opd_dt = new boolean[MAX_PS_ENVELOPES];

	/* indices */
	int[] iid_index_prev = new int[34];
	int[] icc_index_prev = new int[34];
	int[] ipd_index_prev = new int[17];
	int[] opd_index_prev = new int[17];
	int[][] iid_index = new int[MAX_PS_ENVELOPES][34];
	int[][] icc_index = new int[MAX_PS_ENVELOPES][34];
	int[][] ipd_index = new int[MAX_PS_ENVELOPES][17];
	int[][] opd_index = new int[MAX_PS_ENVELOPES][17];

	/* ps data was correctly read */
	boolean ps_data_available;
	/* a header has been read */
	boolean header_read;

	/* hybrid filterbank parameters */
	Filterbank hyb;
	FBType fbt = FBType.T20;

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
	float[][][] ipd_prev = new float[20][2][2];
	float[][][] opd_prev = new float[20][2][2];

	public PSOrig(int numTimeSlotsRate) {
		this.numTimeSlotsRate = numTimeSlotsRate;

		hyb = new Filterbank(numTimeSlotsRate);

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

		for(int i = 0; i<20; i++) {
			ipd_prev[i][0][0] = 0;
			ipd_prev[i][0][1] = 0;
			ipd_prev[i][1][0] = 0;
			ipd_prev[i][1][1] = 0;
			opd_prev[i][0][0] = 0;
			opd_prev[i][0][1] = 0;
			opd_prev[i][1][0] = 0;
			opd_prev[i][1][1] = 0;
		}
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

			fbt = FBType.T20;

			/* Inter-channel Intensity Difference (IID) parameters enabled */
			enable_iid = ld.readBool();

			if(enable_iid) {
				iid_mode = ld.readBits(3);

				nr_iid_par = nr_iid_par_tab[iid_mode];
				nr_ipdopd_par = nr_ipdopd_par_tab[iid_mode];

				if(iid_mode==2||iid_mode==5)
					fbt = FBType.T34;

				/* IPD freq res equal to IID freq res */
				ipd_mode = iid_mode;
			}

			/* Inter-channel Coherence (ICC) parameters enabled */
			enable_icc = ld.readBool();

			if(enable_icc) {
				icc_mode = ld.readBits(3);

				nr_icc_par = nr_icc_par_tab[icc_mode];

				if(icc_mode==2||icc_mode==5)
					fbt = FBType.T34;
			}

			/* PS extension layer enabled */
			enable_ext = ld.readBool();
		}

		frame_class = ld.readBit();
		int tmp = ld.readBits(2);

		num_env = num_env_tab[frame_class][tmp];

		if(frame_class!=0) {
			for(int n = 1; n<num_env+1; n++) {
				border_position[n] = ld.readBits(5)+1;
			}
		}

		if(enable_iid) {
			for(int n = 0; n<num_env; n++) {
				iid_dt[n] = ld.readBool();

				/* iid_data */
				if(iid_mode<3) {
					huff_data(ld, iid_dt[n], nr_iid_par, t_huff_iid_def,
						f_huff_iid_def, iid_index[n]);
				}
				else {
					huff_data(ld, iid_dt[n], nr_iid_par, t_huff_iid_fine,
						f_huff_iid_fine, iid_index[n]);
				}
			}
		}

		if(enable_icc) {
			for(int n = 0; n<num_env; n++) {
				icc_dt[n] = ld.readBool();

				/* icc_data */
				huff_data(ld, icc_dt[n], nr_icc_par, t_huff_icc,
					f_huff_icc, icc_index[n]);
			}
		}

		if(enable_ext) {
			int cnt = ld.readBits(4);
			if(cnt==15) {
				cnt += ld.readBits(8);
			}

			// open a new sub stream
			ld = ld.readSubStream(8*cnt);

			while(ld.getBitsLeft()>7) {
				ps_extension(ld);
			}
		}

		ps_data_available = true;
	}

	private void ps_extension(BitStream ld) {
		int ps_extension_id = ld.readBits(2);

		if(ps_extension_id==0) {
			enable_ipdopd = ld.readBool();

			if(enable_ipdopd) {
				for(int n = 0; n<num_env; n++) {
					ipd_dt[n] = ld.readBool();

					/* ipd_data */
					huff_data(ld, ipd_dt[n], nr_ipdopd_par, t_huff_ipd,
						f_huff_ipd, ipd_index[n]);

					opd_dt[n] = ld.readBool();

					/* opd_data */
					huff_data(ld, opd_dt[n], nr_ipdopd_par, t_huff_opd,
						f_huff_opd, opd_index[n]);
				}
			}
			ld.readBit(); //reserved
		}
	}

	/* read huffman data coded in either the frequency or the time direction */
	private static void huff_data(BitStream ld, boolean dt, int nr_par,
		int[][] t_huff, int[][] f_huff, int[] par) {

		if(dt) {
			/* coded in time direction */
			for(int n = 0; n<nr_par; n++) {
				par[n] = ps_huff_dec(ld, t_huff);
			}
		}
		else {
			/* coded in frequency direction */
			par[0] = ps_huff_dec(ld, f_huff);

			for(int n = 1; n<nr_par; n++) {
				par[n] = ps_huff_dec(ld, f_huff);
			}
		}
	}

	/* binary search huffman decoding */
	private static int ps_huff_dec(BitStream ld, int[][] t_huff) {
		int index = 0;

		while(index>=0) {
			int bit = ld.readBit();
			index = t_huff[index][bit];
		}

		return index+31;
	}

	/* limits the value i to the range [min,max] */
	private static int delta_clip(int i, int min, int max) {
		if(i<min)
			return min;
		else
			if(i>max)
				return max;
		else
			return i;
	}


	/* delta decode array */
	private static void delta_decode(boolean enable, int[] index, int[] index_prev,
		boolean dt_flag, int nr_par, int stride,
		int min_index, int max_index) {

		if(enable) {
			if(!dt_flag) {
				/* delta coded in frequency direction */
				index[0] = 0+index[0];
				index[0] = delta_clip(index[0], min_index, max_index);

				for(int i = 1; i<nr_par; i++) {
					index[i] = index[i-1]+index[i];
					index[i] = delta_clip(index[i], min_index, max_index);
				}
			}
			else {
				/* delta coded in time direction */
				for(int i = 0; i<nr_par; i++) {
                //int8_t tmp2;
					//int8_t tmp = index[i];

					//printf("%d %d\n", index_prev[i*stride], index[i]);
					//printf("%d\n", index[i]);
					index[i] = index_prev[i*stride]+index[i];
					//tmp2 = index[i];
					index[i] = delta_clip(index[i], min_index, max_index);

					//if (iid)
					//{
					//    if (index[i] == 7)
					//    {
					//        printf("%d %d %d\n", index_prev[i*stride], tmp, tmp2);
					//    }
					//}
				}
			}
		}
		else {
			/* set indices to zero */
			for(int i = 0; i<nr_par; i++) {
				index[i] = 0;
			}
		}

		/* coarse */
		if(stride==2) {
			for(int i = (nr_par<<1)-1; i>0; i--) {
				index[i] = index[i>>1];
			}
		}
	}

	/* delta modulo decode array */
	/* in: log2 value of the modulo value to allow using AND instead of MOD */
	private static void delta_modulo_decode(boolean enable, int[] index, int[] index_prev,
		boolean dt_flag, int nr_par, int stride,
		int and_modulo) {

		if(enable) {
			if(!dt_flag) {
				/* delta coded in frequency direction */
				index[0] = 0+index[0];
				index[0] &= and_modulo;

				for(int i = 1; i<nr_par; i++) {
					index[i] = index[i-1]+index[i];
					index[i] &= and_modulo;
				}
			}
			else {
				/* delta coded in time direction */
				for(int i = 0; i<nr_par; i++) {
					index[i] = index_prev[i*stride]+index[i];
					index[i] &= and_modulo;
				}
			}
		}
		else {
			/* set indices to zero */
			for(int i = 0; i<nr_par; i++) {
				index[i] = 0;
			}
		}

		/* coarse */
		if(stride==2) {
			index[0] = 0;
			for(int i = (nr_par<<1)-1; i>0; i--) {
				index[i] = index[i>>1];
			}
		}
	}

	private static void map20indexto34(int[] index, int bins) {
		//index[0] = index[0];
		index[1] = (index[0]+index[1])/2;
		index[2] = index[1];
		index[3] = index[2];
		index[4] = (index[2]+index[3])/2;
		index[5] = index[3];
		index[6] = index[4];
		index[7] = index[4];
		index[8] = index[5];
		index[9] = index[5];
		index[10] = index[6];
		index[11] = index[7];
		index[12] = index[8];
		index[13] = index[8];
		index[14] = index[9];
		index[15] = index[9];
		index[16] = index[10];

		if(bins==34) {
			index[17] = index[11];
			index[18] = index[12];
			index[19] = index[13];
			index[20] = index[14];
			index[21] = index[14];
			index[22] = index[15];
			index[23] = index[15];
			index[24] = index[16];
			index[25] = index[16];
			index[26] = index[17];
			index[27] = index[17];
			index[28] = index[18];
			index[29] = index[18];
			index[30] = index[18];
			index[31] = index[18];
			index[32] = index[19];
			index[33] = index[19];
		}
	}

	/* parse the bitstream data decoded in ps_data() */
	private void ps_data_decode() {

		/* ps data not available, use data from previous frame */
		if(!ps_data_available) {
			num_env = 0;
		}

		for(int env = 0; env<num_env; env++) {
			
			int num_iid_steps = (iid_mode<3) ? 7 : 15 /*fine quant*/;

//        iid = 1;
        /* delta decode iid parameters */
			delta_decode(enable_iid, iid_index[env], 
					env==0 ? iid_index_prev : iid_index[env-1],
					iid_dt[env], nr_iid_par,
					(iid_mode==0||iid_mode==3) ? 2 : 1, 
					-num_iid_steps, num_iid_steps);
//        iid = 0;

			/* delta decode icc parameters */
			delta_decode(enable_icc, icc_index[env], 
					env==0 ? icc_index_prev : icc_index[env-1],
					icc_dt[env], nr_icc_par, 
					(icc_mode==0||icc_mode==3) ? 2 : 1, 
					0, 7);

			/* delta modulo decode ipd parameters */
			delta_modulo_decode(enable_ipdopd, ipd_index[env],
					env==0 ? ipd_index_prev : ipd_index[env-1], 
					ipd_dt[env], nr_ipdopd_par, 1, 7);

			/* delta modulo decode opd parameters */
			delta_modulo_decode(enable_ipdopd, opd_index[env],
					env==0 ? opd_index_prev : opd_index[env-1], 
					opd_dt[env], nr_ipdopd_par, 1, 7);
		}

		/* handle error case */
		if(num_env==0) {
			/* force to 1 */
			num_env = 1;

			if(enable_iid) {
				for(int bin = 0; bin<34; bin++) {
					iid_index[0][bin] = iid_index_prev[bin];
				}
			}
			else {
				for(int bin = 0; bin<34; bin++) {
					iid_index[0][bin] = 0;
				}
			}

			if(enable_icc) {
				for(int bin = 0; bin<34; bin++) {
					icc_index[0][bin] = icc_index_prev[bin];
				}
			}
			else {
				for(int bin = 0; bin<34; bin++) {
					icc_index[0][bin] = 0;
				}
			}

			if(enable_ipdopd) {
				for(int bin = 0; bin<17; bin++) {
					ipd_index[0][bin] = ipd_index_prev[bin];
					opd_index[0][bin] = opd_index_prev[bin];
				}
			}
			else {
				for(int bin = 0; bin<17; bin++) {
					ipd_index[0][bin] = 0;
					opd_index[0][bin] = 0;
				}
			}
		}

		/* update previous indices */
		for(int bin = 0; bin<34; bin++) {
			iid_index_prev[bin] = iid_index[num_env-1][bin];
		}
		for(int bin = 0; bin<34; bin++) {
			icc_index_prev[bin] = icc_index[num_env-1][bin];
		}
		for(int bin = 0; bin<17; bin++) {
			ipd_index_prev[bin] = ipd_index[num_env-1][bin];
			opd_index_prev[bin] = opd_index[num_env-1][bin];
		}

		ps_data_available = false;

		if(frame_class==0) {
			border_position[0] = 0;
			for(int env = 1; env<num_env; env++) {
				border_position[env] = (env*numTimeSlotsRate)/num_env;
			}
			border_position[num_env] = numTimeSlotsRate;
		}
		else {
			border_position[0] = 0;

			if(border_position[num_env]<numTimeSlotsRate) {
				for(int bin = 0; bin<34; bin++) {
					iid_index[num_env][bin] = iid_index[num_env-1][bin];
					icc_index[num_env][bin] = icc_index[num_env-1][bin];
				}
				for(int bin = 0; bin<17; bin++) {
					ipd_index[num_env][bin] = ipd_index[num_env-1][bin];
					opd_index[num_env][bin] = opd_index[num_env-1][bin];
				}
				num_env++;
				border_position[num_env] = numTimeSlotsRate;
			}

			for(int env = 1; env<num_env; env++) {
				int thr = numTimeSlotsRate-(num_env-env);

				if(border_position[env]>thr) {
					border_position[env] = thr;
				}
				else {
					thr = border_position[env-1]+1;
					if(border_position[env]<thr) {
						border_position[env] = thr;
					}
				}
			}
		}

		/* make sure that the indices of all parameters can be mapped
		 * to the same hybrid synthesis filterbank
		 */
		if(fbt==FBType.T34) {
			for(int env = 0; env<num_env; env++) {
				if(iid_mode!=2&&iid_mode!=5)
					map20indexto34(iid_index[env], 34);
				if(icc_mode!=2&&icc_mode!=5)
					map20indexto34(icc_index[env], 34);
				if(ipd_mode!=2&&ipd_mode!=5) {
					map20indexto34(ipd_index[env], 17);
					map20indexto34(opd_index[env], 17);
				}
			}
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
			int maxsb = (gr<fbt.num_hybrid_groups) ? fbt.group_border[gr]+1 : fbt.group_border[gr+1];
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
			int maxsb;
			if(gr<fbt.num_hybrid_groups)
				maxsb = fbt.group_border[gr]+1;
			else
				maxsb = fbt.group_border[gr+1];

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
		float[] sf_iid;
		int no_iid_steps;

		if(iid_mode>=3) {
			no_iid_steps = 15;
			sf_iid = sf_iid_fine;
		}
		else {
			no_iid_steps = 7;
			sf_iid = sf_iid_normal;
		}

		int nr_ipdopd_par; // local version
		if(ipd_mode==0||ipd_mode==3) {
			nr_ipdopd_par = 11; /* resolution */
		}
		else {
			nr_ipdopd_par = this.nr_ipdopd_par;
		}

		for(int gr = 0; gr<fbt.num_groups; gr++) {
			final int bk = fbt.bk(gr);

			/* use one channel per group in the subqmf domain */
			final int maxsb = (gr<fbt.num_hybrid_groups) ? fbt.group_border[gr]+1 : fbt.group_border[gr+1];

			for(int env = 0; env<num_env; env++) {
				if(icc_mode<3) {
					/* type 'A' mixing as described in 8.6.4.6.2.1 */
					float c_1, c_2;
					float cosa, sina;
					float cosb, sinb;
					float ab1, ab2;
					float ab3, ab4;

					/*
					 c_1 = sqrt(2.0 / (1.0 + pow(10.0, quant_iid[no_iid_steps + iid_index] / 10.0)));
					 c_2 = sqrt(2.0 / (1.0 + pow(10.0, quant_iid[no_iid_steps - iid_index] / 10.0)));
					 alpha = 0.5 * acos(quant_rho[icc_index]);
					 beta = alpha * ( c_1 - c_2 ) / sqrt(2.0);
					 */
					//printf("%d\n", ps.iid_index[env][bk]);

					/* calculate the scalefactors c_1 and c_2 from the intensity differences */
					c_1 = sf_iid[no_iid_steps+iid_index[env][bk]];
					c_2 = sf_iid[no_iid_steps-iid_index[env][bk]];

					/* calculate alpha and beta using the ICC parameters */
					cosa = cos_alphas[icc_index[env][bk]];
					sina = sin_alphas[icc_index[env][bk]];

					if(iid_mode>=3) {
						if(iid_index[env][bk]<0) {
							cosb = cos_betas_fine[-iid_index[env][bk]][icc_index[env][bk]];
							sinb = -sin_betas_fine[-iid_index[env][bk]][icc_index[env][bk]];
						}
						else {
							cosb = cos_betas_fine[iid_index[env][bk]][icc_index[env][bk]];
							sinb = sin_betas_fine[iid_index[env][bk]][icc_index[env][bk]];
						}
					}
					else {
						if(iid_index[env][bk]<0) {
							cosb = cos_betas_normal[-iid_index[env][bk]][icc_index[env][bk]];
							sinb = -sin_betas_normal[-iid_index[env][bk]][icc_index[env][bk]];
						}
						else {
							cosb = cos_betas_normal[iid_index[env][bk]][icc_index[env][bk]];
							sinb = sin_betas_normal[iid_index[env][bk]][icc_index[env][bk]];
						}
					}

					ab1 = (cosb*cosa);
					ab2 = (sinb*sina);
					ab3 = (sinb*cosa);
					ab4 = (cosb*sina);

					/* h_xy: COEF */
					h11[0] = (c_2*(ab1-ab2));
					h12[0] = (c_1*(ab1+ab2));
					h21[0] = (c_2*(ab3+ab4));
					h22[0] = (c_1*(ab3-ab4));
				}
				else {
					/* type 'B' mixing as described in 8.6.4.6.2.2 */
					float sina, cosa;
					float cosg, sing;

					if(iid_mode>=3) {
						int abs_iid = Math.abs(iid_index[env][bk]);

						cosa = sincos_alphas_B_fine[no_iid_steps+iid_index[env][bk]][icc_index[env][bk]];
						sina = sincos_alphas_B_fine[30-(no_iid_steps+iid_index[env][bk])][icc_index[env][bk]];
						cosg = cos_gammas_fine[abs_iid][icc_index[env][bk]];
						sing = sin_gammas_fine[abs_iid][icc_index[env][bk]];
					}
					else {
						int abs_iid = Math.abs(iid_index[env][bk]);

						cosa = sincos_alphas_B_normal[no_iid_steps+iid_index[env][bk]][icc_index[env][bk]];
						sina = sincos_alphas_B_normal[14-(no_iid_steps+iid_index[env][bk])][icc_index[env][bk]];
						cosg = cos_gammas_normal[abs_iid][icc_index[env][bk]];
						sing = sin_gammas_normal[abs_iid][icc_index[env][bk]];
					}

					h11[0] = (COEF_SQRT2*(cosa*cosg));
					h12[0] = (COEF_SQRT2*(sina*cosg));
					h21[0] = (COEF_SQRT2*(-cosa*sing));
					h22[0] = (COEF_SQRT2*(sina*sing));
				}

				/* calculate phase rotation parameters H_xy */
				/* note that the imaginary part of these parameters are only calculated when
				 IPD and OPD are enabled
				 */
				if((enable_ipdopd)&&(bk<nr_ipdopd_par)) {

					/* ringbuffer index */
					int i = phase_hist;

					/* previous value */
					tempLeft[0] = (ipd_prev[bk][i][0]*0.25f);
					tempLeft[1] = (ipd_prev[bk][i][1]*0.25f);
					tempRight[0] = (opd_prev[bk][i][0]*0.25f);
					tempRight[1] = (opd_prev[bk][i][1]*0.25f);

					/* save current value */
					ipd_prev[bk][i][0] = ipdopd_cos_tab[Math.abs(ipd_index[env][bk])];
					ipd_prev[bk][i][1] = ipdopd_sin_tab[Math.abs(ipd_index[env][bk])];
					opd_prev[bk][i][0] = ipdopd_cos_tab[Math.abs(opd_index[env][bk])];
					opd_prev[bk][i][1] = ipdopd_sin_tab[Math.abs(opd_index[env][bk])];

					/* add current value */
					tempLeft[0] += ipd_prev[bk][i][0];
					tempLeft[1] += ipd_prev[bk][i][1];
					tempRight[0] += opd_prev[bk][i][0];
					tempRight[1] += opd_prev[bk][i][1];

					/* ringbuffer index */
					if(i==0) {
						i = 2;
					}
					i--;

					/* get value before previous */
					tempLeft[0] += (ipd_prev[bk][i][0]*0.5f);
					tempLeft[1] += (ipd_prev[bk][i][1]*0.5f);
					tempRight[0] += (opd_prev[bk][i][0]*0.5f);
					tempRight[1] += (opd_prev[bk][i][1]*0.5f);

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
				if((enable_ipdopd)&&(bk<nr_ipdopd_par)) {
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
					if((enable_ipdopd)&&(bk<nr_ipdopd_par)) {
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
						if((enable_ipdopd)&&(bk<nr_ipdopd_par)) {
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

				/* shift phase smoother's circular buffer index */
				phase_hist++;
				if(phase_hist==2) {
					phase_hist = 0;
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
		hyb.hybrid_analysis(X_left, X_hybrid_left, fbt);

		/* decorrelate mono signal */
		ps_decorrelate(X_left, X_right, X_hybrid_left, X_hybrid_right);

		/* apply mixing and phase parameters */
		ps_mix_phase(X_left, X_right, X_hybrid_left, X_hybrid_right);

		/* hybrid synthesis, to rebuild the SBR QMF matrices */
		hyb.hybrid_synthesis(X_left, X_hybrid_left, fbt);

		hyb.hybrid_synthesis(X_right, X_hybrid_right, fbt);
	}

}
