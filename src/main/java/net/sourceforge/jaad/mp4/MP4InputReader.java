package net.sourceforge.jaad.mp4;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 01.09.18
 * Time: 13:49
 */
abstract public class MP4InputReader implements MP4Input {

	private static final int BYTE_ORDER_MASK = 0xFEFF;

	// to be implemented
	abstract protected int read() throws IOException;
	abstract protected int read(final byte[] b, int off, int len) throws IOException;
	abstract protected long skip(int n) throws IOException;

	@Override
	public int readByte() throws IOException {
		int i = read();

		if(i==-1){
			throw new EOFException();
		}

		return i;
	}

	@Override
	public void readBytes(final byte[] b, int off, int len) throws IOException {
		int read = 0;

		while(read<len) {
			int i = read(b, off+read, len-read);
			if(i<0)
				throw new EOFException();
			else
				read += i;
		}
	}

		@Override
		public long readBytes(int n) throws IOException {
			if(n<1||n>8)
				throw new IndexOutOfBoundsException("invalid number of bytes to read: "+n);

			final byte[] b = new byte[n];
			readBytes(b, 0, n);

			long result = 0;
			for(int i = 0; i<n; i++) {
				result = (result<<8)|(b[i]&0xFF);
			}
			return result;
		}

	@Override
		public void readBytes(final byte[] b) throws IOException {
			readBytes(b, 0, b.length);
		}

		@Override
		public String readString(final int n) throws IOException {
			int i = -1;
			int pos = 0;
			char[] c = new char[n];
			while(pos<n) {
				i = readByte();
				c[pos] = (char) i;
				pos++;
			}
			return new String(c, 0, pos);
		}

		@Override
		public String readUTFString(int max, String encoding) throws IOException {
			return new String(readTerminated(max, 0), Charset.forName(encoding));
		}

		@Override
		public String readUTFString(int max) throws IOException {
			//read byte order mask
			final byte[] bom = new byte[2];
			readBytes(bom, 0, 2);
			if(bom[0]==0||bom[1]==0)
				return "";
			final int i = (bom[0]<<8)|bom[1];

			//read null-terminated
			final byte[] b = readTerminated(max-2, 0);
			//copy bom
			byte[] b2 = new byte[b.length+bom.length];
			System.arraycopy(bom, 0, b2, 0, bom.length);
			System.arraycopy(b, 0, b2, bom.length, b.length);

			return new String(b2, Charset.forName((i==BYTE_ORDER_MASK) ? UTF16 : UTF8));
		}

		@Override
		public byte[] readTerminated(int max, int terminator) throws IOException {
			final byte[] b = new byte[max];
			int pos = 0;
			int i = 0;
			while(pos<max&&i!=-1) {
				i = readByte();
				if(i!=-1)
					b[pos++] = (byte) i;
			}
			return Arrays.copyOf(b, pos);
		}

		@Override
		public double readFixedPoint(int m, int n) throws IOException {
			final int bits = m+n;
			if((bits%8)!=0)
				throw new IllegalArgumentException("number of bits is not a multiple of 8: "+(m+n));

			final long l = readBytes(bits/8);
			final double x = Math.pow(2, n);
			double d = ((double) l)/x;
			return d;
		}

	@Override
	public void skipBytes(final long n) throws IOException {
		long l = 0;

		while(l<n) {
			 l += skip((int)(n-l));
		}
	}
}
