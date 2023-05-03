package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.syntax.BitStream;

import static net.sourceforge.jaad.aac.sbr.HuffmanTables.*;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 13.05.21
 * Time: 18:58
 */
class Channel {

    final SBR sbr;

    boolean amp_res;

    int abs_bord_lead;
    int abs_bord_trail;
    int n_rel_lead;
    int n_rel_trail;

    int L_E;
    int L_E_prev;
    int L_Q;

    int[] t_E = new int[SBR.MAX_L_E + 1];
    int[] t_Q = new int[3];
    int[] f = new int[SBR.MAX_L_E + 1];
    int f_prev;

    float[][] G_temp_prev = new float[5][64];
    float[][] Q_temp_prev = new float[5][64];
    int GQ_ringbuf_index = 0;

    int[][] E = new int[64][SBR.MAX_L_E];
    int[] E_prev = new int[64];
    float[][] E_orig = new float[64][SBR.MAX_L_E];
    float[][] E_curr = new float[64][SBR.MAX_L_E];
    int[][] Q = new int[64][2];
    float[][] Q_div = new float[64][2];
    float[][] Q_div2 = new float[64][2];
    int[] Q_prev = new int[64];

    int l_A;

    int[] bs_invf_mode = new int[SBR.MAX_L_E];
    int[] bs_invf_mode_prev = new int[SBR.MAX_L_E];
    float[] bwArray = new float[64];
    float[] bwArray_prev = new float[64];

    int[] bs_add_harmonic = new int[64];
    int[] bs_add_harmonic_prev = new int[64];

    int index_noise_prev;
    int psi_is_prev;

    int prevEnvIsShort = -1;

    final AnalysisFilterbank qmfa;

    public static final int MAX_NTSRHFG = 40; //maximum of number_time_slots * rate + HFGen. 16*2+8
    float[][][] Xsbr = new float[MAX_NTSRHFG][64][2];

    FrameClass bs_frame_class;
    int[] bs_rel_bord = new int[9];
    int[] bs_rel_bord_0 = new int[9];
    int[] bs_rel_bord_1 = new int[9];
    int bs_pointer;
    int bs_num_rel_0;
    int bs_num_rel_1;
    int[] bs_df_env = new int[9];
    int[] bs_df_noise = new int[3];

    boolean bs_add_harmonic_flag;
    boolean bs_add_harmonic_flag_prev;

    public Channel(SBR sbr) {
        this.sbr = sbr;
        qmfa = new AnalysisFilterbank(32);
    }

    /* table 8 */
    void sbr_dtdf(BitStream ld) {

        for(int i = 0; i<L_E; i++) {
            bs_df_env[i] = ld.readBit();
        }

        for(int i = 0; i<L_Q; i++) {
            bs_df_noise[i] = ld.readBit();
        }
    }

    /* table 9 */
    void invf_mode(BitStream ld) {
        for(int n = 0; n<sbr.N_Q; n++) {
            bs_invf_mode[n] = ld.readBits(2);
        }
    }

    void couple(Channel oc, int N_Q) {

        bs_frame_class = oc.bs_frame_class;
        L_E = oc.L_E;
        L_Q = oc.L_Q;
        bs_pointer = oc.bs_pointer;

        for(int n = 0; n<=oc.L_E; n++) {
            t_E[n] = oc.t_E[n];
            f[n] = oc.f[n];
        }

        for(int n = 0; n<=oc.L_Q; n++) {
            t_Q[n] = oc.t_Q[n];
        }

        for(int n = 0; n<N_Q; n++) {
            bs_invf_mode[n] = oc.bs_invf_mode[n];
        }
    }

    /* table 10 */

