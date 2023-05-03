package net.sourceforge.jaad.mp4.boxes.impl.sampleentries.codec;

import net.sourceforge.jaad.mp4.MP4Input;

import java.io.IOException;

public class AMRSpecificBox extends CodecSpecificBox {

	private int modeSet, modeChangePeriod, framesPerSample;

	public AMRSpecificBox() {
		super("AMR Specific Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		decodeCommon(in);

		modeSet = (int) in.readBytes(2);
		modeChangePeriod = in.readByte();
		framesPerSample = in.readByte();
	}

	public int getModeSet() {
		return modeSet;
	}

	public int getModeChangePeriod() {
		return modeChangePeriod;
	}

	public int getFramesPerSample() {
		return framesPerSample;
	}
}
