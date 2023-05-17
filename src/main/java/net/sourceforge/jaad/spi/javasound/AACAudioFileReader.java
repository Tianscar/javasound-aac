package net.sourceforge.jaad.spi.javasound;

import com.tianscar.javasound.sampled.spi.AudioResourceReader;
import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.MP4Exception;
import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import static java.nio.file.StandardOpenOption.READ;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

public class AACAudioFileReader extends AudioFileReader implements AudioResourceReader {

	private static AACAudioInputStream decodeAACAudioInputStream(InputStream in) throws IOException {
		return (AACAudioInputStream) decodeAACAudio(in, false);
	}

	private static AudioFileFormat decodeAACAudioFileFormat(InputStream in) throws IOException {
		return (AudioFileFormat) decodeAACAudio(in, true);
	}

	private static Object decodeAACAudio(InputStream in, boolean fileFormat) throws IOException {
		ADTSDemultiplexer adts = new ADTSDemultiplexer(in);
		Decoder decoder = Decoder.create(adts.getDecoderInfo());
		SampleBuffer sampleBuffer = new SampleBuffer(decoder.getAudioFormat());
		if (fileFormat) return new AACAudioFileFormat(adts, decoder.getConfig(), sampleBuffer);
		else return new AACAudioInputStream(adts, decoder, sampleBuffer, in, NOT_SPECIFIED);
	}

	private static MP4AudioInputStream decodeMP4AudioInputStream(MP4InputStream in) throws IOException, UnsupportedAudioFileException {
		return (MP4AudioInputStream) decodeMP4Audio(in, false);
	}

	private static AudioFileFormat decodeMP4AudioFileFormat(MP4InputStream in) throws IOException, UnsupportedAudioFileException {
		return (AudioFileFormat) decodeMP4Audio(in, true);
	}

	private static Object decodeMP4Audio(MP4InputStream in, boolean fileFormat) throws IOException, UnsupportedAudioFileException {
		MP4Container mp4;
		try {
			mp4 = new MP4Container(in);
		}
		catch (MP4Exception e) {
			throw new UnsupportedAudioFileException(e.getMessage());
		}
		Movie movie = mp4.getMovie();
		List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
		if (tracks.isEmpty()) throw new IOException(Utils.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND);
		AudioTrack track = (AudioTrack) tracks.get(0);
		Decoder decoder = Decoder.create(track.getDecoderSpecificInfo().getData());
		if (!track.hasMoreFrames()) throw new IOException("no valid frame exists");
		final Frame frame = track.readNextFrame();
		if (frame == null) throw new IOException("no valid frame exists");
		SampleBuffer sampleBuffer = new SampleBuffer();
		decoder.decodeFrame(frame.getData(), sampleBuffer);
		if (fileFormat) return new AACAudioFileFormat(movie, decoder.getConfig(), sampleBuffer);
		else return new MP4AudioInputStream(track, decoder, sampleBuffer, in, NOT_SPECIFIED);
	}

	@Override
	public AudioFileFormat getAudioFileFormat(InputStream in) throws UnsupportedAudioFileException, IOException {
		if (in instanceof MP4InputStream && ((MP4InputStream) in).seekSupported()) {
			((MP4InputStream) in).seek(0);
			if (isMP4(in)) {
				((MP4InputStream) in).seek(0);
				return decodeMP4AudioFileFormat((MP4InputStream) in);
			}
		}
		in.mark(1000);
		try {
			if (isMP4(in)) {
				in.reset();
				return decodeMP4AudioFileFormat(MP4InputStream.open(in));
			}
			else {
				try {
					return decodeAACAudioFileFormat(in);
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
				return decodeMP4AudioInputStream((MP4InputStream) in);
			}
			in.mark(1000);
			try {
				if (isMP4(in)) {
					in.reset();
					return decodeMP4AudioInputStream(MP4InputStream.open(in));
				}
				else {
					try {
						return decodeAACAudioInputStream(in);
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
			if (Utils.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND.equals(e.getMessage())) {
		        throw new UnsupportedAudioFileException(Utils.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND);
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
				return decodeMP4AudioInputStream(MP4InputStream.open(new RandomAccessFile(file, "r")));
			}
			else {
				try {
					return decodeAACAudioInputStream(in);
				}
				catch (IOException e) {
					throw new UnsupportedAudioFileException();
				}
			}
		}
		catch (IOException e) {
			if (Utils.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND.equals(e.getMessage())) {
				throw new UnsupportedAudioFileException(Utils.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND);
			}
			else throw e;
		}
	}

	@Override
	public AudioFileFormat getAudioFileFormat(ClassLoader resourceLoader, String name) throws UnsupportedAudioFileException, IOException {
		InputStream in = resourceLoader.getResourceAsStream(name);
		if (in == null) throw new IOException("Cannot load resource \"" + name + "\" with ClassLoader \"" + resourceLoader + "\"");
		else return getAudioFileFormatAndClose(in);
	}

	@Override
	public AudioInputStream getAudioInputStream(ClassLoader resourceLoader, String name) throws UnsupportedAudioFileException, IOException {
		try {
			InputStream in = resourceLoader.getResourceAsStream(name);
			if (in == null) throw new IOException("Cannot load resource \"" + name + "\" with ClassLoader \"" + resourceLoader + "\"");
			else if (isMP4(in)) {
				in.close();
				return decodeMP4AudioInputStream(MP4InputStream.open(resourceLoader, name));
			}
			else {
				try {
					return decodeAACAudioInputStream(in);
				}
				catch (IOException e) {
					throw new UnsupportedAudioFileException();
				}
			}
		}
		catch (IOException e) {
			if (Utils.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND.equals(e.getMessage())) {
				throw new UnsupportedAudioFileException(Utils.ERROR_MESSAGE_AAC_TRACK_NOT_FOUND);
			}
			else throw e;
		}
	}

}
