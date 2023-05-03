package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.logging.Logger;

abstract public class SBR {

	static final Logger LOGGER = Logger.getLogger("jaad.aac.sbr.SBR"); //for debugging

	static final int MAX_NTSR = 32; //max number_time_slots * rate, ok for DRM and not DRM mode
	static final int MAX_M = 49; //maximum value for M
	public static final int MAX_L_E = 5; //maximum value for L_E

	static final int EXT_SBR_DATA = 13;
	static final int EXT_SBR_DATA_CRC = 14;
	static final int NO_TIME_SLOTS_960 = 15;
	static final int NO_TIME_SLOTS = 16;
	static final int RATE = 2;
	static final int NOISE_FLOOR_OFFSET = 6;
	static final int T_HFGEN = 8;
	static final int T_HFADJ = 2;

	protected final DecoderConfig config;

	private final boolean downSampled;

	public boolean isSBRDownSampled() {
		return downSampled;
	}

	SynthesisFilterbank openFilterbank() {
		return downSampled ? new SynthesisFilterbank32() : new SynthesisFilterbank64();
	}

	final SampleFrequency sample_rate;

	boolean valid = false;

	public void invalidate() {
		valid = false;
	}

	public boolean isValid() {
		return valid;
	}

	int rate;

	int k0;
	int kx;
	int M;
	int N_master;
	int N_high;
	int N_low;
	int N_Q;
	int[] N_L = new int[4];
	int[] n = new int[2];

	int[] f_master = new int[64];
	int[][] f_table_res = new int[2][64];
	int[] f_table_noise = new int[64];
	int[][] f_table_lim = new int[4][64];

	int[] table_map_k_to_g = new int[64];

	int kx_prev;
	int bsco;
	int bsco_prev;
	int M_prev;

	boolean reset;
	int frame;

	int noPatches;
	int[] patchNoSubbands = new int[64];
	int[] patchStartSubband = new int[64];

	public final int numTimeSlotsRate;
	public final int numTimeSlots;

	int tHFGen;
	int tHFAdj;

	/* to get it compiling
	/* we'll see during the coding of all the tools, whether these are all used or not.
	 */
	int bs_sbr_crc_bits;

	Header hdr = null;
	Header hdr_saved = null;
	
	int bs_samplerate_mode;

	public SBR(DecoderConfig config) {
		this.config = config;
		this.downSampled = !config.setSBRPresent();

		this.sample_rate = config.getOutputFrequency().getNominal();

		// todo: what is this?
		this.bs_samplerate_mode = 1;
		this.rate = (this.bs_samplerate_mode!=0) ? 2 : 1;

		this.tHFGen = T_HFGEN;
		this.tHFAdj = T_HFADJ;

		this.bsco = 0;
		this.bsco_prev = 0;
		this.M_prev = 0;

		if(config.isSmallFrameUsed()) {
			this.numTimeSlotsRate = RATE*NO_TIME_SLOTS_960;
			this.numTimeSlots = NO_TIME_SLOTS_960;
		}
		else {
			this.numTimeSlotsRate = RATE*NO_TIME_SLOTS;
			this.numTimeSlots = NO_TIME_SLOTS;
		}
	}

	int calc_sbr_tables(Header hdr) {
		int result = 0;

		/* calculate the Master Frequency Table */
		k0 = FBT.qmf_start_channel(hdr.bs_start_freq, this.bs_samplerate_mode, this.sample_rate);
		int k2 = FBT.qmf_stop_channel(hdr.bs_stop_freq, this.sample_rate, k0);

		/* check k0 and k2 */
		if(this.sample_rate.getFrequency()>=48000) {
			if((k2-k0)>32)
				result += 1;
		}
		else if(this.sample_rate.getFrequency()<=32000) {
			if((k2-k0)>48)
				result += 1;
		}
		else { /* (sbr.sample_rate == 44100) */

			if((k2-k0)>45)
				result += 1;
		}

		if(hdr.bs_freq_scale==0) {
			result += FBT.master_frequency_table_fs0(this, k0, k2, hdr.bs_alter_scale);
		}
		else {
			result += FBT.master_frequency_table(this, k0, k2, hdr.bs_freq_scale, hdr.bs_alter_scale);
		}
		result += FBT.derived_frequency_table(this, hdr.bs_xover_band, k2);

		result = (result>0) ? 1 : 0;

		return result;
	}

