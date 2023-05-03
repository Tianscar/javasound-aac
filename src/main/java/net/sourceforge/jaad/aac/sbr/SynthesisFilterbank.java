package net.sourceforge.jaad.aac.sbr;

import java.util.Arrays;

abstract class SynthesisFilterbank extends Filterbank {

	SynthesisFilterbank(int channels) {
		super(channels);
	}

	public void reset() {
		Arrays.fill(v, 0);
	}

	abstract void synthesis(int numTimeSlotsRate, float[][][] X, float[] output);
}
