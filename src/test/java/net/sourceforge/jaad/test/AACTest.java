/*
 * Adopted from https://github.com/umjammer/JAADec/blob/0.8.9/src/test/java/net/sourceforge/jaad/spi/javasound/AacFormatConversionProviderTest.java
 *
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 * Copyright (c) 2023 by Karstian Lee, All rights reserved.
 *
 * Originally programmed by Naohide Sano
 * Modifications by Karstian Lee
 */

package net.sourceforge.jaad.test;

import com.tianscar.javasound.sampled.AudioResourceLoader;
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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AACTest {

    @Test
    @DisplayName("unsupported exception is able to detect in 3 ways")
    public void unsupported() {

        Path path = Paths.get("src/test/resources/fbodemo1_vorbis.ogg");

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
            new AACAudioFileReader().getAudioInputStream(new File("src/test/resources/fbodemo1_aac.m4a"));
        });
    }

    private void play(AudioInputStream pcmAis) throws LineUnavailableException, IOException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(pcmAis.getFormat());
        line.start();

        byte[] buf = new byte[128 * 6];
        while (true) {
            int r = pcmAis.read(buf, 0, buf.length);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    private AudioInputStream decode(AudioInputStream mp4Ais) {
        AudioFormat inAudioFormat = mp4Ais.getFormat();
        AudioFormat decodedAudioFormat = new AudioFormat(
                AudioSystem.NOT_SPECIFIED,
                inAudioFormat.getSampleSizeInBits(),
                inAudioFormat.getChannels(),
                true,
                inAudioFormat.isBigEndian());
        return AudioSystem.getAudioInputStream(decodedAudioFormat, mp4Ais);
    }

    @Test
    @DisplayName("aac -> pcm, play via SPI")
    public void convertAACToPCMAndPlay() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File("src/test/resources/fbodemo1.aac");
        System.out.println("in file: " + file.getAbsolutePath());
        AudioInputStream aacAis = AudioSystem.getAudioInputStream(file);
        System.out.println("in stream: " + aacAis);
        AudioFormat inAudioFormat = aacAis.getFormat();
        System.out.println("in audio format: " + inAudioFormat);

        AudioFormat decodedAudioFormat = new AudioFormat(
                AudioSystem.NOT_SPECIFIED,
                inAudioFormat.getSampleSizeInBits(),
                inAudioFormat.getChannels(),
                true,
                inAudioFormat.isBigEndian());

        assertTrue(AudioSystem.isConversionSupported(decodedAudioFormat, inAudioFormat));

        aacAis = AudioSystem.getAudioInputStream(decodedAudioFormat, aacAis);
        decodedAudioFormat = aacAis.getFormat();
        System.out.println("decoded in stream: " + aacAis);
        System.out.println("decoded audio format: " + decodedAudioFormat);

        AudioFormat outAudioFormat = new AudioFormat(
                decodedAudioFormat.getSampleRate(),
                16,
                decodedAudioFormat.getChannels(),
                true,
                false);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, decodedAudioFormat));

        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outAudioFormat, aacAis);
        System.out.println("out stream: " + pcmAis);
        System.out.println("out audio format: " + pcmAis.getFormat());

        play(pcmAis);
        pcmAis.close();
    }

    @Test
    @DisplayName("mp4 -> pcm, play via SPI")
    public void convertMP4ToPCMAndPlay() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File("src/test/resources/fbodemo1_aac.m4a");
        System.out.println("in file: " + file.getAbsolutePath());
        AudioInputStream aacAis = AudioSystem.getAudioInputStream(file);
        System.out.println("in stream: " + aacAis);
        AudioFormat inAudioFormat = aacAis.getFormat();
        System.out.println("in audio format: " + inAudioFormat);

        AudioFormat decodedAudioFormat = new AudioFormat(
                AudioSystem.NOT_SPECIFIED,
                inAudioFormat.getSampleSizeInBits(),
                inAudioFormat.getChannels(),
                true,
                inAudioFormat.isBigEndian());

        assertTrue(AudioSystem.isConversionSupported(decodedAudioFormat, inAudioFormat));

        aacAis = AudioSystem.getAudioInputStream(decodedAudioFormat, aacAis);
        decodedAudioFormat = aacAis.getFormat();
        System.out.println("decoded in stream: " + aacAis);
        System.out.println("decoded audio format: " + decodedAudioFormat);

        AudioFormat outAudioFormat = new AudioFormat(
                decodedAudioFormat.getSampleRate(),
                16,
                decodedAudioFormat.getChannels(),
                true,
                false);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, decodedAudioFormat));

        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outAudioFormat, aacAis);
        System.out.println("out stream: " + pcmAis);
        System.out.println("out audio format: " + pcmAis.getFormat());

        play(pcmAis);
        pcmAis.close();
    }

    @Test
    @DisplayName("play MP4 from InputStream via SPI")
    public void playMP4InputStream() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fbodemo1_aac.m4a");
        AudioInputStream mp4Ais = AudioSystem.getAudioInputStream(stream);
        mp4Ais = decode(mp4Ais);
        play(mp4Ais);
        mp4Ais.close();
    }

    @Test
    @DisplayName("play MP4 from resource name via SPI")
    public void playMP4Resource() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioInputStream mp4Ais = AudioResourceLoader.getAudioInputStream(Thread.currentThread().getContextClassLoader(), "fbodemo1_aac.m4a");
        mp4Ais = decode(mp4Ais);
        play(mp4Ais);
        mp4Ais.close();
    }

    @Test
    @DisplayName("play MP4 from URL via SPI")
    public void playMP4URL() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        URL url = new URL("https://github.com/Tianscar/javasound-aac/raw/main/src/test/resources/fbodemo1_aac.m4a");
        AudioInputStream mp4Ais = AudioSystem.getAudioInputStream(url);
        mp4Ais = decode(mp4Ais);
        play(mp4Ais);
        mp4Ais.close();
    }

    @Test
    @DisplayName("list mp4 properties")
    public void listMP4Properties() throws UnsupportedAudioFileException, IOException {
        File file = new File("src/test/resources/fbodemo1_aac.m4a");
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
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("fbodemo1.aac");
        AudioFileFormat aacAff = AudioSystem.getAudioFileFormat(stream);
        for (Map.Entry<String, Object> entry : aacAff.properties().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        for (Map.Entry<String, Object> entry : aacAff.getFormat().properties().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    @Test
    @DisplayName("can play wav")
    public void checkCanPlayWAV() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        InputStream stream = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("fbodemo1.wav"));
        stream.mark(Integer.MAX_VALUE);
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(stream);
        System.out.println(audioFileFormat);
        stream.reset();
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(stream);
        play(audioInputStream);
        audioInputStream.close();
    }

}
