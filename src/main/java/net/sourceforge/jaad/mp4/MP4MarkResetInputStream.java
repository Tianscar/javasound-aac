package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.InputStream;

class MP4MarkResetInputStream extends MP4InputStream {

	private final InputStream in;
	private long offset;

	/**
	 * Constructs an <code>MP4InputStream</code> that reads from a
	 * <code>InputStream</code> which supports mark. It will have no random access, but seeking
	 * will be possible.
	 *
	 * @param in a <code>InputStream</code> to read from
	 *
	 * @exception IllegalArgumentException if in.markSupported() == false
	 */
	MP4MarkResetInputStream(InputStream in) throws IllegalArgumentException {
		if (!in.markSupported()) throw new IllegalArgumentException("in.markSupported() == false");
		in.mark(MAX_BUFFER_SIZE);
		this.in = in;
		offset = 0;
	}

	@Override
	public int read() throws IOException {
		int i = in.read();
		if(i>=0)
			++offset;

		return i;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int i = in.read(b, off, len);

		if(i>0)
			offset += i;

		return i;
	}

	@Override
	public long skip(long n) throws IOException {
		long i = in.skip(n);

		if(i>0)
			offset += i;

		return i;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	@Override
	public void seek(long pos) throws IOException {
		long bytesToSkip = pos - offset;
		if (bytesToSkip >= 0) skipBytes(bytesToSkip);
		else {
			in.reset();
			offset = 0;
			skipBytes(pos);
		}
	}

	@Override
	public boolean seekSupported() {
		return true;
	}

	@Override
	public boolean hasLeft() throws IOException {
		return in.available()>0;
	}

	public void close() throws IOException {
		in.close();
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

}
