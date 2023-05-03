package net.sourceforge.jaad.aac;

import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.List;

/**
 * An enumeration that represents all possible sample frequencies AAC data can 
 * have.
 * 
 * @author in-somnia
 */
public enum SampleFrequency implements SampleRate {

	SF_96000(0, 96000, new int[]{33, 512}, new int[]{31, 9}),
	SF_88200(1, 88200, new int[]{33, 512}, new int[]{31, 9}),
	SF_64000(2, 64000, new int[]{38, 664}, new int[]{34, 10}),
	SF_48000(3, 48000, new int[]{40, 672}, new int[]{40, 14}),
	SF_44100(4, 44100, new int[]{40, 672}, new int[]{42, 14}),
	SF_32000(5, 32000, new int[]{40, 672}, new int[]{51, 14}),
	SF_24000(6, 24000, new int[]{41, 652}, new int[]{46, 14}),
	SF_22050(7, 22050, new int[]{41, 652}, new int[]{46, 14}),
	SF_16000(8, 16000, new int[]{37, 664}, new int[]{42, 14}),
	SF_12000(9, 12000, new int[]{37, 664}, new int[]{42, 14}),
	SF_11025(10, 11025, new int[]{37, 664}, new int[]{42, 14}),
	SF_8000(11, 8000, new int[]{34, 664}, new int[]{39, 14});

	public static final SampleFrequency SF_NONE = null;

	public static final int ESCAPE_INDEX = 0x0f;

	public static final List<SampleFrequency> TABLE = List.of(values());

	/**
	 * Returns a sample frequency instance for the given index. If the index
	 * is not between 0 and 11 inclusive, SAMPLE_FREQUENCY_NONE is returned.
	 * @return a sample frequency with the given index
	 */
	public static SampleFrequency forInt(int i) {

		if(i>=0&&i<TABLE.size())
			return TABLE.get(i);
		else
			return SF_NONE;
	}

	public SampleRate forFrequency(int freq) {
		if(freq==this.frequency)
			return this;

		return new SampleRate() {

			@Override
			public int getFrequency() {
				return frequency;
			}

			@Override
			public SampleFrequency getNominal() {
				return SampleFrequency.this;
			}

			public SampleRate duplicated() {
				SampleFrequency duplicate = SampleFrequency.this.duplicated();
				return duplicate==SF_NONE ? SF_NONE : duplicate.forFrequency(2*frequency);
			}
		};

	}

	public static SampleFrequency nominalFrequency(int freq) {

		SampleFrequency result = null;
		float dev = Float.POSITIVE_INFINITY;

		for (SampleFrequency sf : values()) {

			// calculate relative deviation
			float d = sf.getDeviationTo(freq);

			// direct match
			if(d==0)
				return sf;

			// better match
			if(d<dev) {
				result = sf;
				dev = d;
			}

			// no better match to be expected as in decreasing order
			if(sf.frequency<freq)
				break;
		}

		return result;
	}
	private final int index, frequency;
	private final int[] prediction, maxTNS_SFB;

	SampleFrequency(int index, int freqency, int[] prediction, int[] maxTNS_SFB) {
		this.index = index;
		this.frequency = freqency;
		this.prediction = prediction;
		this.maxTNS_SFB = maxTNS_SFB;
	}

	public SampleFrequency getNominal() {
		return this;
	}

	public static SampleFrequency decode(BitStream in) {
		int sfIndex = in.readBits(4);
		return forInt(sfIndex);
	}

	/**
	 * Returns this sample frequency's index between 0 (96000) and 11 (8000)
	 * or -1 if this is SAMPLE_FREQUENCY_NONE.
	 * @return the sample frequency's index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the sample frequency as integer value. This may be a value
	 * between 96000 and 8000, or 0 if this is SAMPLE_FREQUENCY_NONE.
	 * @return the sample frequency
	 */
	public int getFrequency() {
		return frequency;
	}

	@Override
	public SampleFrequency duplicated() {
		return index<3 ? SF_NONE : TABLE.get(index-3);
	}

	/**
	 * Return the relative deviation of a given frequency compared to this nominal frequency.
	 * @param frequency to compare.
	 * @return relative deviation.
	 */
	public float getDeviationTo(int frequency) {
		return ((float)frequency - this.frequency) / this.frequency;
	}

	/**
	 * Returns the highest scale factor band allowed for ICPrediction at this
	 * sample frequency.
	 * This method is mainly used internally.
	 * @return the highest prediction SFB
	 */
	public int getMaximalPredictionSFB() {
		return prediction[0];
	}

	/**
	 * Returns the number of predictors allowed for ICPrediction at this
	 * sample frequency.
	 * This method is mainly used internally.
	 * @return the number of ICPredictors
	 */
	public int getPredictorCount() {
		return prediction[1];
	}

	/**
	 * Returns the highest scale factor band allowed for TNS at this
	 * sample frequency.
	 * This method is mainly used internally.
	 * @return the highest SFB for TNS
	 */
	public int getMaximalTNS_SFB(boolean shortWindow) {
		return maxTNS_SFB[shortWindow ? 1 : 0];
	}

	/**
	 * Returns a string representation of this sample frequency.
	 * The method is identical to <code>getDescription()</code>.
	 * @return the sample frequency's description
	 */
	@Override
	public String toString() {
		return Integer.toString(frequency);
	}
}
