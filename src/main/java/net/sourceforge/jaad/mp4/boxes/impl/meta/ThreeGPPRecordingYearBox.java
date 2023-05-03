package net.sourceforge.jaad.mp4.boxes.impl.meta;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

public class ThreeGPPRecordingYearBox extends FullBox {

	private int year;

	public ThreeGPPRecordingYearBox() {
		super("3GPP Recording Year Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		year = (int) in.readBytes(2);
	}

	public int getYear() {
		return year;
	}
}
