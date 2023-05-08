package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.mp4.api.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.nio.file.StandardOpenOption.READ;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;
import static net.sourceforge.jaad.spi.javasound.AACAudioFileFormatType.AAC;
import static net.sourceforge.jaad.spi.javasound.AACAudioFileFormatType.MP4_AAC;
import static net.sourceforge.jaad.spi.javasound.MP4AudioInputStream.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND;

public class AACAudioFileReader extends AudioFileReader {

	private static AACAudioInputStream decodeAACAudioInputStream(InputStream in,
																 Map<String, Object> fileProperties,
																 Map<String, Object> formatProperties) throws IOException {
		return (AACAudioInputStream) decodeAACAudio(in, fileProperties, formatProperties, false);
	}

	private static AudioFileFormat decodeAACAudioFileFormat(InputStream in,
															Map<String, Object> fileProperties,
															Map<String, Object> formatProperties) throws IOException {
		return (AudioFileFormat) decodeAACAudio(in, fileProperties, formatProperties, true);
	}

	private static Object decodeAACAudio(InputStream in,
										 Map<String, Object> fileProperties,
										 Map<String, Object> formatProperties,
										 boolean fileFormat) throws IOException {
		ADTSDemultiplexer adts = new ADTSDemultiplexer(in);
		Decoder decoder = Decoder.create(adts.getDecoderInfo());
		SampleBuffer sampleBuffer = new SampleBuffer(decoder.getAudioFormat());
		decoder.decodeFrame(adts.readNextFrame(), sampleBuffer);

		// TODO calc duration
		/*
		double lengthInSeconds = 0;
		try {
			while (true) {
				decoder.decodeFrame(adts.readNextFrame(), sampleBuffer);
				lengthInSeconds += sampleBuffer.getLength();
			}
		}
		catch (EOFException ignored) {
		}
		fileProperties.put("duration", (long) (lengthInSeconds * 1_000_000L));
		 */

		dumpDecoderConfigProperties(decoder.getConfig(), formatProperties);
		AudioFormat audioFormat = dumpSampleBufferProperties(sampleBuffer, formatProperties);
		if (fileFormat) return new AudioFileFormat(AAC, audioFormat, NOT_SPECIFIED, fileProperties);
		else return new AACAudioInputStream(adts, decoder, sampleBuffer, in, audioFormat, NOT_SPECIFIED);
	}

	private static void dumpDecoderConfigProperties(DecoderConfig config, Map<String, Object> formatProperties) {
		formatProperties.put("aac.samplelength", config.getSampleLength());
		formatProperties.put("aac.framelength", config.getFrameLength());
		formatProperties.put("aac.channelcount", config.getChannelCount());
		formatProperties.put("aac.corecoderdelay", config.getCoreCoderDelay());

		formatProperties.put("aac.dependsoncoreorder", config.isDependsOnCoreCoder());
		formatProperties.put("aac.ps", config.isPSEnabled());
		formatProperties.put("aac.sbr", config.isSBREnabled());
		formatProperties.put("aac.smallframe", config.isSmallFrameUsed());
		formatProperties.put("aac.scalefactorresilience", config.isScalefactorResilienceUsed());
		formatProperties.put("aac.sectiondataresilience", config.isSectionDataResilienceUsed());
		formatProperties.put("aac.spectraldataresilience", config.isSpectralDataResilienceUsed());
	}

