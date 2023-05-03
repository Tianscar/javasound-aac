package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 13.05.21
 * Time: 19:24
 */
public class SBR2 extends SBR {

    final Channel ch0;
   	final Channel ch1;

	boolean bs_coupling;

   	final SynthesisFilterbank qmfs0;
   	SynthesisFilterbank qmfs1;

    public SBR2(DecoderConfig config) {
        super(config);

        ch0 = new Channel(this);
        qmfs0 = openFilterbank();

        ch1 = new Channel(this);
        qmfs1 = openFilterbank();
    }

    /* table 6 */
    protected int sbr_data(BitStream ld) {
   		int result;

   		if(ld.readBool()) {
   			//reserved
   			ld.readBits(4);
   			ld.readBits(4);
   		}

   		this.bs_coupling = ld.readBool();

   		if(this.bs_coupling) {
   			if((result = ch0.sbr_grid(ld))>0)
   				return result;

   			ch0.sbr_dtdf(ld);
   			ch1.sbr_dtdf(ld);
			ch0.invf_mode(ld);

			/* need to copy some data from left to right */
			ch1.couple(ch0, N_Q);

   			ch0.sbr_envelope(ld,false);
   			ch0.sbr_noise(ld,false);

   			ch1.sbr_envelope(ld, this.bs_coupling);
   			ch1.sbr_noise(ld, this.bs_coupling);

   			Arrays.fill(ch0.bs_add_harmonic, 0, 64, 0);
   			Arrays.fill(ch1.bs_add_harmonic, 0, 64, 0);

   			ch0.bs_add_harmonic_flag = ld.readBool();
   			if(ch0.bs_add_harmonic_flag)
   				sinusoidal_coding(ld, ch0);

   			ch1.bs_add_harmonic_flag = ld.readBool();
   			if(ch1.bs_add_harmonic_flag)
   				sinusoidal_coding(ld, ch1);
   		}
   		else {
   			int[] saved_t_E = new int[6], saved_t_Q = new int[3];
   			int saved_L_E = ch0.L_E;
   			int saved_L_Q = ch0.L_Q;
   			FrameClass saved_frame_class = ch0.bs_frame_class;

   			for(int n = 0; n<saved_L_E; n++) {
   				saved_t_E[n] = ch0.t_E[n];
   			}
   			for(int n = 0; n<saved_L_Q; n++) {
   				saved_t_Q[n] = ch0.t_Q[n];
   			}

   			if((result = ch0.sbr_grid(ld))>0)
   				return result;

		   if((result = ch1.sbr_grid(ld))>0) {
   				/* restore first channel data as well */
   				ch0.bs_frame_class = saved_frame_class;
   				ch0.L_E = saved_L_E;
   				ch0.L_Q = saved_L_Q;
   				for(int n = 0; n<6; n++) {
   					ch0.t_E[n] = saved_t_E[n];
   				}
   				for(int n = 0; n<3; n++) {
   					ch0.t_Q[n] = saved_t_Q[n];
   				}

   				return result;
   			}
			ch0.sbr_dtdf(ld);
			ch1.sbr_dtdf(ld);
			ch0.invf_mode(ld);
			ch1.invf_mode(ld);
   			ch0.sbr_envelope(ld, false);
   			ch1.sbr_envelope(ld, false);
   			ch0.sbr_noise(ld, this.bs_coupling);
   			ch1.sbr_noise(ld, this.bs_coupling);

   			Arrays.fill(ch0.bs_add_harmonic, 0, 64, 0);
   			Arrays.fill(ch1.bs_add_harmonic, 0, 64, 0);

   			ch0.bs_add_harmonic_flag = ld.readBool();
   			if(ch0.bs_add_harmonic_flag)
   				sinusoidal_coding(ld, ch0);

   			ch1.bs_add_harmonic_flag = ld.readBool();
   			if(ch1.bs_add_harmonic_flag)
   				sinusoidal_coding(ld, ch1);
   		}

   		if(!this.bs_coupling) {
			NoiseEnvelope.dequantChannel(this, ch0);
			NoiseEnvelope.dequantChannel(this, ch1);
		} else {
			NoiseEnvelope.unmap(this);
		}

		readExtendedData(ld);

   		return 0;
   	}

	public void process(float[] left_chan, float[] right_chan) {
		float[][][] X = new float[MAX_NTSR][64][2];

		ch0.process_channel(left_chan, X, this.reset);
		/* subband synthesis */
		qmfs0.synthesis(numTimeSlotsRate, X, left_chan);

		ch1.process_channel(right_chan, X, false);
		/* subband synthesis */
		qmfs1.synthesis(numTimeSlotsRate, X, right_chan);

		if(this.hdr!=null) {
			sbr_save_prev_data(ch0);
			sbr_save_prev_data(ch1);
		}

		sbr_save_matrix(ch0);
		sbr_save_matrix(ch1);

		this.frame++;
	}
}
