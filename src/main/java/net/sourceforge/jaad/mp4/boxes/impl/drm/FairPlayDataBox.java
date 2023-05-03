package net.sourceforge.jaad.mp4.boxes.impl.drm;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.BoxImpl;

import java.io.IOException;

public class FairPlayDataBox extends BoxImpl {

	private byte[] data;

	public FairPlayDataBox() {
		super("iTunes FairPlay Data Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		data = new byte[(int) getLeft(in)];
		in.readBytes(data);
	}

	public byte[] getData() {
		return data;
	}
}