	private static AudioFormat dumpSampleBufferProperties(SampleBuffer sampleBuffer, Map<String, Object> formatProperties) {
		formatProperties.put("bitrate", sampleBuffer.getBitrate());
		formatProperties.put("samplerate", sampleBuffer.getSampleRate());
		formatProperties.put("samplesizeinbits", sampleBuffer.getBitsPerSample());
		formatProperties.put("channels", sampleBuffer.getChannels());
		formatProperties.put("bigendian", sampleBuffer.isBigEndian());
		return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleBuffer.getSampleRate(), sampleBuffer.getBitsPerSample(),
				sampleBuffer.getChannels(), frameSize(sampleBuffer.getChannels(), sampleBuffer.getBitsPerSample()),
				sampleBuffer.getSampleRate(), sampleBuffer.isBigEndian(), formatProperties);
	}

	private static int frameSize(int channels, int sampleSizeInBits) {
		return (channels == NOT_SPECIFIED || sampleSizeInBits == NOT_SPECIFIED)?
				NOT_SPECIFIED:
				((sampleSizeInBits + 7) / 8) * channels;
	}

	private static MP4AudioInputStream decodeMP4AudioInputStream(MP4InputStream in,
																 Map<String, Object> fileProperties,
																 Map<String, Object> formatProperties) throws IOException {
		return (MP4AudioInputStream) decodeMP4Audio(in, fileProperties, formatProperties, false);
	}

	private static AudioFileFormat decodeMP4AudioFileFormat(MP4InputStream in,
															Map<String, Object> fileProperties,
															Map<String, Object> formatProperties) throws IOException {
		return (AudioFileFormat) decodeMP4Audio(in, fileProperties, formatProperties, true);
	}

	private static Object decodeMP4Audio(MP4InputStream in,
										 Map<String, Object> fileProperties,
										 Map<String, Object> formatProperties,
										 boolean fileFormat) throws IOException {
		MP4Container mp4 = new MP4Container(in);
		Movie movie = mp4.getMovie();
		fileProperties.put("duration", (long) (movie.getDuration() * 1_000_000L));
		fileProperties.put("mp4.creationtime", movie.getCreationTime());
		fileProperties.put("mp4.modificationtime", movie.getModificationTime());
		fileProperties.put("mp4.containsmetadata", movie.containsMetaData());
		if (movie.containsMetaData()) {
			MetaData metaData = movie.getMetaData();
			for (Map.Entry<MetaData.Field<?>, Object> entry : metaData.getAll().entrySet()) {
				fileProperties.put(entry.getKey()
						.getName().replaceAll(" ", "").toLowerCase(Locale.ROOT), entry.getValue());
			}
		}
		List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
		if (tracks.isEmpty()) throw new IOException(ERROR_MESSAGE_AAC_TRACK_NOT_FOUND);
		AudioTrack track = (AudioTrack) tracks.get(0);
		Decoder decoder = Decoder.create(track.getDecoderSpecificInfo().getData());
		if (!track.hasMoreFrames()) throw new IOException("no valid frame exists");
		final Frame frame = track.readNextFrame();
		if (frame == null) throw new IOException("no valid frame exists");
		SampleBuffer sampleBuffer = new SampleBuffer();
		decoder.decodeFrame(frame.getData(), sampleBuffer);
		dumpDecoderConfigProperties(decoder.getConfig(), formatProperties);
		AudioFormat audioFormat = dumpSampleBufferProperties(sampleBuffer, formatProperties);
		if (fileFormat) return new AudioFileFormat(MP4_AAC, audioFormat, NOT_SPECIFIED, fileProperties);
		else return new MP4AudioInputStream(track, decoder, sampleBuffer, in, audioFormat, NOT_SPECIFIED);
	}

	@Override
	public AudioFileFormat getAudioFileFormat(InputStream in) throws UnsupportedAudioFileException, IOException {
		if (in instanceof MP4InputStream && ((MP4InputStream) in).seekSupported()) {
			((MP4InputStream) in).seek(0);
			if (isMP4(in)) {
				((MP4InputStream) in).seek(0);
				return decodeMP4AudioFileFormat((MP4InputStream) in, new HashMap<>(), new HashMap<>());
			}
		}
		in.mark(1000);
		try {
			if (isMP4(in)) {
				in.reset();
				return decodeMP4AudioFileFormat(MP4InputStream.open(in), new HashMap<>(), new HashMap<>());
			}
			else {
				try {
					return decodeAACAudioFileFormat(in, new HashMap<>(), new HashMap<>());
				}
				catch (IOException e) {
					throw new UnsupportedAudioFileException();
				}
			}
		}
		catch (UnsupportedAudioFileException e) {
			in.reset();
			throw e;
		}
	}

	private static boolean isMP4(InputStream in) throws IOException {
		final byte[] head = new byte[12];
		net.sourceforge.jaad.util.Utils.readNBytes(in, head);
		final boolean isMP4;
		if (new String(head, 4, 4).equals("ftyp")) isMP4 = true;
			// This code is pulled directly from MP3-SPI.
		else if ((head[0] == 'R') && (head[1] == 'I') && (head[2] == 'F') && (head[3] == 'F') && (head[8] == 'W') && (head[9] == 'A') && (head[10] == 'V') && (head[11] == 'E'))
		{
			isMP4 = false;	//RIFF/WAV stream found
		}
		else if ((head[0] == '.') && (head[1] == 's') && (head[2] == 'n') && (head[3] == 'd'))
		{
			isMP4 = false;	//AU stream found
		}
		else if ((head[0] == 'F') && (head[1] == 'O') && (head[2] == 'R') && (head[3] == 'M') && (head[8] == 'A') && (head[9] == 'I') && (head[10] == 'F') && (head[11] == 'F'))
		{
			isMP4 = false;	//AIFF stream found
		}
		else if (((head[0] == 'M') | (head[0] == 'm')) && ((head[1] == 'A') | (head[1] == 'a')) && ((head[2] == 'C') | (head[2] == 'c')))
		{
			isMP4 = false;	//APE stream found
		}
		else if (((head[0] == 'F') | (head[0] == 'f')) && ((head[1] == 'L') | (head[1] == 'l')) && ((head[2] == 'A') | (head[2] == 'a')) && ((head[3] == 'C') | (head[3] == 'c')))
		{
			isMP4 = false;	//FLAC stream found
		}
		else if (((head[0] == 'I') | (head[0] == 'i')) && ((head[1] == 'C') | (head[1] == 'c')) && ((head[2] == 'Y') | (head[2] == 'y')))
		{
			isMP4 = false;	//Shoutcast / ICE stream ?
		}
		else if (((head[0] == 'O') | (head[0] == 'o')) && ((head[1] == 'G') | (head[1] == 'g')) && ((head[2] == 'G') | (head[2] == 'g')))
		{
			isMP4 = false;	//Ogg stream ?
		}
		else
			isMP4 = false;
		return isMP4;
	}

	private AudioFileFormat getAudioFileFormatAndClose(InputStream in) throws UnsupportedAudioFileException, IOException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		try {
			return getAudioFileFormat(in);
		}
		finally {
			in.close();
		}
	}

	@Override
	public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
		return getAudioFileFormatAndClose(url.openStream());
	}

	@Override
	public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
		return getAudioFileFormatAndClose(Files.newInputStream(file.toPath(), READ));
	}

	@Override
	public AudioInputStream getAudioInputStream(InputStream in) throws UnsupportedAudioFileException, IOException, IllegalArgumentException {
		if (!(in instanceof MP4InputStream) && !in.markSupported()) throw new IllegalArgumentException("in.markSupported() == false");
		try {
			if (in instanceof MP4InputStream) {
				((MP4InputStream) in).seek(0);
				return decodeMP4AudioInputStream((MP4InputStream) in, new HashMap<>(), new HashMap<>());
			}
			in.mark(1000);
			try {
				if (isMP4(in)) {
					in.reset();
					return decodeMP4AudioInputStream(MP4InputStream.open(in), new HashMap<>(), new HashMap<>());
				}
				else {
					try {
						return decodeAACAudioInputStream(in, new HashMap<>(), new HashMap<>());
					}
					catch (IOException e) {
						throw new UnsupportedAudioFileException();
					}
				}
			}
			catch (UnsupportedAudioFileException e) {
				in.reset();
				throw e;
			}
		}
		catch (IOException e) {
			if (MP4AudioInputStream.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND.equals(e.getMessage())) {
		        throw new UnsupportedAudioFileException(MP4AudioInputStream.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND);
		    }
			else throw e;
		}
	}

	@Override
	public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
		InputStream in = url.openStream();
		try {
			return getAudioInputStream(in.markSupported() ? in : new BufferedInputStream(in));
		}
		catch (UnsupportedAudioFileException | IOException e) {
			in.close();
			throw e;
		}
	}

	@Override
	public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
		try {
			InputStream in = Files.newInputStream(file.toPath(), READ);
			if (isMP4(in)) {
				in.close();
				return decodeMP4AudioInputStream(MP4InputStream.open(new RandomAccessFile(file, "r")), new HashMap<>(), new HashMap<>());
			}
			else {
				try {
					return decodeAACAudioInputStream(in, new HashMap<>(), new HashMap<>());
				}
				catch (IOException e) {
					throw new UnsupportedAudioFileException();
				}
			}
		}
		catch (IOException e) {
			if (MP4AudioInputStream.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND.equals(e.getMessage())) {
				throw new UnsupportedAudioFileException(MP4AudioInputStream.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND);
			}
			else throw e;
		}
	}

}
