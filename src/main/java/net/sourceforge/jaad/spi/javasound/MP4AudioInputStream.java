package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.mp4.api.AudioTrack;

import javax.sound.sampled.AudioInputStream;
import java.io.InputStream;

class MP4AudioInputStream extends AudioInputStream {

    final AudioTrack track;
    final Decoder decoder;
    final SampleBuffer sampleBuffer;
    final InputStream in;

	MP4AudioInputStream(AudioTrack track, Decoder decoder, SampleBuffer sampleBuffer, InputStream in, long length) {
		super(in, new AACAudioFormat(decoder.getConfig(), sampleBuffer), length);
		this.track = track;
		this.decoder = decoder;
		this.sampleBuffer = sampleBuffer;
        this.in = in;
	}

}
