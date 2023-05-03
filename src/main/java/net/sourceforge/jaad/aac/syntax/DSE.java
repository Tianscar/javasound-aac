package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.DecoderConfig;

import java.util.List;

/**
 * data_stream_element Abbreviation DSE.
 *
 * Syntactic element that contains data.
 * Again, there are 16 element_instance_tags.
 * There is, however, no restriction on the number
 * of data_stream_element’s with any one instance tag,
 * as a single data stream may continue across multiple
 * data_stream_element’s with the same instance tag.
 */

class DSE implements Element {

	public static final Type TYPE = Type.DSE;

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
			return new DSE(config, this);
		}
	}

	public static final List<Tag> TAGS = Element.createTagList(32, Tag::new);

	private final Tag tag;

	@Override
	public Tag getElementInstanceTag() {
		return tag;
	}
	private byte[] dataStreamBytes;

	public DSE(DecoderConfig config, Tag tag) {
		super();
		this.tag = tag;
	}

	public void decode(BitStream in) {

		final boolean byteAlign = in.readBool();
		int count = in.readBits(8);
		if(count==255)
			count += in.readBits(8);

		if(byteAlign)
			in.byteAlign();

		dataStreamBytes = new byte[count];
		for(int i = 0; i<count; i++) {
			dataStreamBytes[i] = (byte) in.readBits(8);
		}
	}
}
