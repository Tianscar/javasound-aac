package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.InputStream;

class MP4ResourceInputStream extends MP4InputStream {

	private final String resource;
	private final ClassLoader resourceLoader;
	private volatile boolean closed = false;
	private InputStream in = null;
	private long offset;

	/**
	 * Constructs an <code>MP4InputStream</code> that reads from a
	 * resource. It will have no random access, but seeking
	 * will be possible.
	 *
	 * @param resourceLoader an <code>URL</code> to read from
	 */
	MP4ResourceInputStream(ClassLoader resourceLoader, String resource) throws IOException {
		this.resourceLoader = resourceLoader;
		this.resource = resource;
		ensureStreamAvailable();
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
	public long offset() {
		return offset;
	}

	@Override
	public void seek(long pos) throws IOException {
		checkNotClosed();
		if (pos < 0) throw new IndexOutOfBoundsException("negative position: " + pos);
		long bytesToSkip = pos - offset;
		if (bytesToSkip >= 0) {
			ensureStreamAvailable();
			skipBytes(bytesToSkip);
		}
		else {
			in.close();
			in = null;
			ensureStreamAvailable();
			skipBytes(pos);
		}
	}

	@Override
	public boolean seekSupported() {
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

	private void checkNotClosed() throws IOException {
		if (closed) throw new IOException("Already closed");
	}

	private void ensureStreamAvailable() throws IOException {
		if (in == null) {
			in = resourceLoader.getResourceAsStream(resource);
			if (in == null) throw new IOException("Couldn't read resource '" + resource + "' with ClassLoader '" + resourceLoader + "'");
			else offset = 0;
		}
	}

	private void ensureOpen() throws IOException {
		checkNotClosed();
		ensureStreamAvailable();
	}

	@Override
	public int available() throws IOException {
		ensureOpen();
		return in.available();
	}

}
