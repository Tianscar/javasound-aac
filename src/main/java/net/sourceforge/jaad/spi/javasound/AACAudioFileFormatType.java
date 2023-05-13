package net.sourceforge.jaad.spi.javasound;

import javax.sound.sampled.AudioFileFormat;

public class AACAudioFileFormatType extends AudioFileFormat.Type {

    public static final AudioFileFormat.Type AAC = new AACAudioFileFormatType("AAC", "aac");
    public static final AudioFileFormat.Type MP4_AAC = new AACAudioFileFormatType("MPEG-4 AAC", "m4a");

    private AACAudioFileFormatType(String name, String extension) {
        super(name, extension);
    }

}
