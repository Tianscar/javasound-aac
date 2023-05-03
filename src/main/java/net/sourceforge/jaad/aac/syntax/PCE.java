package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * program_config_element: Abbreviation PCE.
 *
 * Syntactic element that contains program configuration data.
 * The rules for the number of program_config_element’s and
 * element instance tags are the same as for single_channel_element’s.
 * PCEs must come before all other syntactic elements in a raw_data_block.
 */

public class PCE implements Element, AudioDecoderInfo {

	static final Logger LOGGER = Logger.getLogger("jaad.aac.syntax.PCE"); //for debugging

	public static final Type TYPE = Type.PCE;

	static class Tag extends InstanceTag {

		protected Tag(int id) {
			super(id);
		}

		@Override
		public Type getType() {
			return TYPE;
		}

		@Override
		public Element newElement(DecoderConfig config) {
			return new PCE(this);
		}
	}

	public static final List<Tag> TAGS = Element.createTagList(32, Tag::new);

	/**
	 * Read a full PCE element with tag and content.
	 * @param in input stream to decode.
	 * @return a new program config element.
	 */
	public static PCE read(BitStream in) {
		Tag tag = TAGS.get(in.readBits(4));
		PCE pce = new PCE(tag);
		pce.decode(in);
		return pce;
	}

	private final Tag tag;

	@Override
	public Tag getElementInstanceTag() {
		return tag;
	}

	private static final int MAX_FRONT_CHANNEL_ELEMENTS = 16;
	private static final int MAX_SIDE_CHANNEL_ELEMENTS = 16;
	private static final int MAX_BACK_CHANNEL_ELEMENTS = 16;
	private static final int MAX_LFE_CHANNEL_ELEMENTS = 4;
	private static final int MAX_ASSOC_DATA_ELEMENTS = 8;
	private static final int MAX_VALID_CC_ELEMENTS = 16;

	public static class TaggedElement {

		private final boolean isCPE;
		private final int tag;

		public TaggedElement(boolean isCPE, int tag) {
			this.isCPE = isCPE;
			this.tag = tag;
		}

		public boolean isIsCPE() {
			return isCPE;
		}

		public int getTag() {
			return tag;
		}
	}

	public static class CCE {

		private final boolean isIndSW;
		private final int tag;

		public CCE(boolean isIndSW, int tag) {
			this.isIndSW = isIndSW;
			this.tag = tag;
		}

		public boolean isIsIndSW() {
			return isIndSW;
		}

		public int getTag() {
			return tag;
		}
	}

	private Profile profile;
	private SampleFrequency sampleFrequency;
	private int frontChannelElementsCount, sideChannelElementsCount, backChannelElementsCount;
	private int lfeChannelElementsCount, assocDataElementsCount;
	private int validCCElementsCount;
	boolean monoMixdown, stereoMixdown, matrixMixdownIDXPresent;
	int monoMixdownElementNumber, stereoMixdownElementNumber, matrixMixdownIDX;
	boolean pseudoSurround;
	private final TaggedElement[] frontElements, sideElements, backElements;
	private final int[] lfeElementTags;
	private final int[] assocDataElementTags;
	private final CCE[] ccElements;
	private byte[] commentFieldData;

	protected PCE(Tag tag) {
		super();
		this.tag = tag;

		frontElements = new TaggedElement[MAX_FRONT_CHANNEL_ELEMENTS];
		sideElements = new TaggedElement[MAX_SIDE_CHANNEL_ELEMENTS];
		backElements = new TaggedElement[MAX_BACK_CHANNEL_ELEMENTS];
		lfeElementTags = new int[MAX_LFE_CHANNEL_ELEMENTS];
		assocDataElementTags = new int[MAX_ASSOC_DATA_ELEMENTS];
		ccElements = new CCE[MAX_VALID_CC_ELEMENTS];
		sampleFrequency = SampleFrequency.SF_NONE;
	}

	public void decode(BitStream in) {

		profile = Profile.forInt(1+in.readBits(2));

		sampleFrequency = SampleFrequency.decode(in);

		frontChannelElementsCount = in.readBits(4);
		sideChannelElementsCount = in.readBits(4);
		backChannelElementsCount = in.readBits(4);
		lfeChannelElementsCount = in.readBits(2);
		assocDataElementsCount = in.readBits(3);
		validCCElementsCount = in.readBits(4);

		if(monoMixdown = in.readBool()) {
			LOGGER.warning("mono mixdown present, but not yet supported");
			monoMixdownElementNumber = in.readBits(4);
		}
		if(stereoMixdown = in.readBool()) {
			LOGGER.warning("stereo mixdown present, but not yet supported");
			stereoMixdownElementNumber = in.readBits(4);
		}
		if(matrixMixdownIDXPresent = in.readBool()) {
			LOGGER.warning("matrix mixdown present, but not yet supported");
			matrixMixdownIDX = in.readBits(2);
			pseudoSurround = in.readBool();
		}

		readTaggedElementArray(frontElements, in, frontChannelElementsCount);

		readTaggedElementArray(sideElements, in, sideChannelElementsCount);

		readTaggedElementArray(backElements, in, backChannelElementsCount);

		for(int i = 0; i<lfeChannelElementsCount; ++i) {
			lfeElementTags[i] = in.readBits(4);
		}

		for(int i = 0; i<assocDataElementsCount; ++i) {
			assocDataElementTags[i] = in.readBits(4);
		}

		for(int i = 0; i<validCCElementsCount; ++i) {
			ccElements[i] = new CCE(in.readBool(), in.readBits(4));
		}

		in.byteAlign();

		final int commentFieldBytes = in.readBits(8);
		commentFieldData = new byte[commentFieldBytes];
		for(int i = 0; i<commentFieldBytes; i++) {
			commentFieldData[i] = (byte) in.readBits(8);
		}
	}

	private void readTaggedElementArray(TaggedElement[] te, BitStream in, int len) {
		for(int i = 0; i<len; ++i) {
			te[i] = new TaggedElement(in.readBool(), in.readBits(4));
		}
	}

	public Profile getProfile() {
		return profile;
	}

	public SampleFrequency getSampleFrequency() {
		return sampleFrequency;
	}

	public int getChannelCount() {
		int count = lfeChannelElementsCount;//+assocDataElementsCount;

		for(int n=0; n<frontChannelElementsCount; ++n)
			count += frontElements[n].isCPE ? 2 : 1;

		for(int n=0; n<sideChannelElementsCount; ++n)
			count += sideElements[n].isCPE ? 2 : 1;

		for(int n=0; n<backChannelElementsCount; ++n)
			count += backElements[n].isCPE ? 2 : 1;

		return count;
	}

	/**
	 * Turn this PCE into a known ChannelConfiguration.
	 * Todo: replace ChannelConfiguration by a preconfigured PCE.
	 * @return a matching ChannelConfiguration according its channel count.
	 */
	public ChannelConfiguration getChannelConfiguration() {
		return ChannelConfiguration.forChannelCount(getChannelCount());
	}
}
