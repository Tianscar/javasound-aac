package com.tianscar.media.aac;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import javax.sound.sampled.AudioFormat;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class AACDurationExample {

    public static void main(String[] args) {
        try {
            ADTSDemultiplexer adts = new ADTSDemultiplexer(Files.newInputStream(
                    new File("src/test/resources/fbodemo1.aac").toPath(), StandardOpenOption.READ));
            final Decoder decoder = Decoder.create(adts.getDecoderInfo());
            final SampleBuffer sampleBuffer = new SampleBuffer(decoder.getAudioFormat());
            double lengthInSeconds = 0;
            try {
                while (true) {
                    decoder.decodeFrame(adts.readNextFrame(), sampleBuffer);
                    lengthInSeconds += sampleBuffer.getLength();
                }
            }
            catch (EOFException ignored) {
            }
            System.out.println("AAC audio duration: " + lengthInSeconds);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
