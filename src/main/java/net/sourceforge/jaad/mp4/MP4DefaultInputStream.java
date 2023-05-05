package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.InputStream;

public class MP4DefaultInputStream extends MP4InputStream {

	private final InputStream in;
	private long offset;

	/**
	 * Constructs an <code>MP4InputStream</code> that reads from an
	 * <code>InputStream</code>. It will have no random access, thus seeking 
	 * will not be possible.
	 * 
	 * @param in an <code>InputStream</code> to read from
	 */
	MP4DefaultInputStream(InputStream in) {
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
		throw new IOException("could not seek");
	}

	@Override
	public boolean isSeekable() {
		return false;
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
