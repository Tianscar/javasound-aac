package net.sourceforge.jaad.mp4.boxes.impl.meta;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

public class ITunesMetadataMeanBox extends FullBox {

	private String domain;

	public ITunesMetadataMeanBox() {
		super("iTunes Metadata Mean Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		domain = in.readString((int) getLeft(in));
	}

	public String getDomain() {
		return domain;
	}
}
