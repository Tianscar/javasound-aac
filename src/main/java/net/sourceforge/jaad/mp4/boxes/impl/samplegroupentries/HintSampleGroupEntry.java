package net.sourceforge.jaad.mp4.boxes.impl.samplegroupentries;

import net.sourceforge.jaad.mp4.MP4Input;

import java.io.IOException;

public class HintSampleGroupEntry extends SampleGroupDescriptionEntry {

	public HintSampleGroupEntry() {
		super("Hint Sample Group Entry");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
	}
}
