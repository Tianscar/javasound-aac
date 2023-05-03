package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.filterbank.FilterBank;
import net.sourceforge.jaad.aac.sbr.SBR;
import net.sourceforge.jaad.aac.sbr.SBR2;
import net.sourceforge.jaad.aac.tools.IS;
import net.sourceforge.jaad.aac.tools.MS;
import net.sourceforge.jaad.aac.tools.MSMask;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * channel_pair_element: abbreviation CPE.
 *
 * Syntactic element of the bitstream payload containing data for a pair of channels.
 * A channel_pair_element consists of two individual_channel_streams and additional
 * joint channel coding information. The two channels may share common side information.
 *
 * The channel_pair_element has the same restrictions as the single channel element
 * as far as element_instance_tag, and number of occurrences.
 */

public class CPE extends ChannelElement {

    static final Logger LOGGER = Logger.getLogger("jaad.aac.syntax.CPE"); //for debugging

	public static final Type TYPE = Type.CPE;

	static class Tag extends ChannelTag {

		protected Tag(int id) {
			super(id);
		}

		@Override
		public boolean isChannelPair() {
			return true;
		}

		@Override
		public Type getType() {
			return TYPE;
		}

		@Override
		public ChannelElement newElement(DecoderConfig config) {
			return new CPE(config, this);
		}
	}

	public static final List<Tag> TAGS = Element.createTagList(16, Tag::new);

	public static final int MAX_MS_MASK = 128;

	private MSMask msMask;
	private boolean[] msUsed;
	private boolean commonWindow;
	private final ICStream icsL, icsR;

	public CPE(DecoderConfig config, ChannelTag tag) {
		super(config, tag);
		msUsed = new boolean[MAX_MS_MASK];
		icsL = new ICStream(config);
		icsR = new ICStream(config);
	}

	public boolean isChannelPair() {
 		return true;
	}

	public boolean isStereo() {
 		return true;
	}

	protected SBR openSBR() {
		return new SBR2(config);
	}

	public void decode(BitStream in) {
		super.decode(in);

		commonWindow = in.readBool();
		final ICSInfo infoL = icsL.getInfo();
		final ICSInfo infoR = icsR.getInfo();

		LOGGER.log(Level.FINE, ()->String.format("CPE %s", commonWindow? "common":""));

		if(commonWindow) {
			infoL.decode(in, commonWindow);
			infoR.setCommonData(in, infoL);

			msMask = MSMask.forInt(in.readBits(2));
			if(msMask.equals(MSMask.TYPE_USED)) {
				final int maxSFB = infoL.getMaxSFB();
				final int windowGroupCount = infoL.getWindowGroupCount();

				for(int idx = 0; idx<windowGroupCount*maxSFB; idx++) {
					msUsed[idx] = in.readBool();
				}
			}
			else if(msMask.equals(MSMask.TYPE_ALL_1))
				Arrays.fill(msUsed, true);

			else if(msMask.equals(MSMask.TYPE_ALL_0))
				Arrays.fill(msUsed, false);

			else
				throw new AACException("reserved MS mask type used");
		}
		else {
			msMask = MSMask.TYPE_ALL_0;
			Arrays.fill(msUsed, false);
		}

		icsL.decode(in, commonWindow, config);
		icsR.decode(in, commonWindow, config);
	}

	public ICStream getLeftChannel() {
		return icsL;
	}

	public ICStream getRightChannel() {
		return icsR;
	}

	public MSMask getMSMask() {
		return msMask;
	}

	public boolean isMSUsed(int off) {
		return msUsed[off];
	}

	public boolean isMSMaskPresent() {
		return !msMask.equals(MSMask.TYPE_ALL_0);
	}

	public boolean isCommonWindow() {
		return commonWindow;
	}

	public void process(FilterBank filterBank, List<CCE> cces, Consumer<float[]> target) {

		final float[] dataL = getDataL();
		final float[] dataR = getDataR();

		//inverse quantization
		final float[] iqDataL = icsL.getInvQuantData();
		final float[] iqDataR = icsR.getInvQuantData();

		//MS
		if(isCommonWindow()&isMSMaskPresent())
			MS.process(this, iqDataL, iqDataR);

		//prediction
		icsL.processICP();
		icsR.processICP();

		//IS
		IS.process(this, iqDataL, iqDataR);

		icsL.processLTP(filterBank);
		icsR.processLTP(filterBank);

		//dependent coupling
		processDependentCoupling(cces, CCE.BEFORE_TNS, iqDataL, iqDataR);

		icsL.processTNS();
		icsR.processTNS();

		//dependent coupling
		processDependentCoupling(cces, CCE.AFTER_TNS, iqDataL, iqDataR);

		//filterbank
		icsL.process(dataL, filterBank);
		icsR.process(dataR, filterBank);

		icsL.updateLTP(dataL);
		icsR.updateLTP(dataR);

		//independent coupling
		processIndependentCoupling(cces, dataL, dataR);

		//gain control
		icsL.processGainControl();
		icsR.processGainControl();

		//SBR
		if(isSBRPresent()&&config.isSBREnabled()) {
			if(dataL.length==config.getFrameLength())
				LOGGER.log(Level.WARNING, "SBR data present, but buffer has normal size!");

			getSBR().process(dataL, dataR);
		} else if(dataL.length!=config.getFrameLength()) {
			SBR.upsample(dataL);
			SBR.upsample(dataR);
		}

		target.accept(dataL);
		target.accept(dataR);
	}
}
