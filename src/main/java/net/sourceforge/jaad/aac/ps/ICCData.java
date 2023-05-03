package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 12:37
 */
public class ICCData extends ICData<ICCMode> {

    /**
     * ICCMode default is mode(1) ??
     * @return
     */
    ICCMode mode() {
        return mode == null ? mode(1) : mode;
    }

    @Override
    protected ICCMode mode(int id) {
        return ICCMode.mode(id);
    }
}
