package net.sourceforge.jaad.cli;

import net.sourceforge.jaad.SampleBuffer;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;
import net.sourceforge.jaad.util.wav.WaveFileWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Command line example, that can decode an AAC file to a WAVE file.
 * @author in-somnia
 */
public class Main {

	private static final String USAGE = "usage:\nnet.sourceforge.jaad.cli.Main [-mp4] <infile> <outfile>\n\n\t-mp4\tinput file is in MP4 container format";

	public static void main(String[] args) {
		try {
			if(args.length<2)
				printUsage();
			if(args[0].equals("-mp4")) {
				if(args.length<3)
					printUsage();
				else
					decodeMP4(args[1], args[2]);
			}
			else
				decodeAAC(args[0], args[1]);
		}
		catch(Exception e) {
			System.err.println("error while decoding: "+e.toString());
		}
	}

	private static void printUsage() {
		System.out.println(USAGE);
		System.exit(1);
	}

	private static void decodeMP4(String in, String out) throws Exception {
		WaveFileWriter wav = null;
		try {
			final MP4InputStream is = MP4InputStream.open(new RandomAccessFile(in, "r"));
			final MP4Container cont = new MP4Container(is);
			final Movie movie = cont.getMovie();
			final List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
			if(tracks.isEmpty())
			    throw new Exception("movie does not contain any AAC track");
			final AudioTrack track = (AudioTrack) tracks.get(0);

			wav = new WaveFileWriter(new File(out), track.getSampleRate(), track.getChannelCount(), track.getSampleSize());

			final Decoder dec = Decoder.create(track.getDecoderSpecificInfo().getData());

			Frame frame;
			final SampleBuffer buf = new SampleBuffer();
			while(track.hasMoreFrames()) {
				frame = track.readNextFrame();
				dec.decodeFrame(frame.getData(), buf);
				wav.write(buf.getData());
			}
		}
		finally {
			if(wav!=null)
				wav.close();
		}
	}

	private static void decodeAAC(String in, String out) throws IOException {
		WaveFileWriter wav = null;
		try {
			final ADTSDemultiplexer adts = new ADTSDemultiplexer(Files.newInputStream(Paths.get(in), StandardOpenOption.READ));
			final Decoder dec = Decoder.create(adts.getDecoderInfo());

			final SampleBuffer buf = new SampleBuffer();
			byte[] b;
			while (true) {
				b = adts.readNextFrame();
				dec.decodeFrame(b, buf);

				if(wav==null)
					wav = new WaveFileWriter(new File(out), buf.getSampleRate(), buf.getChannels(), buf.getBitsPerSample());
				wav.write(buf.getData());
			}
		}
		catch (EOFException ignored) {
		}
		finally {
			if(wav!=null)
				wav.close();
		}
	}
}
