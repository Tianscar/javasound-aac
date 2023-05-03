package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.ChannelConfiguration;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.error.RVLC;
import net.sourceforge.jaad.aac.filterbank.FilterBank;
import net.sourceforge.jaad.aac.gain.GainControl;
import net.sourceforge.jaad.aac.huffman.HCB;
import net.sourceforge.jaad.aac.huffman.Huffman;
import net.sourceforge.jaad.aac.tools.TNS;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: apply pulse data
public class ICStream implements HCB, ScaleFactorTable, IQTable {

	static final Logger LOGGER = Logger.getLogger("jaad.aac.syntax.ICStream"); //for debugging

	public static final int MAX_SECTIONS = 120;

	private static final int SF_DELTA = 60;
	private static final int SF_OFFSET = 200;
	private static int randomState = 0x1F2E3D4C;
	private final int frameLength;
	//always needed
	private final ICSInfo info;
	private final int[] sfbCB;
	private final int[] sectEnd;
	private final float[] iqData;
	private final float[] scaleFactors;
	private int globalGain;
	private boolean pulseDataPresent, tnsDataPresent, gainControlPresent;
	//only allocated if needed
	private TNS tns;
	private GainControl gainControl;
	private int[] pulseOffset, pulseAmp;
	private int pulseCount;
	private int pulseStartSWB;
	//error resilience
	private boolean noiseUsed;
	private int reorderedSpectralDataLen, longestCodewordLen;
	private RVLC rvlc;

	private float[] overlap;

	public ICStream(DecoderConfig config) {
		this.frameLength = config.getFrameLength();
		this.info = new ICSInfo(config);
		this.sfbCB = new int[MAX_SECTIONS];
		this.sectEnd = new int[MAX_SECTIONS];
		this.iqData = new float[frameLength];
		this.scaleFactors = new float[MAX_SECTIONS];
		this.overlap = new float[frameLength];
	}

	/* ========= decoding ========== */
	public void decode(BitStream in, boolean commonWindow, DecoderConfig conf) {
		if(conf.isScalefactorResilienceUsed()&&rvlc==null)
			rvlc = new RVLC();

		final boolean er = conf.getProfile().isErrorResilientProfile();

		globalGain = in.readBits(8);

		if(!commonWindow)
			info.decode(in, commonWindow);

		decodeSectionData(in, conf.isSectionDataResilienceUsed());

		//if(conf.isScalefactorResilienceUsed()) rvlc.decode(in, this, scaleFactors);
		/*else*/ decodeScaleFactors(in);

		pulseDataPresent = in.readBool();
		if(pulseDataPresent) {
			if(info.isEightShortFrame())
				throw new AACException("pulse data not allowed for short frames");

			LOGGER.log(Level.FINE, "PULSE");
			decodePulseData(in);
		}

		tnsDataPresent = in.readBool();
		if(tnsDataPresent&&!er) {
			if(tns==null)
				tns = new TNS();
			tns.decode(in, info);
		}

		gainControlPresent = in.readBool();
		if(gainControlPresent) {
			if(gainControl==null)
				gainControl = new GainControl(frameLength);
			LOGGER.log(Level.FINE, "GAIN");
			gainControl.decode(in, info.getWindowSequence());
		}

		//RVLC spectral data
		//if(conf.isScalefactorResilienceUsed()) rvlc.decodeScalefactors(this, in, scaleFactors);

		if(conf.isSpectralDataResilienceUsed()) {
			int max = (conf.getChannelConfiguration()==ChannelConfiguration.STEREO) ? 6144 : 12288;
			reorderedSpectralDataLen = Math.max(in.readBits(14), max);
			longestCodewordLen = Math.max(in.readBits(6), 49);
			//HCR.decodeReorderedSpectralData(this, in, data, conf.isSectionDataResilienceUsed());
		}
		else
			decodeSpectralData(in);
	}

