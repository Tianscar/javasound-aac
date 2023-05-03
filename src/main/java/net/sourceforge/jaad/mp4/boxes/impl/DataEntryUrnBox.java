package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

public class DataEntryUrnBox extends FullBox {

	private boolean inFile;
	private String referenceName, location;

	public DataEntryUrnBox() {
		super("Data Entry Urn Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		inFile = (flags&1)==1;
		if(!inFile) {
			referenceName = in.readUTFString((int) getLeft(in), MP4Input.UTF8);
			if(getLeft(in)>0)
				location = in.readUTFString((int) getLeft(in), MP4Input.UTF8);
		}
	}

	public boolean isInFile() {
		return inFile;
	}

	public String getReferenceName() {
		return referenceName;
	}

	public String getLocation() {
		return location;
	}
}
