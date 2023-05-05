package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.InputStream;

public class MP4ResourceInputStream extends MP4InputStream {

	private final String resource;
	private final ClassLoader resourceLoader;
	private boolean closed = false;
	private InputStream in = null;
	private long offset;

	/**
	 * Constructs an <code>MP4InputStream</code> that reads from a
	 * resource. It will have no random access, but seeking
	 * will be possible.
	 *
	 * @param resourceLoader an <code>URL</code> to read from
	 */
	MP4ResourceInputStream(ClassLoader resourceLoader, String resource) {
		this.resourceLoader = resourceLoader;
		this.resource = resource;
		offset = 0;
	}

	@Override
	public int read() throws IOException {
		ensureOpen();
		int i = in.read();
		if(i>=0)
			++offset;

		return i;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ensureOpen();
		int i = in.read(b, off, len);

		if(i>0)
			offset += i;

		return i;
	}

	@Override
	public long skip(long n) throws IOException {
		ensureOpen();
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
		ensureOpen();
		long bytesToSkip = pos - offset;
		if (bytesToSkip >= 0) skipBytes(bytesToSkip);
		else {
			in.close();
			in = resourceLoader.getResourceAsStream(resource);
			skipBytes(Math.min(pos, MAX_BUFFER_SIZE));
		}
	}

	@Override
	public boolean isSeekable() {
		return true;
	}

	@Override
	public boolean hasLeft() throws IOException {
		ensureOpen();
		return in.available()>0;
	}

	public void close() throws IOException {
		if (closed) return;
		closed = true;
		in.close();
	}

	private void ensureOpen() throws IOException {
		if (closed) throw new IOException("Already closed");
		if (in == null) in = resourceLoader.getResourceAsStream(resource);
	}

	@Override
	public int available() throws IOException {
		ensureOpen();
		return in.available();
	}

}
