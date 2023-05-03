package net.sourceforge.jaad.mp4.boxes.impl.oma;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

public class OMAAccessUnitFormatBox extends FullBox {

	private boolean selectiveEncrypted;
	private int keyIndicatorLength, initialVectorLength;

	public OMAAccessUnitFormatBox() {
		super("OMA DRM Access Unit Format Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		//1 bit selective encryption, 7 bits reserved
		selectiveEncrypted = ((in.readByte()>>7)&1)==1;
		keyIndicatorLength = in.readByte(); //always zero?
		initialVectorLength = in.readByte();
	}

	public boolean isSelectiveEncrypted() {
		return selectiveEncrypted;
	}

	public int getKeyIndicatorLength() {
		return keyIndicatorLength;
	}

	public int getInitialVectorLength() {
		return initialVectorLength;
	}
}