	public void decodeSectionData(BitStream in, boolean sectionDataResilienceUsed) {
		Arrays.fill(sfbCB, 0);
		Arrays.fill(sectEnd, 0);
		final int bits = info.isEightShortFrame() ? 3 : 5;
		final int escVal = (1<<bits)-1;

		final int windowGroupCount = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();

		int idx = 0;

		for(int g = 0; g<windowGroupCount; g++) {
			for(int k=0; k<maxSFB; ) {
				int end = k;
				int cb = in.readBits(4);
				if(cb==12)
					throw new AACException("invalid huffman codebook: 12");

				int incr;
				do {
					incr = in.readBits(bits);
					end += incr;
				} while(incr==escVal);

				if(end>maxSFB)
					throw new AACException("too many bands: "+end+", allowed: "+maxSFB);

				for(; k<end; k++, idx++) {
					sfbCB[idx] = cb;
					sectEnd[idx] = end;
				}
			}
		}
	}

	private void decodePulseData(BitStream in) {
		pulseCount = in.readBits(2)+1;
		pulseStartSWB = in.readBits(6);
		if(pulseStartSWB>=info.getSWBCount())
			throw new AACException("pulse SWB out of range: "+pulseStartSWB+" > "+info.getSWBCount());

		if(pulseOffset==null||pulseCount!=pulseOffset.length) {
			//only reallocate if needed
			pulseOffset = new int[pulseCount];
			pulseAmp = new int[pulseCount];
		}

		pulseOffset[0] = info.getSWBOffsets()[pulseStartSWB];
		pulseOffset[0] += in.readBits(5);
		pulseAmp[0] = in.readBits(4);
		for(int i = 1; i<pulseCount; i++) {
			pulseOffset[i] = in.readBits(5)+pulseOffset[i-1];
			if(pulseOffset[i]>1023)
				throw new AACException("pulse offset out of range: "+pulseOffset[0]);

			pulseAmp[i] = in.readBits(4);
		}
	}

	public void decodeScaleFactors(BitStream in) {
		final int windowGroups = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();
		//0: spectrum, 1: noise, 2: intensity
		final int[] offset = {globalGain, globalGain-90, 0};

		boolean noiseFlag = true;

		for(int g = 0, idx = 0; g<windowGroups; g++) {
			for(int sfb = 0; sfb<maxSFB;) {
				int end = sectEnd[idx];
				switch(sfbCB[idx]) {
					case ZERO_HCB:
						for(; sfb<end; sfb++, idx++) {
							scaleFactors[idx] = 0;
						}
						break;
					case INTENSITY_HCB:
					case INTENSITY_HCB2:
						for(; sfb<end; sfb++, idx++) {
							offset[2] += Huffman.decodeScaleFactor(in)-SF_DELTA;
							int tmp = Math.min(Math.max(offset[2], -155), 100);
							scaleFactors[idx] = SCALEFACTOR_TABLE[-tmp+SF_OFFSET];
						}
						break;
					case NOISE_HCB:
						for(; sfb<end; sfb++, idx++) {
							if(noiseFlag) {
								offset[1] += in.readBits(9)-256;
								noiseFlag = false;
							}
							else
								offset[1] += Huffman.decodeScaleFactor(in)-SF_DELTA;
							int tmp = Math.min(Math.max(offset[1], -100), 155);
							scaleFactors[idx] = -SCALEFACTOR_TABLE[tmp+SF_OFFSET];
						}
						break;
					default:
						for(; sfb<end; sfb++, idx++) {
							offset[0] += Huffman.decodeScaleFactor(in)-SF_DELTA;
							if(offset[0]>255)
								throw new AACException("scalefactor out of range: "+offset[0]);
							scaleFactors[idx] = SCALEFACTOR_TABLE[offset[0]-100+SF_OFFSET];
						}
						break;
				}
			}
		}
	}

