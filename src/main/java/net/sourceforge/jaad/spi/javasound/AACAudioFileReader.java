package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import net.sourceforge.jaad.util.Utils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import static java.nio.file.StandardOpenOption.READ;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

public class AACAudioFileReader extends AudioFileReader {

	public static final AudioFileFormat.Type AAC = new AudioFileFormat.Type("AAC", "aac");
	public static final AudioFileFormat.Type MP4_AAC = new AudioFileFormat.Type("MP4 AAC", "mp4");

	static final AudioFormat.Encoding AAC_ENCODING = new AudioFormat.Encoding("AAC");

	@Override
	public AudioFileFormat getAudioFileFormat(InputStream in) throws UnsupportedAudioFileException, IOException {
		in.mark(1000);
		try {
			final boolean[] noContainer = new boolean[1];
			if (isAAC(in, noContainer)) {
				AudioFormat format = new AudioFormat(AAC_ENCODING, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, true);
				return new AudioFileFormat(noContainer[0] ? AAC : MP4_AAC, format, NOT_SPECIFIED);
			}
			else throw new UnsupportedAudioFileException();
		}
		catch (UnsupportedAudioFileException e) {
			in.reset();
			throw e;
		}
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

	private static boolean isAAC(InputStream in, boolean[] noContainer) throws IOException {
		final byte[] head = new byte[12];
		Utils.readNBytes(in, head);
		final boolean isMP4;
		if (new String(head, 4, 4).equals("ftyp"))
			isMP4 = true;
		//This code is pulled directly from MP3-SPI.
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
		else {
			try {
				ADTSDemultiplexer adts = new ADTSDemultiplexer(in);
				Decoder.create(adts.getDecoderInfo()).decodeFrame(adts.readNextFrame(), new SampleBuffer());
				noContainer[0] = true;
				return true;
			}
			catch (Exception e) {
				return false;
			}
		}
		noContainer[0] = false;
		return isMP4;
	}

	@Override
	public AudioInputStream getAudioInputStream(InputStream in) throws UnsupportedAudioFileException, IOException, IllegalArgumentException {
		if (!in.markSupported()) throw new IllegalArgumentException("in.markSupported() == false");
		try {
			AudioFileFormat aff = getAudioFileFormat(in);
			in.reset();
			if (aff.getType() == AAC) return new AACAudioInputStream(in, aff.getFormat(), NOT_SPECIFIED);
			else return new MP4AudioInputStream(in, aff.getFormat(), NOT_SPECIFIED);
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
		try {
			InputStream inputStream = url.openStream();
			inputStream = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);
			AudioFileFormat aff = getAudioFileFormat(url);
			if (aff.getType() == AAC) {
				inputStream.reset();
				return new AACAudioInputStream(inputStream, aff.getFormat(), NOT_SPECIFIED);
			}
			else {
				inputStream.close();
				return new MP4AudioInputStream(url, aff.getFormat(), NOT_SPECIFIED);
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
	public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
		try {
			InputStream inputStream = Files.newInputStream(file.toPath(), READ);
			inputStream = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);
			AudioFileFormat aff = getAudioFileFormat(inputStream);
			if (aff.getType() == AAC) {
				inputStream.reset();
				return new AACAudioInputStream(inputStream, aff.getFormat(), NOT_SPECIFIED);
			}
			else {
				inputStream.close();
				return new MP4AudioInputStream(file, aff.getFormat(), NOT_SPECIFIED);
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
