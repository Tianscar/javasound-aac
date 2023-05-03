package net.sourceforge.jaad.aac;

import net.sourceforge.jaad.aac.syntax.BitStream;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 04.05.19
 * Time: 18:28
 */
public interface SampleRate {

    int getFrequency();

    SampleRate duplicated();

    SampleFrequency getNominal();

    static SampleRate forFrequency(int frequency) {
        SampleFrequency nominalFrequency = SampleFrequency.nominalFrequency(frequency);
        return nominalFrequency.forFrequency(frequency);
    }

    /**
     * Decode a frequency which may either an index to a nominal frequency
     * or an explicitly given frequency.
     *
     * See: 1.6.2.1 AudioSpecificConfig
     *
     * @param in input bit stream to decode.
     * @return a SampleFrequency.
     */
    static SampleRate decode(BitStream in) {
        int index = in.readBits(4);

        // indexed nominal frequency
        if(index != SampleFrequency.ESCAPE_INDEX)
            return SampleFrequency.TABLE.get(index);

        // for explicit frequency
        int frequency = in.readBits(24);
        return forFrequency(frequency);
    }
}
