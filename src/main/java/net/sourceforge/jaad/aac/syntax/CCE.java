package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.huffman.HCB;
import net.sourceforge.jaad.aac.huffman.Huffman;

import java.util.List;

/**
 * coupling_channel_element: Abbreviation CCE.
 *
 * Syntactic element that contains audio data for a coupling channel.
 * A coupling channel represents the information for multi-channel intensity
 * for one block, or alternately for dialogue for multilingual programming.
 *
 * The rules for number of coupling_channel_element’s and instance tags
 * are as for single_channel_element().
 */

class CCE implements Element {

	public static final Type TYPE = Type.CCE;

	static class Tag extends InstanceTag {

		protected Tag(int id) {
			super(id);
		}

		@Override
		public int getId() {
			return super.getId()%16;
		}

		public boolean isIsIndSW() {
			return id>=16;
		}

		@Override
		public Type getType() {
			return TYPE;
		}

		@Override
		public Element newElement(DecoderConfig config) {
			return new CCE(config, this);
		}
	}

	public static final List<Tag> TAGS = Element.createTagList(32, CCE.Tag::new);


	private final DecoderConfig config;
	private final Tag tag;

	@Override
	public Tag getElementInstanceTag() {
		return tag;
	}

	public static final int BEFORE_TNS = 0;
	public static final int AFTER_TNS = 1;
	public static final int AFTER_IMDCT = 2;
	private static final float[] CCE_SCALE = {
		1.09050773266525765921f,
		1.18920711500272106672f,
		1.4142135623730950488016887f,
		2f};
	private final ICStream ics;
	private int couplingPoint;
	private int coupledCount;
	private final boolean[] channelPair;
	private final int[] idSelect;
	private final int[] chSelect;
	/*[0] shared list of gains; [1] list of gains for right channel;
	 *[2] list of gains for left channel; [3] lists of gains for both channels
	 */
	private final float[][] gain;

	CCE(DecoderConfig config, Tag tag) {
		super();
		this.config = config;
		this.tag = tag;

		ics = new ICStream(config);
		channelPair = new boolean[8];
		idSelect = new int[8];
		chSelect = new int[8];
		gain = new float[16][120];
	}

	int getCouplingPoint() {
		return couplingPoint;
	}

	int getCoupledCount() {
		return coupledCount;
	}

	boolean isChannelPair(int index) {
		return channelPair[index];
	}

	int getIDSelect(int index) {
		return idSelect[index];
	}

	int getCHSelect(int index) {
		return chSelect[index];
	}

	public void decode(BitStream in) {
		couplingPoint = 2*in.readBit();
		coupledCount = in.readBits(3);
		int gainCount = 0;

		for(int i = 0; i<=coupledCount; i++) {
			gainCount++;
			channelPair[i] = in.readBool();
			idSelect[i] = in.readBits(4);
			if(channelPair[i]) {
				chSelect[i] = in.readBits(2);
				if(chSelect[i]==3)
					gainCount++;
			} else
				chSelect[i] = 2;
		}
		couplingPoint += in.readBit();
		couplingPoint |= (couplingPoint>>1);

		final boolean sign = in.readBool();
		final double scale = CCE_SCALE[in.readBits(2)];

		ics.decode(in, false, config);
		final ICSInfo info = ics.getInfo();
		final int windowGroupCount = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();

		final int[] sfbCB = ics.getSfbCB();

		for(int i = 0; i<gainCount; i++) {
			int idx = 0;
			int cge = 1;
			int xg = 0;
			float gainCache = 1.0f;
			if(i>0) {
				cge = couplingPoint==2 ? 1 : in.readBit();
				xg = cge==0 ? 0 : Huffman.decodeScaleFactor(in)-60;
				gainCache = (float) Math.pow(scale, -xg);
			}
			if(couplingPoint==2)
				gain[i][0] = gainCache;
			else {
				for(int g = 0; g<windowGroupCount; g++) {
					for(int sfb = 0; sfb<maxSFB; sfb++, idx++) {
						if(sfbCB[idx]!=HCB.ZERO_HCB) {
							if(cge==0) {
								int t = Huffman.decodeScaleFactor(in)-60;
								if(t!=0) {
									int s = 1;
									t = xg += t;
									if(!sign) {
										s -= 2*(t&0x1);
										t >>= 1;
									}
									gainCache = (float) (Math.pow(scale, -t)*s);
								}
							}
							gain[i][idx] = gainCache;
						}
					}
				}
			}
		}
	}

	void process() {
	}

	void applyIndependentCoupling(int index, float[] data) {
		final double g = gain[index][0];
		float[] iqData = ics.getInvQuantData();
		for(int i = 0; i<data.length; i++) {
			data[i] += g*iqData[i];
		}
	}

	void applyDependentCoupling(int index, float[] data) {
		final ICSInfo info = ics.getInfo();
		final int[] swbOffsets = info.getSWBOffsets();
		final int windowGroupCount = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();
		final int[] sfbCB = ics.getSfbCB();
		float[] iqData = ics.getInvQuantData();

		int srcOff = 0;
		int dstOff = 0;

		int idx = 0;
		for(int g = 0; g<windowGroupCount; g++) {
			int len = info.getWindowGroupLength(g);
			for(int sfb = 0; sfb<maxSFB; sfb++, idx++) {
				if(sfbCB[idx]!=HCB.ZERO_HCB) {
					float x = gain[index][idx];
					for(int group = 0; group<len; group++) {
						for(int k = swbOffsets[sfb]; k<swbOffsets[sfb+1]; k++) {
							data[dstOff+group*128+k] += x*iqData[srcOff+group*128+k];
						}
					}
				}
			}
			dstOff += len*128;
			srcOff += len*128;
		}
	}
}