    void sbr_envelope(BitStream ld, boolean coupled) {
        int delta = 0;
        int[][] t_huff, f_huff;

        if((L_E==1)&&(bs_frame_class== FrameClass.FIXFIX))
            amp_res = false;
        else
            amp_res = sbr.hdr.bs_amp_res;

        if(coupled) {
            delta = 1;
            if(amp_res) {
                t_huff = T_HUFFMAN_ENV_BAL_3_0DB;
                f_huff = F_HUFFMAN_ENV_BAL_3_0DB;
            }
            else {
                t_huff = T_HUFFMAN_ENV_BAL_1_5DB;
                f_huff = F_HUFFMAN_ENV_BAL_1_5DB;
            }
        }
        else {
            delta = 0;
            if(amp_res) {
                t_huff = T_HUFFMAN_ENV_3_0DB;
                f_huff = F_HUFFMAN_ENV_3_0DB;
            }
            else {
                t_huff = T_HUFFMAN_ENV_1_5DB;
                f_huff = F_HUFFMAN_ENV_1_5DB;
            }
        }

        for(int env = 0; env<L_E; env++) {
            if(bs_df_env[env]==0) {
                if(coupled) {
                    if(amp_res) {
                        E[0][env] = ld.readBits(5)<<delta;
                    }
                    else {
                        E[0][env] = ld.readBits(6)<<delta;
                    }
                }
                else {
                    if(amp_res) {
                        E[0][env] = ld.readBits(6)<<delta;
                    }
                    else {
                        E[0][env] = ld.readBits(7)<<delta;
                    }
                }

                for(int band = 1; band<sbr.n[f[env]]; band++) {
                    E[band][env] = (decodeHuffman(ld, f_huff)<<delta);
                }

            }
            else {
                for(int band = 0; band<sbr.n[f[env]]; band++) {
                    E[band][env] = (decodeHuffman(ld, t_huff)<<delta);
                }
            }
        }

        extract_envelope_data();
    }

    void extract_envelope_data() {

        for(int l = 0; l<L_E; l++) {
            if(bs_df_env[l]==0) {
                for(int k = 1; k<sbr.n[f[l]]; k++) {
                    E[k][l] = E[k-1][l]+E[k][l];
                    if(E[k][l]<0)
                        E[k][l] = 0;
                }

            }
            else { /* bs_df_env == 1 */

                int g = (l==0) ? f_prev : f[l-1];

                if(f[l]==g) {
                    for(int k = 0; k<sbr.n[f[l]]; k++) {
                        int prev = l==0 ? E_prev[k] : E[k][l-1];
                        E[k][l] = prev+E[k][l];
                    }
                }
                else if((g==1)&&(f[l]==0)) {

                    for(int k = 0; k<sbr.n[f[l]]; k++) {
                        for(int i = 0; i<sbr.N_high; i++) {
                            if(sbr.f_table_res[FBT.HI_RES][i]==sbr.f_table_res[FBT.LO_RES][k]) {
                                int prev = l==0 ? E_prev[i] : E[i][l-1];
                                E[k][l] = prev+E[k][l];
                            }
                        }
                    }

                }
                else if((g==0)&&(f[l]==1)) {

                    for(int k = 0; k<sbr.n[f[l]]; k++) {
                        for(int i = 0; i<sbr.N_low; i++) {
                            if((sbr.f_table_res[FBT.LO_RES][i]<=sbr.f_table_res[FBT.HI_RES][k])
                                &&(sbr.f_table_res[FBT.HI_RES][k]<sbr.f_table_res[FBT.LO_RES][i+1])) {

                                int prev = l==0 ? E_prev[i] : E[i][l-1];
                                E[k][l] = prev+E[k][l];
                            }
                        }
                    }
                }
            }
        }
    }

