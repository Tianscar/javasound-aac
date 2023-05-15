package net.sourceforge.jaad.spi.javasound;

import net.sourceforge.jaad.spi.javasound.CircularBuffer.Trigger;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

abstract class AsynchronousAudioInputStream extends AudioInputStream implements Trigger {

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

	@Override
	public long skip(long n) throws IOException {

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
