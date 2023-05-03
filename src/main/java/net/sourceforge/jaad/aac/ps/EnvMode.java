package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 24.09.21
 * Time: 23:30
 */
abstract public class EnvMode {

    final int id;
    final int nr_par;

    public EnvMode(int id, int nr_par) {
        this.id = id;
        this.nr_par = nr_par;
    }

    abstract int stride();

    /**
     * Reset calculated index to its regular range
     * @param idx to clip
     * @return clipped index
     */
    abstract int clip(int idx);

    abstract EnvTables tables();
}
