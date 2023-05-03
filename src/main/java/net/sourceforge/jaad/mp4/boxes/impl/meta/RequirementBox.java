package net.sourceforge.jaad.mp4.boxes.impl.meta;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

public class RequirementBox extends FullBox {

	private String requirement;

	public RequirementBox() {
		super("Requirement Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		requirement = in.readString((int) getLeft(in));
	}

	public String getRequirement() {
		return requirement;
	}
}
