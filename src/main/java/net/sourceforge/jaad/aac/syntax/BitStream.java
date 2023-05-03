package net.sourceforge.jaad.aac.syntax;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 21.12.18
 * Time: 13:40
 */
public interface BitStream {

    int getPosition();

    int getBitsLeft();

    int readBits(int n);

    default byte readByte(int n) {
        if(n>8)
            throw new IllegalArgumentException();

        return (byte) readBits(n);
    }

    default short readShort(int n) {
        if(n>16)
            throw new IllegalArgumentException();

        return (short) readBits(n);
    }

    int readBit();

    boolean readBool();

    int peekBits(int n);

    void skipBits(int n);

    void skipBit();

    void byteAlign();

    BitStream readSubStream(int n);

    static BitStream open(byte[] data) {
        return new ByteArrayBitStream(data);
    }
}
