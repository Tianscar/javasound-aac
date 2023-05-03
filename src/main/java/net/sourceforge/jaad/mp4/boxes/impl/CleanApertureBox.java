package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.BoxImpl;

import java.io.IOException;

public class CleanApertureBox extends BoxImpl {

	private long cleanApertureWidthN;
	private long cleanApertureWidthD;
	private long cleanApertureHeightN;
	private long cleanApertureHeightD;
	private long horizOffN;
	private long horizOffD;
	private long vertOffN;
	private long vertOffD;

	public CleanApertureBox() {
		super("Clean Aperture Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		cleanApertureWidthN = in.readBytes(4);
		cleanApertureWidthD = in.readBytes(4);
		cleanApertureHeightN = in.readBytes(4);
		cleanApertureHeightD = in.readBytes(4);
		horizOffN = in.readBytes(4);
		horizOffD = in.readBytes(4);
		vertOffN = in.readBytes(4);
		vertOffD = in.readBytes(4);
	}

	public long getCleanApertureWidthN() {
		return cleanApertureWidthN;
	}

	public long getCleanApertureWidthD() {
		return cleanApertureWidthD;
	}

	public long getCleanApertureHeightN() {
		return cleanApertureHeightN;
	}

	public long getCleanApertureHeightD() {
		return cleanApertureHeightD;
	}

	public long getHorizOffN() {
		return horizOffN;
	}

	public long getHorizOffD() {
		return horizOffD;
	}

	public long getVertOffN() {
		return vertOffN;
	}

	public long getVertOffD() {
		return vertOffD;
	}
}
