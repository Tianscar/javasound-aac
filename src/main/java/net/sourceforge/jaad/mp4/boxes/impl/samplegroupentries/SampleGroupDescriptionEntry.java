package net.sourceforge.jaad.mp4.boxes.impl.samplegroupentries;

import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.mp4.boxes.BoxImpl;

import java.io.IOException;

public abstract class SampleGroupDescriptionEntry extends BoxImpl {

	protected SampleGroupDescriptionEntry(String name) {
		super(name);
	}

	@Override
	public abstract void decode(MP4InputStream in) throws IOException;
}
