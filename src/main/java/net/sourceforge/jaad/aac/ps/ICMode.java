package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 17:52
 */
public abstract class ICMode extends EnvMode {

    public ICMode(int id, int nr_par) {
        super(id, nr_par);
    }

    FBType fbType() {
            return (id %3)==2 ? FBType.T34 : FBType.T20;
        }

    int stride() {
        return (id%3)==0 ? 2 : 0;
    }
}
