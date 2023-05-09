package net.sourceforge.jaad.test;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class AACDurationExample {

    public static void main(String[] args) {
        try {
            ADTSDemultiplexer adts = new ADTSDemultiplexer(Files.newInputStream(
                    new File("src/test/resources/fbodemo1.aac").toPath(), StandardOpenOption.READ));
            final Decoder decoder = Decoder.create(adts.getDecoderInfo());
            int frames = 0;
            long time = System.currentTimeMillis();
            try {
                while (true) {
                    adts.skipNextFrame();
                    frames ++;
                }
            }
            catch (EOFException ignored) {
            }
            time = System.currentTimeMillis() - time;
            System.out.println(time);
            final double lengthInSeconds = frames * (double) decoder.getConfig().getSampleLength() / (double) decoder.getConfig().getSampleFrequency().getFrequency();
            System.out.println("AAC audio duration: " + (long) (lengthInSeconds * 1_000_000L));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
