package net.sourceforge.jaad.mp4.boxes.impl.samplegroupentries;

import net.sourceforge.jaad.mp4.MP4Input;

import java.io.IOException;

public class VisualSampleGroupEntry extends SampleGroupDescriptionEntry {

	public VisualSampleGroupEntry() {
		super("Video Sample Group Entry");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
	}

}
