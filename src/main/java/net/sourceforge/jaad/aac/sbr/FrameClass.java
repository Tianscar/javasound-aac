package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 28.12.20
 * Time: 09:43
 */
enum FrameClass {

    FIXFIX, FIXVAR, VARFIX, VARVAR;

    public static List<FrameClass> VALUES = List.of(values());

    static FrameClass read(BitStream is) {
        int bits = is.readBits(2);
        return VALUES.get(bits);
    }
}
