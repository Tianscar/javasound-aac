package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.Box;
import net.sourceforge.jaad.mp4.boxes.BoxFactory;
import net.sourceforge.jaad.mp4.boxes.BoxTypes;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

//needs to be defined, because readChildren() is not called by factory
/* TODO: this class shouldn't be needed. at least here, things become too
complicated. change this!!! */
public class MetaBox extends FullBox {

	public MetaBox() {
		super("Meta Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);
		readChildren(in);
	}

	@Override
	protected Box parseBox(MP4Input in) throws IOException {

		long offset = in.getOffset();
		long size = in.readBytes(4);
		long type = in.readBytes(4);

		// some encoders (such as Android's MexiaMuxer) do not include
		// the version and flags fields in the meta box, instead going
		// directly to the hdlr box.
		// Indication of it that size already contains the type.
		// Shift back all parameters by 4 bytes: type <- size <- version:flags <- 0

		if(children.isEmpty() && size==BoxTypes.HANDLER_BOX) {
			offset -= 4;
			type = size;
			size = (version&(2L*Integer.MAX_VALUE+1))<<24;
			size += flags;
			version=flags=0;
		}

		return BoxFactory.parseBox(this, offset, size, type, in);
	}
}
