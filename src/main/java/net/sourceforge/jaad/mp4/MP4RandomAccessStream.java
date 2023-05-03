package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MP4RandomAccessStream extends MP4InputReader {

	private final RandomAccessFile fin;

	/**
	 * Constructs an <code>MP4InputStream</code> that reads from a
	 * <code>RandomAccessFile</code>. It will have random access and seeking
	 * will be possible.
	 *
	 * @param fin a <code>RandomAccessFile</code> to read from
	 */
	MP4RandomAccessStream(RandomAccessFile fin) {
		this.fin = fin;
	}

	@Override
	protected int read() throws IOException {
		return fin.read();
	}


	@Override
	protected int read(byte[] b, int off, int len) throws IOException {
		return fin.read(b, off, len);
	}

	@Override
	protected long skip(int n) throws IOException {
		return fin.skipBytes(n);
	}

	@Override
	public long getOffset() throws IOException {
		return fin.getFilePointer();
	}

	@Override
	public void seek(long pos) throws IOException {
		fin.seek(pos);
	}

	@Override
	public boolean hasRandomAccess() {
		return true;
	}


	@Override
	public boolean hasLeft() throws IOException {
		return fin.getFilePointer()<(fin.length()-1);
	}

	public void close() throws IOException {
		fin.close();
	}
}
