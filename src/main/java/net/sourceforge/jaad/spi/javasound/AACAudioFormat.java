package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.DecoderConfig;

import javax.sound.sampled.AudioFormat;

import java.util.HashMap;
import java.util.Map;

import static net.sourceforge.jaad.spi.javasound.AACAudioFormat.Encoding.AAC;

public class AACAudioFormat extends AudioFormat {

    public static class Encoding extends AudioFormat.Encoding {
        public static final AudioFormat.Encoding AAC = new Encoding("AAC");
        private Encoding(String name) {
            super(name);
        }
    }

    public AACAudioFormat(DecoderConfig config, SampleBuffer sampleBuffer) {
        super(AAC,
                sampleBuffer.getSampleRate(),
                sampleBuffer.getBitsPerSample(),
                sampleBuffer.getChannels(),
                Utils.frameSize(sampleBuffer.getChannels(), sampleBuffer.getBitsPerSample()),
                sampleBuffer.getSampleRate(),
                sampleBuffer.isBigEndian(),
                generateProperties(config, sampleBuffer));
    }

    private static Map<String, Object> generateProperties(DecoderConfig config, SampleBuffer sampleBuffer) {
        Map<String, Object> properties = new HashMap<>();

        properties.put("bitrate", sampleBuffer.getBitrate());

        properties.put("aac.samplelength", config.getSampleLength());
        properties.put("aac.framelength", config.getFrameLength());
        properties.put("aac.corecoderdelay", config.getCoreCoderDelay());

        properties.put("aac.dependsoncoreorder", config.isDependsOnCoreCoder());
        properties.put("aac.ps", config.isPSEnabled());
        properties.put("aac.sbr", config.isSBREnabled());
        properties.put("aac.smallframe", config.isSmallFrameUsed());
        properties.put("aac.scalefactorresilience", config.isScalefactorResilienceUsed());
        properties.put("aac.sectiondataresilience", config.isSectionDataResilienceUsed());
        properties.put("aac.spectraldataresilience", config.isSpectralDataResilienceUsed());

        return properties;
    }

}
