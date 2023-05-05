package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MP4URLInputStream extends MP4InputStream {

	private final URL url;
	private boolean closed = false;
	private InputStream in = null;
	private long offset;

	/**
	 * Constructs an <code>MP4InputStream</code> that reads from an
	 * <code>URL</code>. It will have no random access, but seeking
	 * will be possible.
	 *
	 * @param url an <code>URL</code> to read from
	 */
	MP4URLInputStream(URL url) {
		this.url = url;
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
			in = url.openStream();
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
		if (in == null) in = url.openStream();
	}

	@Override
	public int available() throws IOException {
		ensureOpen();
		return in.available();
	}

}
