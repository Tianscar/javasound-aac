package net.sourceforge.jaad.mp4.boxes.impl.meta;

import net.sourceforge.jaad.mp4.MP4Input;

import java.io.IOException;

public class ThreeGPPAlbumBox extends ThreeGPPMetadataBox {

	private int trackNumber;

	public ThreeGPPAlbumBox() {
		super("3GPP Album Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		trackNumber = (getLeft(in)>0) ? in.readByte() : -1;
	}

	/**
	 * The track number (order number) of the media on this album. This is an 
	 * optional field. If the field is not present, -1 is returned.
	 * 
	 * @return the track number
	 */
	public int getTrackNumber() {
		return trackNumber;
	}
}
