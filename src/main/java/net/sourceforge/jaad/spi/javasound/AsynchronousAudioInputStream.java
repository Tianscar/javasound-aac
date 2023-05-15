package net.sourceforge.jaad.spi.javasound;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

abstract class AsynchronousAudioInputStream extends AudioInputStream implements CircularBuffer.Trigger {

	private static final int MAX_SKIP_BUFFER_SIZE = 2048;

	private byte[] singleByte;
	protected final CircularBuffer buffer;

	AsynchronousAudioInputStream(InputStream in, AudioFormat format, long length) throws IOException {
		super(in, format, length);
		buffer = new CircularBuffer(this);
	}

	@Override
	public int read() throws IOException {
		final int i;
		if (singleByte == null)
			singleByte = new byte[1];
		if (buffer.read(singleByte, 0, 1) == -1)
			i = -1;
		else
			i = singleByte[0] & 0xFF;
		return i;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return buffer.read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return buffer.read(b, off, len);
	}

	/**
	 * Skips over and discards a specified number of bytes from this
	 * audio input stream.
	 * @param n the requested number of bytes to be skipped
	 * @return the actual number of bytes skipped
	 * @throws IOException if an input or output error occurs
	 * @see #read
	 * @see #available
	 */
	@Override
	public long skip(long n) throws IOException {

		// make sure not to skip fractional frames
		if( (n%frameSize) != 0 ) {
			n -= (n%frameSize);
		}

		if( frameLength != AudioSystem.NOT_SPECIFIED ) {
			// don't skip more than our set length in frames.
			if( (n/frameSize) > (frameLength-framePos) ) {
				n = (frameLength-framePos) * frameSize;
			}
		}
		long temp = mSkip(n);

		// if no error, update our position.
		if( temp%frameSize != 0 ) {

			// Throw an IOException if we've skipped a fractional number of frames
			throw new IOException("Could not skip an integer number of frames.");
		}
		if( temp >= 0 ) {
			framePos += temp/frameSize;
		}
		return temp;

	}

	private long mSkip(long n) throws IOException {

		long remaining = n;
		int nr;

		if (n <= 0) {
			return 0;
		}

		int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
		byte[] skipBuffer = new byte[size];
		while (remaining > 0) {
			nr = read(skipBuffer, 0, (int)Math.min(size, remaining));
			if (nr < 0) {
				break;
			}
			remaining -= nr;
		}

		return n - remaining;
	}

	@Override
	public int available() throws IOException {
		return buffer.availableRead();
	}

	@Override
	public void close() throws IOException {
		buffer.close();
		super.close();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void mark(int limit) {}

	@Override
	public void reset() throws IOException {
		throw new IOException("mark not supported");
	}

}
