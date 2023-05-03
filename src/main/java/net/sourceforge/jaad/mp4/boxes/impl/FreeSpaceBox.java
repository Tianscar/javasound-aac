package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.BoxImpl;

import java.io.IOException;

/**
 * This class is used for all boxes, that are known but don't contain necessary 
 * data and can be skipped. This is mainly used for 'skip', 'free' and 'wide'.
 * 
 * @author in-somnia
 */
public class FreeSpaceBox extends BoxImpl {

	public FreeSpaceBox() {
		super("Free Space Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		//no need to read, box will be skipped
	}
}
