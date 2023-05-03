package net.sourceforge.jaad.mp4.boxes;

import net.sourceforge.jaad.mp4.MP4Input;

import java.io.IOException;

/**
 * Box implementation that is used for unknown types.
 * 
 * @author in-somnia
 */
class UnknownBox extends BoxImpl {

	UnknownBox() {
		super("unknown");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		//no need to read, box will be skipped
	}
}
