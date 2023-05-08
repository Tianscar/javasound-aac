/*
 * Adapted from https://github.com/umjammer/JAADec/blob/0.8.9/src/test/java/net/sourceforge/jaad/spi/javasound/AacFormatConversionProviderTest.java
 *
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 * Copyright (c) 2023 by Karstian Lee, All rights reserved.
 *
 * Originally programmed by Naohide Sano
 * Modifications by Karstian Lee
 */

package net.sourceforge.jaad.test;

import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.spi.javasound.AACAudioFileReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AACTest {

    @Test
    @DisplayName("unsupported exception is able to detect in 3 ways")
    public void unsupported() {

        Path path = Paths.get("src/test/resources/fbodemo1.ogg");

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
    @DisplayName("movie does not contain any AAC track")
    public void tryToDecodeALAC() {
        assertThrows(UnsupportedAudioFileException.class, () -> {
            new AACAudioFileReader().getAudioInputStream(new File("src/test/resources/fbodemo1_alac.m4a"));
        });
    }

    @Test
    @DisplayName("a file consumes input stream all")
    public void tryToDecodeCAF() {
        assertThrows(UnsupportedAudioFileException.class, () -> {
            new AACAudioFileReader().getAudioInputStream(new File("src/test/resources/fbodemo1.caf"));
        });
    }

    private void play(AudioInputStream pcmAis) throws LineUnavailableException, IOException {
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
    @DisplayName("aac -> pcm, play via SPI")
    public void convertAACToPCMAndPlay() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File("src/test/resources/fbodemo1.aac");
        System.out.println("file: " + file.getAbsolutePath());
        AudioInputStream aacAis = AudioSystem.getAudioInputStream(file);
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

        play(pcmAis);
        pcmAis.close();
    }

    @Test
    @DisplayName("mp4 -> pcm, play via SPI")
    public void convertMP4ToPCMAndPlay() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File("src/test/resources/fbodemo1.m4a");
        System.out.println("file: " + file.getAbsolutePath());
        AudioInputStream aacAis = AudioSystem.getAudioInputStream(file);
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

        play(pcmAis);
        pcmAis.close();
    }

    @Test
    @DisplayName("play MP4 from InputStream via SPI")
    public void playMP4InputStream() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fbodemo1.m4a");
        AudioInputStream mp4Ais = AudioSystem.getAudioInputStream(stream);
        play(mp4Ais);
        mp4Ais.close();
    }

    @Test
    @DisplayName("play MP4 from resource name via SPI")
    public void playMP4Resource() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        MP4InputStream stream = MP4InputStream.open(Thread.currentThread().getContextClassLoader(), "fbodemo1.m4a");
        AudioInputStream mp4Ais = AudioSystem.getAudioInputStream(stream);
        play(mp4Ais);
        mp4Ais.close();
    }

    @Test
    @DisplayName("play MP4 from URL via SPI")
    public void playMP4URL() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        URL url = new URL("https://github.com/Tianscar/jaadec/raw/main/src/test/resources/fbodemo1.m4a");
        AudioInputStream mp4Ais = AudioSystem.getAudioInputStream(url);
        play(mp4Ais);
        mp4Ais.close();
    }

    @Test
    @DisplayName("list mp4 properties")
    public void listMP4Properties() throws UnsupportedAudioFileException, IOException {
        File file = new File("src/test/resources/fbodemo1.m4a");
        AudioFileFormat mp4Aff = AudioSystem.getAudioFileFormat(file);
        for (Map.Entry<String, Object> entry : mp4Aff.properties().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        for (Map.Entry<String, Object> entry : mp4Aff.getFormat().properties().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    @Test
    @DisplayName("list aac properties")
    public void listAACProperties() throws UnsupportedAudioFileException, IOException {
        File file = new File("src/test/resources/fbodemo1.aac");
        AudioFileFormat mp4Aff = AudioSystem.getAudioFileFormat(file);
        for (Map.Entry<String, Object> entry : mp4Aff.properties().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        for (Map.Entry<String, Object> entry : mp4Aff.getFormat().properties().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

}
