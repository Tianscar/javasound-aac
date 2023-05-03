package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 12:37
 */
public class IIDData extends ICData<IIDMode> {

    @Override
    protected IIDMode mode(int id) {
        return IIDMode.mode(id);
    }
}