    /* table 11 */
    void sbr_noise(BitStream ld, boolean coupled) {
        int delta = 0;
        int[][] t_huff, f_huff;

        if(coupled) {
            delta = 1;
            t_huff = T_HUFFMAN_NOISE_BAL_3_0DB;
            f_huff = F_HUFFMAN_ENV_BAL_3_0DB;
        }
        else {
            delta = 0;
            t_huff = T_HUFFMAN_NOISE_3_0DB;
            f_huff = F_HUFFMAN_ENV_3_0DB;
        }

        for(int noise = 0; noise<L_Q; noise++) {
            if(bs_df_noise[noise]==0) {
                if(coupled) {
                    Q[0][noise] = ld.readBits(5)<<delta;
                }
                else {
                    Q[0][noise] = ld.readBits(5)<<delta;
                }
                for(int band = 1; band<sbr.N_Q; band++) {
                    Q[band][noise] = (decodeHuffman(ld, f_huff)<<delta);
                }
            }
            else {
                for(int band = 0; band<sbr.N_Q; band++) {
                    Q[band][noise] = (decodeHuffman(ld, t_huff)<<delta);
                }
            }
        }

        extract_noise_floor_data();
    }

    static int decodeHuffman(BitStream ld, int[][] t_huff) {
        int index = 0;

        while(index>=0) {
            int bit = ld.readBit();
            index = t_huff[index][bit];
        }

        return index+64;
    }

    void extract_noise_floor_data() {

        for(int l = 0; l<L_Q; l++) {
            if(bs_df_noise[l]==0) {
                for(int k = 1; k<sbr.N_Q; k++) {
                    Q[k][l] = Q[k][l]+Q[k-1][l];
                }
            }
            else {
                if(l==0) {
                    for(int k = 0; k<sbr.N_Q; k++) {
                        Q[k][l] = Q_prev[k]+Q[k][0];
                    }
                }
                else {
                    for(int k = 0; k<sbr.N_Q; k++) {
                        Q[k][l] = Q[k][l-1]+Q[k][l];
                    }
                }
            }
        }
    }

    /* table 7 */
    int sbr_grid(BitStream ld) {
        int result;
        int saved_L_E = L_E;
        int saved_L_Q = L_Q;
        FrameClass saved_frame_class = bs_frame_class;

        bs_frame_class = FrameClass.read(ld);

        switch(bs_frame_class) {
            case FIXFIX: {
                int i = ld.readBits(2);

                int bs_num_env = Math.min(1 << i, 5);

                i = ld.readBit();
                for (int env = 0; env < bs_num_env; env++) {
                    f[env] = i;
                }

                L_E = Math.min(bs_num_env, 4);

                abs_bord_lead = 0;
                abs_bord_trail = sbr.numTimeSlots;
                n_rel_lead = bs_num_env - 1;
                n_rel_trail = 0;
                break;
            }
            case FIXVAR: {
                int bs_abs_bord = ld.readBits(2) + sbr.numTimeSlots;
                int bs_num_env = ld.readBits(2) + 1;

                for (int rel = 0; rel < bs_num_env - 1; rel++) {
                    bs_rel_bord[rel] = 2 * ld.readBits(2) + 2;
                }
                int i = sbr_log2(bs_num_env + 1);
                bs_pointer = ld.readBits(i);

                for (int env = 0; env < bs_num_env; env++) {
                    f[bs_num_env - env - 1] = ld.readBit();
                }

                L_E = Math.min(bs_num_env, 4);

                abs_bord_lead = 0;
                abs_bord_trail = bs_abs_bord;
                n_rel_lead = 0;
                n_rel_trail = bs_num_env - 1;
                break;
            }
            case VARFIX: {
                int bs_abs_bord = ld.readBits(2);
                int bs_num_env = ld.readBits(2) + 1;

                for (int rel = 0; rel < bs_num_env - 1; rel++) {
                    bs_rel_bord[rel] = 2 * ld.readBits(2) + 2;
                }
                int i = sbr_log2(bs_num_env + 1);
                bs_pointer = ld.readBits(i);

                for (int env = 0; env < bs_num_env; env++) {
                    f[env] = ld.readBit();
                }

                L_E = Math.min(bs_num_env, 4);

                abs_bord_lead = bs_abs_bord;
                abs_bord_trail = sbr.numTimeSlots;
                n_rel_lead = bs_num_env - 1;
                n_rel_trail = 0;
                break;
            }
            case VARVAR: {
                int bs_abs_bord = ld.readBits(2);
                int bs_abs_bord_1 = ld.readBits(2) + sbr.numTimeSlots;
                bs_num_rel_0 = ld.readBits(2);
                bs_num_rel_1 = ld.readBits(2);

                int bs_num_env = Math.min(5, bs_num_rel_0 + bs_num_rel_1 + 1);

                for (int rel = 0; rel < bs_num_rel_0; rel++) {
                    bs_rel_bord_0[rel] = 2 * ld.readBits(2) + 2;
                }
                for (int rel = 0; rel < bs_num_rel_1; rel++) {
                    bs_rel_bord_1[rel] = 2 * ld.readBits(2) + 2;
                }
                int i = sbr_log2(bs_num_rel_0 + bs_num_rel_1 + 2);
                bs_pointer = ld.readBits(i);

                for (int env = 0; env < bs_num_env; env++) {
                    f[env] = ld.readBit();
                }

                L_E = Math.min(bs_num_env, 5);

                abs_bord_lead = bs_abs_bord;
                abs_bord_trail = bs_abs_bord_1;
                n_rel_lead = bs_num_rel_0;
                n_rel_trail = bs_num_rel_1;

                break;
            }
        }

        if(L_E<=0)
            return 1;

        if(L_E>1)
            L_Q = 2;
        else
            L_Q = 1;

        /* TODO: this code can probably be integrated into the code above! */
        if((result = envelope_time_border_vector())>0) {
            bs_frame_class = saved_frame_class;
            L_E = saved_L_E;
            L_Q = saved_L_Q;
            return result;
        }
        
       noise_floor_time_border_vector();

        return 0;
    }

