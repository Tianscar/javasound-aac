package net.sourceforge.jaad.spi.javasound;

import javax.sound.sampled.AudioFileFormat;

public class AACAudioFileType {

    private AACAudioFileType() {
        throw new UnsupportedOperationException();
    }

    public static final AudioFileFormat.Type AAC = new AudioFileFormat.Type("AAC", "aac");
    public static final AudioFileFormat.Type MP4_AAC = new AudioFileFormat.Type("MPEG-4 AAC", "m4a");

}
