package net.sourceforge.jaad.mp4.boxes.impl;

import net.sourceforge.jaad.mp4.MP4Input;
import net.sourceforge.jaad.mp4.boxes.FullBox;

import java.io.IOException;

public class AppleLosslessBox extends FullBox {

	private long maxSamplePerFrame, maxCodedFrameSize, bitRate, sampleRate;
	private int sampleSize, historyMult, initialHistory, kModifier, channels;

	public AppleLosslessBox() {
		super("Apple Lossless Box");
	}

	@Override
	public void decode(MP4Input in) throws IOException {
		super.decode(in);

		maxSamplePerFrame = in.readBytes(4);
		in.skipBytes(1); //?
		sampleSize = in.readByte();
		historyMult = in.readByte();
		initialHistory = in.readByte();
		kModifier = in.readByte();
		channels = in.readByte();
		in.skipBytes(2); //?
		maxCodedFrameSize = in.readBytes(4);
		bitRate = in.readBytes(4);
		sampleRate = in.readBytes(4);
	}

	public long getMaxSamplePerFrame() {
		return maxSamplePerFrame;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public int getHistoryMult() {
		return historyMult;
	}

	public int getInitialHistory() {
		return initialHistory;
	}

	public int getkModifier() {
		return kModifier;
	}

	public int getChannels() {
		return channels;
	}

	public long getMaxCodedFrameSize() {
		return maxCodedFrameSize;
	}

	public long getBitRate() {
		return bitRate;
	}

	public long getSampleRate() {
		return sampleRate;
	}
}
