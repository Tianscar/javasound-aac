package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.filterbank.FilterBank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SyntacticElements {
	static final Logger LOGGER = Logger.getLogger("jaad.SyntacticElements"); //for debugging

	//global properties
	private DecoderConfig config;

	private final FilterBank filterBank;

	//elements

	private List<CCE> cces = new ArrayList<>();

	private final Map<Element.InstanceTag, Element> elements = new HashMap<>();

	private final List<ChannelElement> audioElements = new ArrayList<>(); //SCE, LFE and CPE

	private List<float[]> channels = new ArrayList<>();

	private Element newElement(Element.InstanceTag tag) {
		return tag.newElement(config);
	}

	private Element getElement(Element.InstanceTag tag) {
		return elements.computeIfAbsent(tag, this::newElement);
	}

	public SyntacticElements(DecoderConfig config) {
		this.config = config;
		filterBank = new FilterBank(config.isSmallFrameUsed());

		startNewFrame();
	}

	public final void startNewFrame() {
		audioElements.clear();
		cces.clear();
		channels.clear();
	}

	public void decode(BitStream in) {

		if(!config.getProfile().isErrorResilientProfile()) {

			loop: do {
				switch(Element.readType(in)) {
					case SCE:
						decode(SCE.TAGS, in);
						break;
					case CPE:
						decode(CPE.TAGS, in);
						break;
					case CCE:
						decode(CCE.TAGS, in);
						break;
					case LFE:
						decode(LFE.TAGS, in);
						break;
					case DSE:
						decode(DSE.TAGS, in);
						break;
					case PCE:
						decode(PCE.TAGS, in);
						break;
					case FIL:
						decodeFIL(in);
						break;
					case END:
						break loop;
				}
			} while(true);
		}
		else {
			//error resilient raw data block
			switch(config.getChannelConfiguration()) {
				case MONO:
					decode(SCE.TAGS, in);
					break;
				case STEREO:
					decode(CPE.TAGS, in);
					break;
				case STEREO_PLUS_CENTER:
					decode(SCE.TAGS, in);
					decode(CPE.TAGS, in);
					break;
				case STEREO_PLUS_CENTER_PLUS_REAR_MONO:
					decode(SCE.TAGS, in);
					decode(CPE.TAGS, in);
					decode(LFE.TAGS, in);
					break;
				case FIVE:
					decode(SCE.TAGS, in);
					decode(CPE.TAGS, in);
					decode(CPE.TAGS, in);
					break;
				case FIVE_PLUS_ONE:
					decode(SCE.TAGS, in);
					decode(CPE.TAGS, in);
					decode(CPE.TAGS, in);
					decode(LFE.TAGS, in);
					break;
				case SEVEN_PLUS_ONE:
					decode(SCE.TAGS, in);
					decode(CPE.TAGS, in);
					decode(CPE.TAGS, in);
					decode(CPE.TAGS, in);
					decode(LFE.TAGS, in);
					break;
				default:
					throw new AACException("unsupported channel configuration for error resilience: "+config.getChannelConfiguration());
			}
		}
		in.byteAlign();

		LOGGER.finest("END");
	}

	private Element decode(List<? extends Element.InstanceTag> tags, BitStream in) {

		int id = in.readBits(4);
		Element.InstanceTag tag = tags.get(id);

		LOGGER.finest(tag.toString());

		Element element = getElement(tag);

		element.decode(in);

		if(element instanceof ChannelElement) {
			audioElements.add((ChannelElement) element);
		}

		if(element instanceof CCE) {
			cces.add((CCE)element);
		}

		if(element instanceof PCE) {
			PCE pce = (PCE) element;
			config.setAudioDecoderInfo(pce);
		}

		return element;
	}

	private static final int EXT_FILL = 0;
	private static final int EXT_FILL_DATA = 1;
	private static final int EXT_DATA_ELEMENT = 2;
	private static final int EXT_DYNAMIC_RANGE = 11;
	private static final int EXT_SAC_DATA = 12;
	private static final int EXT_SBR_DATA = 13;
	private static final int EXT_SBR_DATA_CRC = 14;

	private void decodeFIL(BitStream in) {

		int count = in.readBits(4);
		if(count==15)
			count += in.readBits(8)-1;

		if(count==0)
			return;

		in = in.readSubStream(8*count);

		int type = in.readBits(4);

		switch(type) {
			case EXT_DYNAMIC_RANGE:
				decodeDynamicRangeInfo(in);
				break;

			case EXT_SBR_DATA:
			case EXT_SBR_DATA_CRC:
				decodeSBR(in, type);
				break;

			case EXT_FILL:
			case EXT_FILL_DATA:
				break;

			case EXT_SAC_DATA:
				decodeSAC(in);
				break;

			case EXT_DATA_ELEMENT:
				decodeExtData(in);
		}
	}

	private ChannelElement getLastAudioElement() {
		int n = audioElements.size();
		return n==0 ? null : audioElements.get(n-1);
	}

	private void decodeSBR(BitStream in, int type) {
		ChannelElement prev = getLastAudioElement();
		if(prev!=null)
			prev.decodeSBR(in, (type == EXT_SBR_DATA_CRC));
	}

	// decoded but unused.
	private DRC dri;

	private void decodeDynamicRangeInfo(BitStream in) {
		if (dri == null)
			dri = new DRC();

		dri.decode(in);
	}

	// Spatial Audio Coding (ISO/IEC 23003-1)
	private void decodeSAC(BitStream in) {

	}

	private void decodeExtData(BitStream in) {

	}

	public List<float[]> process() {

		channels.clear();

		for (ChannelElement e : audioElements) {
			e.process(filterBank, cces, channels::add);
		}

		// upgrade to stereo
		if(channels.size()==1 && config.getChannelCount()>1)
			channels.add(channels.get(0));

		return channels;
	}
}