    private static final int log2tab[] = {0, 0, 1, 2, 2, 3, 3, 3, 3, 4};

    /* integer log[2](x): input range [0,10) */
    private static int sbr_log2(int val) {

        if(val<10&&val>=0)
            return log2tab[val];
        else
            return 0;
    }


    private final int[] eTmp = new int[6];

    /* function constructs new time border vector */
    /* first build into temp vector to be able to use previous vector on error */
    int envelope_time_border_vector() {

        eTmp[0] = sbr.rate*abs_bord_lead;
        eTmp[L_E] = sbr.rate*abs_bord_trail;

        switch(bs_frame_class) {
            case FIXFIX:
                switch(L_E) {
                    case 4:
                        int temp = (sbr.numTimeSlots/4);
                        eTmp[3] = sbr.rate*3*temp;
                        eTmp[2] = sbr.rate*2*temp;
                        eTmp[1] = sbr.rate*temp;
                        break;
                    case 2:
                        eTmp[1] = sbr.rate*(sbr.numTimeSlots/2);
                        break;
                    default:
                        break;
                }
                break;

            case FIXVAR:
                if(L_E>1) {
                    int i = L_E;
                    int border = abs_bord_trail;

                    for(int l = 0; l<(L_E-1); l++) {
                        if(border<bs_rel_bord[l])
                            return 1;

                        border -= bs_rel_bord[l];
                        eTmp[--i] = sbr.rate*border;
                    }
                }
                break;

            case VARFIX:
                if(L_E>1) {
                    int i = 1;
                    int border = abs_bord_lead;

                    for(int l = 0; l<(L_E-1); l++) {
                        border += bs_rel_bord[l];

                        if(sbr.rate*border+sbr.tHFAdj>sbr.numTimeSlotsRate+sbr.tHFGen)
                            return 1;

                        eTmp[i++] = sbr.rate*border;
                    }
                }
                break;

            case VARVAR:
                if(bs_num_rel_0!=0) {
                    int i = 1;
                    int border = abs_bord_lead;

                    for(int l = 0; l<bs_num_rel_0; l++) {
                        border += bs_rel_bord_0[l];

                        if(sbr.rate*border+sbr.tHFAdj>sbr.numTimeSlotsRate+sbr.tHFGen)
                            return 1;

                        eTmp[i++] = sbr.rate*border;
                    }
                }

                if(bs_num_rel_1!=0) {
                    int i = L_E;
                    int border = abs_bord_trail;

                    for(int l = 0; l<bs_num_rel_1; l++) {
                        if(border<bs_rel_bord_1[l])
                            return 1;

                        border -= bs_rel_bord_1[l];
                        eTmp[--i] = sbr.rate*border;
                    }
                }
                break;
        }

        /* no error occured, we can safely use this t_E vector */
        System.arraycopy(eTmp, 0, t_E, 0, 6);

        return 0;
    }