	private void decodeSpectralData(BitStream in) {
		Arrays.fill(iqData, 0);
		final int maxSFB = info.getMaxSFB();
		final int windowGroups = info.getWindowGroupCount();
		final int[] offsets = info.getSWBOffsets();
		final int[] buf = new int[4];

		for(int g = 0, idx = 0, groupOff = 0; g<windowGroups; g++) {
			int groupLen = info.getWindowGroupLength(g);

			for(int sfb = 0; sfb<maxSFB; sfb++, idx++) {
				int hcb = sfbCB[idx];
				int off = groupOff+offsets[sfb];
				int width = offsets[sfb+1]-offsets[sfb];
				if(hcb==ZERO_HCB||hcb==INTENSITY_HCB||hcb==INTENSITY_HCB2) {
					for(int w = 0; w<groupLen; w++, off += 128) {
						Arrays.fill(iqData, off, off+width, 0);
					}
				}
				else if(hcb==NOISE_HCB) {
					//apply PNS: fill with random values
					for(int w = 0; w<groupLen; w++, off += 128) {
						float energy = 0;

						for(int k = 0; k<width; k++) {
							randomState = 1664525*randomState+1013904223;
							iqData[off+k] = randomState;
							energy += iqData[off+k]* iqData[off+k];
						}

						final float scale = (float) (scaleFactors[idx]/Math.sqrt(energy));
						for(int k = 0; k<width; k++) {
							iqData[off+k] *= scale;
						}
					}
				}
				else {
					for(int w = 0; w<groupLen; w++, off += 128) {
						int num = (hcb>=FIRST_PAIR_HCB) ? 2 : 4;
						for(int k = 0; k<width; k += num) {
							Huffman.decodeSpectralData(in, hcb, buf, 0);

							//inverse quantization & scaling
							for(int j = 0; j<num; j++) {
								iqData[off+k+j] = (buf[j]>0) ? IQ_TABLE[buf[j]] : -IQ_TABLE[-buf[j]];
								iqData[off+k+j] *= scaleFactors[idx];
							}
						}
					}
				}
			}
			groupOff += groupLen<<7;
		}
	}

	/* =========== gets ============ */
	/**
	 * Does inverse quantization and applies the scale factors on the decoded
	 * data. After this the noiseless decoding is finished and the decoded data
	 * is returned.
	 * @return the inverse quantized and scaled data
	 */
	public float[] getInvQuantData() {
		return iqData;
	}

	public float[] getOverlap() {
		return overlap;
	}

	public ICSInfo getInfo() {
		return info;
	}

	public int[] getSectEnd() {
		return sectEnd;
	}

	public int[] getSfbCB() {
		return sfbCB;
	}

	public float[] getScaleFactors() {
		return scaleFactors;
	}

	public void process(float[] data, FilterBank filterBank) {
		filterBank.process(info.getWindowSequence(), info.getWindowShape(ICSInfo.CURRENT), info.getWindowShape(ICSInfo.PREVIOUS),
				iqData, data, overlap);
	}

	private void processTNS(float[] data, boolean decode) {
		if(tns!=null && tnsDataPresent)
			tns.process(this, data, info.sf, decode);
	}

	public void processICP() {
		info.processICP(this.iqData);
	}

	public void processTNS() {
		processTNS(this.iqData, false);
	}

	public void processTNS(float[] data) {
		processTNS(data, true);
	}

	public void processLTP(FilterBank filterBank) {
		if(info.ltPredict!=null)
			info.ltPredict.process(this, filterBank);
	}

	public void updateLTP(float[] data) {
		if(info.ltPredict!=null)
			info.ltPredict.updateState(data, getOverlap(), info.config.getProfile());
	}

	public int getGlobalGain() {
		return globalGain;
	}

	public boolean isNoiseUsed() {
		return noiseUsed;
	}

	public int getLongestCodewordLength() {
		return longestCodewordLen;
	}

	public int getReorderedSpectralDataLength() {
		return reorderedSpectralDataLen;
	}

	public void processGainControl() {
		if(gainControl!=null && gainControlPresent)
			gainControl.process(this.iqData,
					info.getWindowShape(ICSInfo.CURRENT),
					info.getWindowShape(ICSInfo.PREVIOUS),
					info.getWindowSequence());
	}
}