	/* table 2 */
	public void decode(BitStream ld, boolean crc) {

		if(crc) {
			this.bs_sbr_crc_bits = ld.readBits(10);
		} else
			this.bs_sbr_crc_bits = -1;

		reset = readHeader(ld);

		if(reset) {
			int rt = calc_sbr_tables(this.hdr);

			/* if an error occurred with the new header values revert to the old ones */
			if (rt > 0) {
				calc_sbr_tables(swapHeaders());
			}
		}

		if(this.hdr!=null) {
			int result = sbr_data(ld);

			valid = (result==0);
		} else
			valid = true;
	}

	/**
	 * Save current header and return the previously saved header.
	 * @return the saved header.
	 */
	private Header swapHeaders() {

		// save current header and recycle old one (if exists)
		Header hdr = this.hdr_saved;
		this.hdr_saved = this.hdr;

		if(hdr==null)
			hdr = new Header();
		this.hdr = hdr;

		return hdr;
	}

	/**
	 * Read a new header and return if the header parameter changed.
	 * See: 5.3.1 Decoding process.
	 * 
	 * @param ld input data.
	 * @return true if relevant parameters changed.
	 */

	private boolean readHeader(BitStream ld) {
		boolean bs_header_flag = ld.readBool();

		if(bs_header_flag) {
			Header hdr = swapHeaders();
			hdr.decode(ld);
			return hdr.differs(hdr_saved);
		} else
			return false;
	}

	abstract protected int sbr_data(BitStream ld);

	public abstract void process(float[] left_chan, float[] right_chan);

	protected void readExtendedData(BitStream ld) {
		boolean bs_extended_data = ld.readBool();
		if(bs_extended_data) {

			int cnt = ld.readBits(4);
			if(cnt==15) {
				cnt += ld.readBits(8);
			}

			ld = ld.readSubStream(8*cnt);
			while(ld.getBitsLeft()>7) {
				int bs_extension_id = ld.readBits(2);
				sbr_extension(ld, bs_extension_id);
			}
		}
	}

	protected void sbr_extension(BitStream ld, int bs_extension_id) {

	}

	/* table 12 */
	protected void sinusoidal_coding(BitStream ld, Channel ch) {

		for(int n = 0; n<this.N_high; n++) {
			ch.bs_add_harmonic[n] = ld.readBit();
		}
	}

	protected void sbr_save_prev_data(Channel ch) {

		/* save data for next frame */
		this.kx_prev = this.kx;
		this.M_prev = this.M;
		this.bsco_prev = this.bsco;

		ch.L_E_prev = ch.L_E;

		/* sbr.L_E[ch] can become 0 on files with bit errors */
		if(ch.L_E<=0)
			throw new AACException("L_E<0");

		ch.f_prev = ch.f[ch.L_E-1];
		for(int i = 0; i<MAX_M; i++) {
			ch.E_prev[i] = ch.E[i][ch.L_E-1];
			ch.Q_prev[i] = ch.Q[i][ch.L_Q-1];
		}

		for(int i = 0; i<MAX_M; i++) {
			ch.bs_add_harmonic_prev[i] = ch.bs_add_harmonic[i];
		}
		ch.bs_add_harmonic_flag_prev = ch.bs_add_harmonic_flag;

		if(ch.l_A==ch.L_E)
			ch.prevEnvIsShort = 0;
		else
			ch.prevEnvIsShort = -1;
	}

	protected void sbr_save_matrix(Channel ch) {

		for(int i = 0; i<this.tHFGen; i++) {
			for(int j = 0; j<64; j++) {
				ch.Xsbr[i][j][0] = ch.Xsbr[i+numTimeSlotsRate][j][0];
				ch.Xsbr[i][j][1] = ch.Xsbr[i+numTimeSlotsRate][j][1];
			}
		}
		for(int i = this.tHFGen; i<Channel.MAX_NTSRHFG; i++) {
			for(int j = 0; j<64; j++) {
				ch.Xsbr[i][j][0] = 0;
				ch.Xsbr[i][j][1] = 0;
			}
		}
	}

	public static void upsample(float[] data) {

		for(int i=data.length/2-1; i>0; --i) {
			float v = data[i];
			data[2*i] = v;
			data[2*i+1] = v;
		}
	}
}
