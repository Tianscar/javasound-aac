package net.sourceforge.jaad.mp4;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.08.18
 * Time: 17:25
 */
public interface MP4Input extends AutoCloseable {

	static MP4Input open(InputStream in) {
	    return new MP4InputStream(in);
	}

	static MP4Input open(RandomAccessFile in) {
	    return new MP4RandomAccessStream(in);
	}

	String UTF8 = "UTF-8";
	String UTF16 = "UTF-16";

	/**
	 * Reads the next byte of data from the input. The value byte is returned as
	 * an int in the range 0 to 255. If no byte is available because the end of
	 * the stream has been reached, an EOFException is thrown. This method
	 * blocks until input data is available, the end of the stream is detected,
	 * or an I/O error occurs.
	 *
	 * @return the next byte of data
	 * @throws IOException If the end of the stream is detected or any I/O error occurs.
	 */
	int readByte() throws IOException;

	/**
	 * Reads <code>len</code> bytes of data from the input into the array
	 * <code>b</code>. If len is zero, then no bytes are read.
	 *
	 * This method blocks until all bytes could be read, the end of the stream
	 * is detected, or an I/O error occurs.
	 *
	 * If the stream ends before <code>len</code> bytes could be read an
	 * EOFException is thrown.
	 *
	 * @param b the buffer into which the data is read.
	 * @param off the start offset in array <code>b</code> at which the data is written.
	 * @param len the number of bytes to read.
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 */
	void readBytes(byte[] b, int off, int len) throws IOException;

	/**
	 * Reads up to eight bytes as a long value. This method blocks until all
	 * bytes could be read, the end of the stream is detected, or an I/O error
	 * occurs.
	 *
	 * @param n the number of bytes to read >0 and <=8
	 * @return the read bytes as a long value
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 * @throws IndexOutOfBoundsException if <code>n</code> is not in the range
	 * [1...8] inclusive.
	 */
	long readBytes(int n) throws IOException;

	/**
	 * Reads data from the input stream and stores them into the buffer array b.
	 * This method blocks until all bytes could be read, the end of the stream
	 * is detected, or an I/O error occurs.
	 * If the length of b is zero, then no bytes are read.
	 *
	 * @param b the buffer into which the data is read.
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 */

	void readBytes(byte[] b) throws IOException;

	/**
	 * Reads <code>n</code> bytes from the input as a String. The bytes are
	 * directly converted into characters. If not enough bytes could be read, an
	 * EOFException is thrown.
	 * This method blocks until all bytes could be read, the end of the stream
	 * is detected, or an I/O error occurs.
	 *
	 * @param n the length of the String.
	 * @return the String, that was read
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 */
	String readString(int n) throws IOException;

	/**
	 * Reads a null-terminated UTF-encoded String from the input. The maximum
	 * number of bytes that can be read before the null must appear must be
	 * specified.
	 * Although the method is preferred for unicode, the encoding can be any
	 * charset name, that is supported by the system.
	 *
	 * This method blocks until all bytes could be read, the end of the stream
	 * is detected, or an I/O error occurs.
	 *
	 * @param max the maximum number of bytes to read, before the null-terminator
	 * must appear.
	 * @param encoding the charset used to encode the String
	 * @return the decoded String
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 */
	String readUTFString(int max, String encoding) throws IOException;

	/**
	 * Reads a null-terminated UTF-encoded String from the input. The maximum
	 * number of bytes that can be read before the null must appear must be
	 * specified.
	 * The encoding is detected automatically, it may be UTF-8 or UTF-16
	 * (determined by a byte order mask at the beginning).
	 *
	 * This method blocks until all bytes could be read, the end of the stream
	 * is detected, or an I/O error occurs.
	 *
	 * @param max the maximum number of bytes to read, before the null-terminator
	 * must appear.
	 * @return the decoded String
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 */
	String readUTFString(int max) throws IOException;

	/**
	 * Reads a byte array from the input that is terminated by a specific byte
	 * (the 'terminator'). The maximum number of bytes that can be read before
	 * the terminator must appear must be specified.
	 *
	 * The terminator will not be included in the returned array.
	 *
	 * This method blocks until all bytes could be read, the end of the stream
	 * is detected, or an I/O error occurs.
	 *
	 * @param max the maximum number of bytes to read, before the terminator
	 * must appear.
	 * @param terminator the byte that indicates the end of the array
	 * @return the buffer into which the data is read.
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 */
	byte[] readTerminated(int max, int terminator) throws IOException;

	/**
	 * Reads a fixed point number from the input. The number is read as a
	 * <code>m.n</code> value, that results from deviding an integer by
	 * 2<sup>n</sup>.
	 *
	 * @param m the number of bits before the point
	 * @param n the number of bits after the point
	 * @return a floating point number with the same value
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 * @throws IllegalArgumentException if the total number of bits (m+n) is not
	 * a multiple of eight
	 */
	double readFixedPoint(int m, int n) throws IOException;

	/**
	 * Skips <code>n</code> bytes in the input. This method blocks until all
	 * bytes could be skipped, the end of the stream is detected, or an I/O
	 * error occurs.
	 *
	 * @param n the number of bytes to skip
	 * @throws IOException If the end of the stream is detected, the input
	 * stream has been closed, or if some other I/O error occurs.
	 */
	void skipBytes(long n) throws IOException;

	/**
	 * Returns the current offset in the stream.
	 *
	 * @return the current offset
	 * @throws IOException if an I/O error occurs (only when using a RandomAccessFile)
	 */
	long getOffset() throws IOException;

	/**
	 * Seeks to a specific offset in the stream. This is only possible when
	 * using a RandomAccessFile. If an InputStream is used, this method throws
	 * an IOException.
	 *
	 * @param pos the offset position, measured in bytes from the beginning of the
	 * stream
	 * @throws IOException if an InputStream is used, pos is less than 0 or an
	 * I/O error occurs
	 */
	void seek(long pos) throws IOException;

	/**
	 * Indicates, if random access is available. That is, if this
	 * <code>MP4InputStream</code> was constructed with a RandomAccessFile. If
	 * this method returns false, seeking is not possible.
	 *
	 * @return true if random access is available
	 */
	boolean hasRandomAccess();

	/**
	 * Indicates, if the input has some data left.
	 *
	 * @return true if there is at least one byte left
	 * @throws IOException if an I/O error occurs
	 */
	boolean hasLeft() throws IOException;


	/**
	 * Closes the input and releases any system resources associated with it.
	 * Once the stream has been closed, further reading or skipping will throw
	 * an IOException. Closing a previously closed stream has no effect.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	void close() throws IOException;
}