    void noise_floor_time_border_vector() {
        t_Q[0] = t_E[0];

        if(L_E==1) {
            t_Q[1] = t_E[1];
            t_Q[2] = 0;
        }
        else {
            int index = middleBorder();
            t_Q[1] = t_E[index];
            t_Q[2] = t_E[L_E];
        }
    }

    private int middleBorder() {
        int retval = 0;

        switch(bs_frame_class) {
            case FIXFIX:
                retval = L_E/2;
                break;
            case VARFIX:
                if(bs_pointer==0)
                    retval = 1;
                else if(bs_pointer==1)
                    retval = L_E-1;
                else
                    retval = bs_pointer-1;
                break;
            case FIXVAR:
            case VARVAR:
                if(bs_pointer>1)
                    retval = L_E+1-bs_pointer;
                else
                    retval = L_E-1;
                break;
        }

        return (retval>0) ? retval : 0;
    }
    
    
    void process_channel(float[] channel_buf, float[][][] X, boolean reset) {
   
   		sbr.bsco = 0;
   
   		boolean dont_process = sbr.hdr==null;
   
   		/* subband analysis */
        qmfa.sbr_qmf_analysis_32(sbr.numTimeSlotsRate, channel_buf,
                Xsbr, sbr.tHFGen, dont_process ? 32 : sbr.kx);

   		if(!dont_process) {
   			/* insert high frequencies here */
   			/* hf generation using patching */
   			HFGeneration.hf_generation(Xsbr, Xsbr, this, reset);
   
   
   			/* hf adjustment */
   			HFAdjustment.hf_adjustment(sbr, Xsbr, this);
   		}
   
   		if(dont_process) {
   			for(int l = 0; l<sbr.numTimeSlotsRate; l++) {
   				for(int k = 0; k<32; k++) {
   					X[l][k][0] = Xsbr[l+sbr.tHFAdj][k][0];
   					X[l][k][1] = Xsbr[l+sbr.tHFAdj][k][1];
   				}
   				for(int k = 32; k<64; k++) {
   					X[l][k][0] = 0;
   					X[l][k][1] = 0;
   				}
   			}
   		}
   		else {
   			for(int l = 0; l<sbr.numTimeSlotsRate; l++) {
   				int kx_band, M_band, bsco_band;
   
   				if(l<t_E[0]) {
   					kx_band = sbr.kx_prev;
   					M_band = sbr.M_prev;
   					bsco_band = sbr.bsco_prev;
   				}
   				else {
   					kx_band = sbr.kx;
   					M_band = sbr.M;
   					bsco_band = sbr.bsco;
   				}
   
   				for(int k = 0; k<kx_band+bsco_band; k++) {
   					X[l][k][0] = Xsbr[l+sbr.tHFAdj][k][0];
   					X[l][k][1] = Xsbr[l+sbr.tHFAdj][k][1];
   				}
   				for(int k = kx_band+bsco_band; k<kx_band+M_band; k++) {
   					X[l][k][0] = Xsbr[l+sbr.tHFAdj][k][0];
   					X[l][k][1] = Xsbr[l+sbr.tHFAdj][k][1];
   				}
   				for(int k = Math.max(kx_band+bsco_band, kx_band+M_band); k<64; k++) {
   					X[l][k][0] = 0;
   					X[l][k][1] = 0;
   				}
   			}
   		}
   	}
}
