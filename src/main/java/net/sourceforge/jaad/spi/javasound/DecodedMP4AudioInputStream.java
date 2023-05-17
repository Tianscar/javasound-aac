package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;

import java.io.IOException;

import static net.sourceforge.jaad.spi.javasound.Utils.getPCMFormat;

class DecodedMP4AudioInputStream extends AsynchronousAudioInputStream {

	final AudioTrack track;
	final Decoder decoder;
	final SampleBuffer sampleBuffer;

	DecodedMP4AudioInputStream(MP4AudioInputStream stream) throws IOException {
		super(stream.in, getPCMFormat(stream.getFormat()), stream.getFrameLength());
		this.track = stream.track;
		this.decoder = stream.decoder;
		this.sampleBuffer = stream.sampleBuffer;
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
