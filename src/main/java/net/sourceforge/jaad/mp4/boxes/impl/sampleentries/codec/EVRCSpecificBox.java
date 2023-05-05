package net.sourceforge.jaad.mp4.boxes.impl.sampleentries.codec;

import net.sourceforge.jaad.mp4.MP4InputStream;

import java.io.IOException;

public class EVRCSpecificBox extends CodecSpecificBox {

	private int framesPerSample;

	public EVRCSpecificBox() {
		super("EVCR Specific Box");
	}

	@Override
	public void decode(MP4InputStream in) throws IOException {
		decodeCommon(in);

		framesPerSample = in.readByte();
	}

	public int getFramesPerSample() {
		return framesPerSample;
	}
}
