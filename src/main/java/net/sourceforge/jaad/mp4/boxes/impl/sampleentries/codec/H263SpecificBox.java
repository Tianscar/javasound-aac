package net.sourceforge.jaad.mp4.boxes.impl.sampleentries.codec;

import net.sourceforge.jaad.mp4.MP4Input;

import java.io.IOException;

public class H263SpecificBox extends CodecSpecificBox {

	private int level, profile;

	public H263SpecificBox() {
		super("H.263 Specific Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		decodeCommon(in);

		level = in.readByte();
		profile = in.readByte();
	}

	public int getLevel() {
		return level;
	}

	public int getProfile() {
		return profile;
	}
}
