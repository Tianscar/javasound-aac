package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;

class MP4AudioInputStream extends AsynchronousAudioInputStream {

	private final AudioTrack track;
	private final Decoder decoder;
	private final SampleBuffer sampleBuffer;

	static final String ERROR_MESSAGE_AAC_TRACK_NOT_FOUND = "movie does not contain any AAC track";

	MP4AudioInputStream(AudioTrack track, Decoder decoder, SampleBuffer sampleBuffer, InputStream in, AudioFormat format, long length) throws IOException {
		super(in, format, length);
		this.track = track;
		this.decoder = decoder;
		this.sampleBuffer = sampleBuffer;
	}

	public void execute() {
		decodeFrame();
		if (buffer.isOpen()) buffer.write(sampleBuffer.getData());
	}

	private void decodeFrame() {
		if (!track.hasMoreFrames()) {
			buffer.close();
			return;
		}
		try {
			final Frame frame = track.readNextFrame();
			if (frame == null) {
				buffer.close();
				return;
			}
			decoder.decodeFrame(frame.getData(), sampleBuffer);
		}
		catch (IOException e) {
			buffer.close();
		}
	}

}
