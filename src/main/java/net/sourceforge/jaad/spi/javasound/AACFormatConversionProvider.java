package net.sourceforge.jaad.spi.javasound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;
import java.io.IOException;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;
import static net.sourceforge.jaad.spi.javasound.AACAudioFormat.Encoding.AAC;

public class AACFormatConversionProvider extends FormatConversionProvider {

    private static final AudioFormat.Encoding[] SOURCE_ENCODINGS = new AudioFormat.Encoding[] { AAC };
    private static final AudioFormat.Encoding[] TARGET_ENCODINGS = new AudioFormat.Encoding[] { PCM_SIGNED };

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return SOURCE_ENCODINGS.clone();
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return TARGET_ENCODINGS.clone();
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AAC)) return TARGET_ENCODINGS.clone();
        else return new AudioFormat.Encoding[0];
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AAC)) {
            return new AudioFormat[] { getTargetFormat(sourceFormat) };
        }
        else return new AudioFormat[0];
    }

    private static AudioFormat getTargetFormat(AudioFormat sourceFormat) {
        return new AudioFormat(
                PCM_SIGNED,
                NOT_SPECIFIED,
                sourceFormat.getSampleSizeInBits(),
                sourceFormat.getChannels(),
                NOT_SPECIFIED,
                NOT_SPECIFIED,
                sourceFormat.isBigEndian()
        );
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        return getAudioInputStream(getTargetFormat(sourceStream.getFormat()), sourceStream);
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (targetFormat.getEncoding().equals(PCM_SIGNED) && sourceStream.getFormat().getEncoding().equals(AAC)) {
            AudioFormat sourceFormat = sourceStream.getFormat();
            if (sourceFormat.isBigEndian() == targetFormat.isBigEndian() &&
            sourceFormat.getChannels() == targetFormat.getChannels() &&
            sourceFormat.getSampleSizeInBits() == targetFormat.getSampleSizeInBits()) {
                try {
                    if (sourceStream instanceof AACAudioInputStream)
                        return new DecodedAACAudioInputStream((AACAudioInputStream) sourceStream);
                    else if (sourceStream instanceof MP4AudioInputStream)
                        return new DecodedMP4AudioInputStream((MP4AudioInputStream) sourceStream);
                    else throw new IllegalArgumentException("conversion not supported");
                }
                catch (IOException ignored) {}
            }
            throw new IllegalArgumentException("unable to convert "
                    + sourceFormat + " to "
                    + targetFormat);
        }
        else throw new IllegalArgumentException("conversion not supported");
    }

}
