package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;
import net.sourceforge.jaad.mp4.od.Descriptor;

import java.io.IOException;

public class ObjectDescriptorBox extends FullBox {

	private Descriptor objectDescriptor;

	public ObjectDescriptorBox() {
		super("Object Descriptor Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);
		objectDescriptor = Descriptor.createDescriptor(in);
	}

	public Descriptor getObjectDescriptor() {
		return objectDescriptor;
	}
}
