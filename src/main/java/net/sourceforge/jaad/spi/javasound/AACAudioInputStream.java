package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.SampleBuffer;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

class AACAudioInputStream extends AsynchronousAudioInputStream {

	private final ADTSDemultiplexer adts;
	private final Decoder decoder;
	private final SampleBuffer sampleBuffer;

	AACAudioInputStream(ADTSDemultiplexer adts, Decoder decoder, SampleBuffer sampleBuffer,
						InputStream in, AudioFormat format, long length) throws IOException {
		super(in, format, length);
		this.adts = adts;
		this.decoder = decoder;
		this.sampleBuffer = sampleBuffer;
	}

	public void execute() {
		try {
			decoder.decodeFrame(adts.readNextFrame(), sampleBuffer);
			buffer.write(sampleBuffer.getData());
		}
		catch (IOException e) {
			buffer.close();
		}
	}

}
