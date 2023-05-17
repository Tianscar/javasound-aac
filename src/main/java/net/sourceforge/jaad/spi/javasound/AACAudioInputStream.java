package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import javax.sound.sampled.AudioInputStream;
import java.io.InputStream;

class AACAudioInputStream extends AudioInputStream {

    final ADTSDemultiplexer adts;
    final Decoder decoder;
    final SampleBuffer sampleBuffer;
    final InputStream in;

    AACAudioInputStream(ADTSDemultiplexer adts, Decoder decoder, SampleBuffer sampleBuffer, InputStream in, long length) {
        super(in, new AACAudioFormat(decoder.getConfig(), sampleBuffer), length);
        this.adts = adts;
        this.decoder = decoder;
        this.sampleBuffer = sampleBuffer;
        this.in = in;
    }

}
