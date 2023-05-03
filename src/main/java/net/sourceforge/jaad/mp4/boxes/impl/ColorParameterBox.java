package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

//TODO: check decoding, add get-methods
public class ColorParameterBox extends FullBox {

	private long colorParameterType;
	private int primariesIndex, transferFunctionIndex, matrixIndex;

	public ColorParameterBox() {
		super("Color Parameter Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		colorParameterType = in.readBytes(4);
		primariesIndex = (int) in.readBytes(2);
		transferFunctionIndex = (int) in.readBytes(2);
		matrixIndex = (int) in.readBytes(2);
	}
}
