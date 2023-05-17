package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import java.io.IOException;

import static net.sourceforge.jaad.spi.javasound.Utils.getPCMFormat;

class DecodedAACAudioInputStream extends AsynchronousAudioInputStream {

	final ADTSDemultiplexer adts;
	final Decoder decoder;
	final SampleBuffer sampleBuffer;

	DecodedAACAudioInputStream(AACAudioInputStream stream) throws IOException {
		super(stream.in, getPCMFormat(stream.getFormat()), stream.getFrameLength());
		this.adts = stream.adts;
		this.decoder = stream.decoder;
		this.sampleBuffer = stream.sampleBuffer;
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
