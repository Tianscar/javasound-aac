package net.sourceforge.jaad.mp4.boxes.impl.meta;

import net.sourceforge.jaad.mp4.MP4Input;

import java.io.IOException;

public class ThreeGPPKeywordsBox extends ThreeGPPMetadataBox {

	private String[] keywords;

	public ThreeGPPKeywordsBox() {
		super("3GPP Keywords Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		decodeCommon(in);

		final int count = in.readByte();
		keywords = new String[count];

		int len;
		for(int i = 0; i<count; i++) {
			len = in.readByte();
			keywords[i] = in.readUTFString(len);
		}
	}

	public String[] getKeywords() {
		return keywords;
	}
}
