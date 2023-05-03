package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

/**
 * The item protection box provides an array of item protection information, for
 * use by the Item Information Box.
 *
 * @author in-somnia
 */
public class ItemProtectionBox extends FullBox {

	public ItemProtectionBox() {
		super("Item Protection Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		final int protectionCount = (int) in.readBytes(2);

		readChildren(in, protectionCount);
	}
}
