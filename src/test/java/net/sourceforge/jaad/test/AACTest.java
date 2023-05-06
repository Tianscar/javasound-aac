/*
 * Adapted from https://github.com/umjammer/JAADec/blob/0.8.9/src/test/java/net/sourceforge/jaad/spi/javasound/AacFormatConversionProviderTest.java
 *
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package net.sourceforge.jaad.test;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.sourceforge.jaad.spi.javasound.AACAudioFileReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AACTest {

    private static final String inFile1 = "/fbodemo1.aac";
    private static final String inFile  = "/fbodemo1.ogg";
    private static final String inFile2 = "/fbodemo1.m4a";
    private static final String inFile4 = "/fbodemo1_alac.m4a";
    private static final String inFile3 = "/fbodemo1.caf";

    @Test
    @DisplayName("unsupported exception is able to detect in 3 ways")
    public void test1() throws Exception {

        Path path = Paths.get("src/test/resources", inFile);

        assertThrows(UnsupportedAudioFileException.class, () -> {
            // don't replace with Files#newInputStream(Path)
            new AACAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path.toFile().toPath())));
        });

        assertThrows(UnsupportedAudioFileException.class, () -> {
            new AACAudioFileReader().getAudioInputStream(path.toFile());
        });

        assertThrows(UnsupportedAudioFileException.class, () -> {
            new AACAudioFileReader().getAudioInputStream(path.toUri().toURL());
        });
    }

    @Test
    public void test11() throws Exception {

        Path path = Paths.get(Objects.requireNonNull(AACTest.class.getResource(inFile)).toURI());

        assertThrows(UnsupportedAudioFileException.class, () -> {
            new AACAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        });
    }

    @Test
    @DisplayName("movie does not contain any AAC track")
    public void test12() throws Exception {

        Path path = Paths.get(Objects.requireNonNull(AACTest.class.getResource(inFile4)).toURI());

        assertThrows(UnsupportedAudioFileException.class, () -> {
            new AACAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        });
    }

    @Test
    @DisplayName("a file consumes input stream all")
    public void test13() throws Exception {

        Path path = Paths.get(Objects.requireNonNull(AACTest.class.getResource(inFile3)).toURI());

        assertThrows(UnsupportedAudioFileException.class, () -> {
            new AACAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        });
    }

    @Test
    @DisplayName("aac -> pcm")
    public void test2() throws Exception {

        Path path = Paths.get(Objects.requireNonNull(AACTest.class.getResource(inFile1)).toURI());
        System.out.println("file: " + path);
        AudioInputStream aacAis = AudioSystem.getAudioInputStream(path.toFile());
        System.out.println("INS: " + aacAis);
        AudioFormat inAudioFormat = aacAis.getFormat();
        System.out.println("INF: " + inAudioFormat);
        System.out.println(inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getSampleRate(),
            16,
            inAudioFormat.getChannels(),
            true,
            false);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outAudioFormat, aacAis);
        System.out.println("OUTS: " + pcmAis);
        System.out.println("OUT: " + pcmAis.getFormat());
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(pcmAis.getFormat());
        line.start();


        byte[] buf = new byte[1024];
        while (true) {
            int r = pcmAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("mp4 -> pcm")
    public void test3() throws Exception {

        Path path = Paths.get(Objects.requireNonNull(AACTest.class.getResource(inFile2)).toURI());
        System.out.println("file: " + path);
        AudioInputStream aacAis = AudioSystem.getAudioInputStream(path.toFile());
        System.out.println("INS: " + aacAis);
        AudioFormat inAudioFormat = aacAis.getFormat();
        System.out.println("INF: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getSampleRate(),
            16,
            inAudioFormat.getChannels(),
            true,
            false);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outAudioFormat, aacAis);
        System.out.println("OUTS: " + pcmAis);
        System.out.println("OUT: " + pcmAis.getFormat());
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(pcmAis.getFormat());
        line.start();


        byte[] buf = new byte[1024];
        while (true) {
            int r = pcmAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

}
