package net.sourceforge.jaad.aac.ps;

import static net.sourceforge.jaad.aac.ps.Huffman.f_huff_icc;
import static net.sourceforge.jaad.aac.ps.Huffman.t_huff_icc;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 01:20
 */
public class ICCMode extends ICMode {

    private static final EnvTables ICC_TABLES = new EnvTables(f_huff_icc, t_huff_icc);

    @Override
    EnvTables tables() {
        return ICC_TABLES;
    }

    public ICCMode(int id, int nr_par) {
        super(id, nr_par);
    }

    @Override
    int clip(int idx) {
        idx = Math.max(idx, 0);
        idx = Math.min(idx, 7);
        return idx;
    }

    static private final ICCMode[] ICC_MODES = {
                new ICCMode(0, 10),
                new ICCMode(1, 20),
                new ICCMode(2, 34),
                new ICCMode(3, 10),
                new ICCMode(4, 20),
                new ICCMode(5, 34)
    };

    static ICCMode mode(int id) {
        return ICC_MODES[id];
    }
}
