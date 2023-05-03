package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.syntax.BitStream;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 24.09.21
 * Time: 23:25
 */
public interface PS {

    boolean isDataAvailable();

    void decode(BitStream ld);

    /* main Parametric Stereo decoding function */
    void process(float[][][] X_left, float[][][] X_right);
}
