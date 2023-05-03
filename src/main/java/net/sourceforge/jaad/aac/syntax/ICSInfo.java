package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.tools.Utils;
import net.sourceforge.jaad.aac.tools.ICPrediction;
import net.sourceforge.jaad.aac.tools.LTPrediction;

import java.util.logging.Logger;

public class ICSInfo implements ScaleFactorBands {

	//maximum numbers
	public static final int MAX_WINDOW_COUNT = 8;
	public static final int MAX_WINDOW_GROUP_COUNT = MAX_WINDOW_COUNT;

	static final Logger LOGGER = Logger.getLogger("jaad.aac.syntax.ICSInfo"); //for debugging

	public static final int WINDOW_SHAPE_SINE = 0;
	public static final int WINDOW_SHAPE_KAISER = 1;
	public static final int PREVIOUS = 0;
	public static final int CURRENT = 1;

	public enum WindowSequence {

		ONLY_LONG_SEQUENCE,
		LONG_START_SEQUENCE,
		EIGHT_SHORT_SEQUENCE,
		LONG_STOP_SEQUENCE;

		public static WindowSequence forInt(int i) {
			WindowSequence w;
			switch(i) {
				case 0:
					w = ONLY_LONG_SEQUENCE;
					break;
				case 1:
					w = LONG_START_SEQUENCE;
					break;
				case 2:
					w = EIGHT_SHORT_SEQUENCE;
					break;
				case 3:
					w = LONG_STOP_SEQUENCE;
					break;
				default:
					throw new AACException("unknown window sequence type");
			}
			return w;
		}
	}

	final DecoderConfig config;

	final SampleFrequency sf;
	private final int frameLength;
	private WindowSequence windowSequence;
	private int[] windowShape;
	private int maxSFB;
	//prediction
	private boolean predictionDataPresent;
	private ICPrediction icPredict;
	LTPrediction ltPredict;
	//windows/sfbs
	private int windowCount;
	private int windowGroupCount;
	private int[] windowGroupLength;
	private int swbCount;
	private int[] swbOffsets;

	public ICSInfo(DecoderConfig config) {
		this.config = config;
		this.sf = config.getSampleFrequency().getNominal();
		this.frameLength = config.getFrameLength();
		this.windowShape = new int[2];
		this.windowSequence = WindowSequence.ONLY_LONG_SEQUENCE;
		this.windowGroupLength = new int[MAX_WINDOW_GROUP_COUNT];

		if(LTPrediction.isLTPProfile(config.getProfile()))
			this.ltPredict = new LTPrediction(frameLength);
	}

	/* ========== decoding ========== */
	public void decode(BitStream in, boolean commonWindow) {

		in.skipBit(); //reserved
		windowSequence = WindowSequence.forInt(in.readBits(2));
		windowShape[PREVIOUS] = windowShape[CURRENT];
		windowShape[CURRENT] = in.readBit();

		windowGroupCount = 1;
		windowGroupLength[0] = 1;
		if(windowSequence.equals(WindowSequence.EIGHT_SHORT_SEQUENCE)) {
			maxSFB = in.readBits(4);
			for(int i = 0; i<7; i++) {
				if(in.readBool())
					windowGroupLength[windowGroupCount-1]++;
				else {
					windowGroupCount++;
					windowGroupLength[windowGroupCount-1] = 1;
				}
			}
			windowCount = 8;
			swbOffsets = SWB_OFFSET_SHORT_WINDOW[sf.getIndex()];
			swbCount = SWB_SHORT_WINDOW_COUNT[sf.getIndex()];
			predictionDataPresent = false;
		}
		else {
			maxSFB = in.readBits(6);
			windowCount = 1;
			swbOffsets = SWB_OFFSET_LONG_WINDOW[sf.getIndex()];
			swbCount = SWB_LONG_WINDOW_COUNT[sf.getIndex()];
			predictionDataPresent = in.readBool();
			if(predictionDataPresent)
				readPredictionData(in, commonWindow);
		}
	}

	private void readPredictionData(BitStream in, boolean commonWindow) {

		LOGGER.fine("prediction present");
		Profile profile = config.getProfile();

		switch(profile) {
			case AAC_MAIN:
				if(icPredict==null)
					icPredict = new ICPrediction();
				icPredict.decode(in, maxSFB, sf);
				break;
			case AAC_LTP:
			case ER_AAC_LTP:
				ltPredict.decode(in, this, profile);

				break;
			default:
				throw new AACException("unexpected profile for LTP: "+profile);
		}
	}

	/* =========== gets ============ */
	public int getMaxSFB() {
		return maxSFB;
	}

	public int getSFB() {
		return Math.min(sf.getMaximalPredictionSFB(), maxSFB);
	}

	public int getSWBCount() {
		return swbCount;
	}

	public int[] getSWBOffsets() {
		return swbOffsets;
	}

	public int getSWBOffsetMax() {
		return swbOffsets[swbCount];
	}

	public int getWindowCount() {
		return windowCount;
	}

	public int getWindowGroupCount() {
		return windowGroupCount;
	}

	public int getWindowGroupLength(int g) {
		return windowGroupLength[g];
	}

	public WindowSequence getWindowSequence() {
		return windowSequence;
	}

	public boolean isEightShortFrame() {
		return windowSequence.equals(WindowSequence.EIGHT_SHORT_SEQUENCE);
	}

	public int getWindowShape(int index) {
		return windowShape[index];
	}

	public void processICP(float[] data) {
		if(config.getProfile().equals(Profile.AAC_MAIN))
			if(icPredict!=null && predictionDataPresent)
				icPredict.process(this, data);
	}

	public void setCommonData(BitStream in, ICSInfo info) {

		windowSequence = WindowSequence.valueOf(info.windowSequence.name());
		windowShape[PREVIOUS] = windowShape[CURRENT];
		windowShape[CURRENT] = info.windowShape[CURRENT];
		maxSFB = info.maxSFB;

		predictionDataPresent = info.predictionDataPresent;
		icPredict = null;

		windowCount = info.windowCount;
		windowGroupCount = info.windowGroupCount;
		windowGroupLength = Utils.copyOf(info.windowGroupLength);
		swbCount = info.swbCount;
		swbOffsets = Utils.copyOf(info.swbOffsets);

		if(predictionDataPresent && ltPredict!=null)
			ltPredict.decode(in, this, config.getProfile());
	}
}
