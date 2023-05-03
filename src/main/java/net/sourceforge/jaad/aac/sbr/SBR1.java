package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 13.05.21
 * Time: 19:12
 */
public class SBR1 extends SBR {

	static final int EXTENSION_ID_PS = 2;

    final Channel ch0;

   	final SynthesisFilterbank qmfs0;
   	SynthesisFilterbank qmfs1;

	PS ps;

    public SBR1(DecoderConfig config) {
        super(config);

        ch0 = new Channel(this);
        qmfs0 = openFilterbank();
    }


    /* table 5 */
   	protected int sbr_data(BitStream ld) {
   		int result;

   		if(ld.readBool()) {
   			ld.readBits(4); //reserved
   		}

   		if((result = ch0.sbr_grid(ld))>0)
   			return result;

		ch0.sbr_dtdf(ld);
   		ch0.invf_mode(ld);
		ch0.sbr_envelope(ld,false);
		ch0.sbr_noise(ld,false);

   		NoiseEnvelope.dequantChannel(this, ch0);

   		Arrays.fill(ch0.bs_add_harmonic, 0, 64, 0);

   		ch0.bs_add_harmonic_flag = ld.readBool();
   		if(ch0.bs_add_harmonic_flag)
   			sinusoidal_coding(ld, ch0);

		readExtendedData(ld);

   		return 0;
   	}

	protected void sbr_extension(BitStream ld, int bs_extension_id) {
   		if(bs_extension_id==EXTENSION_ID_PS  && config.isPSEnabled()) {
			if(ps==null) {
				this.ps = config.openPS(this);
				this.qmfs1 = openFilterbank();
			}

			ps.decode(ld);

		} else
			super.sbr_extension(ld, bs_extension_id);
	}

	public void process(float[] left_chan, float[] right_chan) {
		if(isPSUsed()) {
			processPS(left_chan, right_chan);
		} else {
			process(left_chan);
			System.arraycopy(left_chan, 0, right_chan, 0, right_chan.length);
		}
	}


	private void process(float[] channel) {
		float[][][] X = new float[MAX_NTSR][64][2];

		ch0.process_channel(channel, X, this.reset);

		/* subband synthesis */
		qmfs0.synthesis(numTimeSlotsRate, X, channel);

		if(this.hdr!=null) {
			sbr_save_prev_data(ch0);
		}

		sbr_save_matrix(ch0);

		this.frame++;
	}

	private int processPS(float[] left_channel, float[] right_channel) {
		int ret = 0;
		float[][][] X_left = new float[MAX_NTSR+6][64][2];
		float[][][] X_right = new float[MAX_NTSR+6][64][2];

		ch0.process_channel(left_channel, X_left, this.reset);

		/* copy some extra data for PS */
		for(int l = this.numTimeSlotsRate; l<this.numTimeSlotsRate+6; l++) {
			for(int k = 0; k<5; k++) {
				X_left[l][k][0] = ch0.Xsbr[this.tHFAdj+l][k][0];
				X_left[l][k][1] = ch0.Xsbr[this.tHFAdj+l][k][1];
			}
		}

		/* perform parametric stereo */
		ps.process(X_left, X_right);

		/* subband synthesis */

		qmfs0.synthesis(numTimeSlotsRate, X_left, left_channel);
		qmfs1.synthesis(numTimeSlotsRate, X_right, right_channel);

		if(this.hdr!=null&&ret==0) {
			sbr_save_prev_data(ch0);
		}

		sbr_save_matrix(ch0);

		this.frame++;

		return 0;
	}

	public boolean isPSUsed() {
		return ps!=null && ps.isDataAvailable();
	}

}
