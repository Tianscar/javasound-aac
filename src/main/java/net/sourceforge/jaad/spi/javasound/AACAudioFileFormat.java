package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import net.sourceforge.jaad.mp4.api.MetaData;
import net.sourceforge.jaad.mp4.api.Movie;

import javax.sound.sampled.AudioFileFormat;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;
import static net.sourceforge.jaad.spi.javasound.AACAudioFileFormat.Type.AAC;
import static net.sourceforge.jaad.spi.javasound.AACAudioFileFormat.Type.MP4_AAC;

public class AACAudioFileFormat extends AudioFileFormat {

    public static class Type extends AudioFileFormat.Type {
        public static final AudioFileFormat.Type AAC = new Type("AAC", "aac");
        public static final AudioFileFormat.Type MP4_AAC = new Type("MPEG-4 AAC", "m4a");
        private Type(String name, String extension) {
            super(name, extension);
        }
    }

    public AACAudioFileFormat(Movie movie, DecoderConfig config, SampleBuffer sampleBuffer) {
        super(MP4_AAC, new AACAudioFormat(config, sampleBuffer), NOT_SPECIFIED, generateProperties(movie));
    }

    public AACAudioFileFormat(ADTSDemultiplexer adts, DecoderConfig config, SampleBuffer sampleBuffer) {
        super(AAC, new AACAudioFormat(config, sampleBuffer), NOT_SPECIFIED, generateProperties(adts, config));
    }

    private static Map<String, Object> generateProperties(Movie movie) {
        Map<String, Object> properties = new HashMap<>();

        properties.put("duration", (long) (movie.getDuration() * 1_000_000L));

        properties.put("mp4.creationtime", movie.getCreationTime());
        properties.put("mp4.modificationtime", movie.getModificationTime());

        if (movie.containsMetaData()) {
            MetaData metaData = movie.getMetaData();
            String entryName;
            for (Map.Entry<MetaData.Field<?>, Object> entry : metaData.getAll().entrySet()) {
                entryName = entry.getKey().getName().replaceAll(" ", "").toLowerCase(Locale.ROOT);
                switch (entryName) {
                    case "artist":
                        entryName = "author";
                        break;
                    case "comments":
                        entryName = "comment";
                        break;
                    case "releasedate":
                        entryName = "date";
                        break;
                    default:
                        entryName = "mp4." + entryName;
                        break;
                }
                properties.put(entryName, entry.getValue());
            }
        }

        return properties;
    }

    private static Map<String, Object> generateProperties(ADTSDemultiplexer adts, DecoderConfig config) {
        Map<String, Object> properties = new HashMap<>();

        int frames = 1; // already read 1 frame when new ADTSDemultiplexer(in)
        try {
            while (true) {
                adts.readNextFrame();
                frames ++;
            }
        }
        catch (EOFException ignored) {}
        catch (IOException e) {
            return properties;
        }

        final double lengthInSeconds = frames * (double) config.getSampleLength() / (double) config.getSampleFrequency().getFrequency();

        properties.put("duration", (long) (lengthInSeconds * 1_000_000L));

        return properties;
    }

}
