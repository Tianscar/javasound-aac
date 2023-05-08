package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.RandomAccessFile;

class MP4RAFInputStream extends MP4InputStream {

	private final RandomAccessFile fin;

	/**
	 * Constructs an <code>MP4InputStream</code> that reads from a
	 * <code>RandomAccessFile</code>. It will have random access and seeking
	 * will be possible.
	 *
	 * @param fin a <code>RandomAccessFile</code> to read from
	 */
	MP4RAFInputStream(RandomAccessFile fin) {
		this.fin = fin;
	}

	@Override
	public int read() throws IOException {
		return fin.read();
	}


	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return fin.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return fin.skipBytes((int) Math.min(Integer.MAX_VALUE, n));
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
	public boolean seekSupported() {
		return true;
	}


	@Override
	public boolean hasLeft() throws IOException {
		return fin.getFilePointer()<(fin.length()-1);
	}

	public void close() throws IOException {
		fin.close();
	}

	@Override
	public int available() throws IOException {
		return (int) Math.min(Integer.MAX_VALUE, fin.length()-fin.getFilePointer()-1);
	}

}
