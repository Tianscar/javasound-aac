package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.syntax.BitStream;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 12:37
 */
abstract public class ICData<Mode extends ICMode> extends EnvData<Mode> {

    ICData() {
        super(34);
    }

    FBType fbType() {
        return mode==null ? null : mode.fbType();
    }

    Mode readMode(BitStream ld) {
        boolean enabled = ld.readBool();

        if (enabled) {
            int id = ld.readBits(3);
            this.mode = mode(id);
        } else {
            this.mode = null;
        }

        return mode;
    }
}
