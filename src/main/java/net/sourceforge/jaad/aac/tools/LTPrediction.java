package net.sourceforge.jaad.aac.tools;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.filterbank.FilterBank;
import net.sourceforge.jaad.aac.syntax.BitStream;
import net.sourceforge.jaad.aac.syntax.ICSInfo;
import net.sourceforge.jaad.aac.syntax.ICStream;

import java.util.logging.Logger;

/**
 * Long-term prediction
 * @author in-somnia
 */
public class LTPrediction {

	static final Logger LOGGER = Logger.getLogger("jaad.aac.syntax.LTPrediction"); //for debugging

	public static final int MAX_LTP_SFB = 40;

	private static final float[] CODEBOOK = {
		0.570829f,
		0.696616f,
		0.813004f,
		0.911304f,
		0.984900f,
		1.067894f,
		1.194601f,
		1.369533f
	};

	private boolean isPresent = false;

	private final int frameLength;
	private final int[] states;
	private int coef, lag, lastBand;
	private boolean lagUpdate;
	private boolean[] shortUsed, shortLagPresent, longUsed;
	private int[] shortLag;

	public LTPrediction(int frameLength) {
		this.frameLength = frameLength;
		states = new int[4*frameLength];
	}

	public boolean isPresent() {
		return isPresent;
	}

	public void decode(BitStream in, ICSInfo info, Profile profile) {
		lag = 0;

		isPresent = in.readBool();
		if(!isPresent) {
			return;
		}

		if(profile.equals(Profile.AAC_LD)) {
			lagUpdate = in.readBool();
			if(lagUpdate)
				lag = in.readBits(10);
		}
		else
			lag = in.readBits(11);
		if(lag>(frameLength<<1))
			throw new AACException("LTP lag too large: "+lag);

		coef = in.readBits(3);

		final int windowCount = info.getWindowCount();

		if(info.isEightShortFrame()) {
			shortUsed = new boolean[windowCount];
			shortLagPresent = new boolean[windowCount];
			shortLag = new int[windowCount];
			for(int w = 0; w<windowCount; w++) {
				if((shortUsed[w] = in.readBool())) {
					shortLagPresent[w] = in.readBool();
					if(shortLagPresent[w])
						shortLag[w] = in.readBits(4);
				}
			}
		}
		else {
			lastBand = Math.min(info.getMaxSFB(), MAX_LTP_SFB);
			longUsed = new boolean[lastBand];
			for(int i = 0; i<lastBand; i++) {
				longUsed[i] = in.readBool();
			}
		}
	}

	public void process(ICStream ics, FilterBank filterBank) {

		if(!isPresent)
			return;

		float[] data = ics.getInvQuantData();

		final ICSInfo info = ics.getInfo();

		if(!info.isEightShortFrame()) {
			final int samples = frameLength<<1;
			final float[] in = new float[2048];
			final float[] out = new float[2048];

			for(int i = 0; i<samples; i++) {
				in[i] = states[samples+i-lag]*CODEBOOK[coef];
			}

			filterBank.processLTP(info.getWindowSequence(), info.getWindowShape(ICSInfo.CURRENT),
					info.getWindowShape(ICSInfo.PREVIOUS), in, out);

			ics.processTNS(out);

			final int[] swbOffsets = info.getSWBOffsets();
			final int swbOffsetMax = info.getSWBOffsetMax();
			for(int sfb = 0; sfb<lastBand; sfb++) {
				if(longUsed[sfb]) {
					int low = swbOffsets[sfb];
					int high = Math.min(swbOffsets[sfb+1], swbOffsetMax);

					for(int bin = low; bin<high; bin++) {
						data[bin] += out[bin];
					}
				}
			}
		}
	}

	public void updateState(float[] time, float[] overlap, Profile profile) {
		if(profile.equals(Profile.AAC_LD)) {
			for(int i = 0; i<frameLength; i++) {
				states[i] = states[i+frameLength];
				states[frameLength+i] = states[i+(frameLength*2)];
				states[(frameLength*2)+i] = Math.round(time[i]);
				states[(frameLength*3)+i] = Math.round(overlap[i]);
			}
		}
		else {
			for(int i = 0; i<frameLength; i++) {
				states[i] = states[i+frameLength];
				states[frameLength+i] = Math.round(time[i]);
				states[(frameLength*2)+i] = Math.round(overlap[i]);
			}
		}
		isPresent = false;
	}

	public static boolean isLTPProfile(Profile profile) {
		return profile.equals(Profile.AAC_LTP)||profile.equals(Profile.ER_AAC_LTP)||profile.equals(Profile.AAC_LD);
	}

	public void copyOf(LTPrediction ltp) {
		System.arraycopy(ltp.states, 0, states, 0, states.length);
		coef = ltp.coef;
		lag = ltp.lag;
		lastBand = ltp.lastBand;
		lagUpdate = ltp.lagUpdate;
		shortUsed = Utils.copyOf(ltp.shortUsed);
		shortLagPresent = Utils.copyOf(ltp.shortLagPresent);
		shortLag = Utils.copyOf(ltp.shortLag);
		longUsed = Utils.copyOf(ltp.longUsed);
	}

}
